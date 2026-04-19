# AI Agent Admin - 变更日志

## 2026-04-18 移除检索得分功能

### 功能调整

**移除原因**：检索得分（Recall@K）需要用户在数据集项中预先标注期望文档ID，但评估任务创建时可动态选择知识库，两者无法匹配，导致该功能实际无法使用。

**删除内容**：

| 模块   | 删除项                           |
|------|-------------------------------|
| 数据集项 | `expectedDocIds`、`context` 字段 |
| 评估结果 | `retrievalScore` 字段           |
| 评估指标 | `averageRetrievalScore` 统计    |
| 前端   | 数据集表单中的"期望文档ID"和"参考上下文"输入框    |
| 前端   | 评估详情表格中的"检索得分"列               |

**数据库变更**：

```sql
ALTER TABLE dataset_items DROP COLUMN IF EXISTS expected_doc_ids;
ALTER TABLE dataset_items DROP COLUMN IF EXISTS context_data;
ALTER TABLE evaluation_results DROP COLUMN IF EXISTS retrieval_score;
```

**保留的评估指标**：

- AI 得分（0-100）- LLM-as-Judge 质量评分
- 语义相似度（0-1）- Embedding 余弦相似度
- 忠实度（0-1）- 答案是否忠实于检索内容

---

## 2026-04-15 语义切分异步处理修复与优化

### Bug修复

**临时文件清理问题**

异步处理时 Tomcat 清理临时上传文件导致 `NoSuchFileException`：

```
java.nio.file.NoSuchFileException: C:\Users\Administrator\AppData\Local\Temp\tomcat...
```

**问题原因**：

- HTTP 请求结束后 Tomcat 自动清理临时上传文件
- 异步线程在事务提交后执行（请求已结束），临时文件已不存在

**解决方案**：事件驱动架构 + byte[] 存储

```
上传请求 → 读取 file.getBytes() → 发布事件（存储 byte[]）→ 事务提交
         → 异步线程收到事件 → 创建 MockMultipartFile → 处理文档
```

**代码改动**：

1. `DocumentUploadEvent.java` - 存储 `byte[]` 而不是 `MultipartFile`
2. `DocumentServiceImpl.java` - 在发布事件前读取 `file.getBytes()`
3. `DocumentProcessingEventListener.java` - 使用 `MockMultipartFile` 包装 byte 数组
4. `pom.xml` - 添加 `spring-test` 依赖（MockMultipartFile 所在包）

### 功能优化

**进度显示优化**

- 当 `semanticProgressTotal` 为 0 或未初始化时，显示"准备中..."而不是"0/0 句子"
- 避免显示无意义的进度数字

**轮询刷新优化**

- 状态变成 `CHUNKED` 或 `FAILED` 时，重新获取完整文档列表
- 解决只刷新状态、不刷新分块数量的问题

**前端改动** (`Documents/index.tsx`)：

```typescript
// 轮询逻辑增加判断
if (['CHUNKED', 'FAILED'].includes(res.data.status)) {
    needRefreshAll = true  // 重新获取完整列表
}

// 进度显示优化
{record.semanticProgressTotal && record.semanticProgressTotal > 0 ? (
    // 显示进度条
) : (
    <span style={{color: '#999'}}>准备中...</span>
)}
```

---

## 2026-04-15 语义切分异步处理

### 新增功能

**语义切分异步处理**

将语义切分改为异步处理，避免上传接口阻塞：

- **新增状态**: `SEMANTIC_PROCESSING` - 语义切分处理中
- **进度显示**: 文档列表显示"语义切分中 (50/100 句子)"
- **进度 API**: `GET /api/v1/documents/{id}/semantic-progress`
- **前端轮询**: 每2秒自动更新进度

**数据模型变更**

```java
// Document 实体新增字段
@Column(name = "semantic_progress_current")
private Integer semanticProgressCurrent;  // 已处理句子数

@Column(name = "semantic_progress_total")
private Integer semanticProgressTotal;     // 总句子数
```

**处理流程**

```
上传文档 (SEMANTIC)
    → 创建文档记录 (status=SEMANTIC_PROCESSING)
    → 返回响应（快速，< 1秒）
    → 异步线程开始处理：
        1. 提取文本
        2. 分批调用 Embedding API（每批10个）
        3. 每批完成后更新进度到数据库
        4. 完成后更新状态为 CHUNKED
```

---

## 2026-04-15 文档分块策略增强

### 新增功能

**引入 LangChain4j 框架进行文档分块**

使用 LangChain4j 0.36.2 的官方分块器，替代自定义实现：

| 策略             | LangChain4j 实现                  | 说明                          |
|----------------|---------------------------------|-----------------------------|
| **FIXED_SIZE** | `DocumentByCharacterSplitter`   | 按字符分块                       |
| **PARAGRAPH**  | `DocumentByParagraphSplitter`   | 按段落，过长段落递归分割                |
| **SENTENCE**   | `DocumentBySentenceSplitter`    | 按句子分块                       |
| **RECURSIVE**  | `DocumentSplitters.recursive()` | 段落→句子→词→字符递归分割              |
| **SEMANTIC**   | 自实现 + Embedding API             | 调用 Embedding 计算句子相似度，在断点处分割 |

**语义分块实现（基于 Embedding API）**

- 将文本拆成句子列表
- 批量调用 Embedding API 计算每个句子的向量
- 计算相邻句子的余弦相似度
- 找到相似度断点（低于第30百分位数）
- 在断点处分割，形成语义连贯的文本块
- 设置最大分块限制（默认1000字符）防止过长

**界面改进**

- 每种策略旁边显示小问号按钮（Tooltip介绍）
- 根据策略自动显示/隐藏分块大小和重叠输入框
- SEMANTIC策略需选择Embedding模型

### 依赖变更

```xml
<!-- 新增 LangChain4j -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.36.2</version>
</dependency>
```

### 代码改动

**后端** (`DocumentServiceImpl.java`)

- 引入 LangChain4j 分块器类
- `splitText()` 使用 LangChain4j 分块器替代自定义实现
- 删除自定义分块方法（splitFixedSize, splitByParagraph等）
- 保留语义分块方法 `splitBySemanticWithEmbedding()`

**前端** (`Documents/index.tsx`, `api/documents.ts`)

- 新增 Tooltip 组件介绍每种策略
- 策略选择动态显示/隐藏参数输入
- SEMANTIC策略显示Embedding模型选择
- API增加 `embeddingModelId` 参数

### 测试结果

- 后端测试：172 个测试全部通过
- 前端构建：成功

---

## 2026-04-13 RAG评估系统改造

### 已完成功能

- EmbeddingService：文本向量计算服务
- 向量检索：pgvector集成，余弦相似度搜索
- 数据集增强：expectedDocIds/context字段
- 评估任务增强：documentId/enableRag字段
- 评估结果增强：semanticSimilarity/retrievalScore/faithfulness字段
- 多指标评估：AI评分 + 语义相似度 + 检索得分 + 忠实度