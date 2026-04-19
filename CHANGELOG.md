# AI Agent Admin - 变更日志

## 2026-04-19 pg_jieba 中文分词与 BM25 搜索优化

### 功能概述

为 PostgreSQL 数据库安装 pg_jieba 中文分词扩展，优化 BM25 中文全文搜索效果。

### Docker 镜像重构

**新镜像**: `postgres-pgvector-jieba:pg15`

基于 `pgvector/pgvector:pg15`，集成：

- pgvector - 向量存储扩展
- pg_jieba - 中文分词扩展（结巴分词）
- pg_trgm - 三元组模糊匹配扩展

**构建文件** (`docker/postgres-zhparser/`):

```
Dockerfile          - 镜像构建脚本
init.sql            - 初始化脚本（创建扩展、配置）
pg_jieba.zip        - pg_jieba 源码（Gitee镜像）
cppjieba.zip        - cppjieba 分词库（Gitee镜像）
limonp.zip          - limonp 工具库（GitHub）
```

**关键配置**:

```sql
-- init.sql
ALTER SYSTEM SET shared_preload_libraries = 'pg_jieba';
CREATE EXTENSION IF NOT EXISTS pg_jieba;
```

**构建命令**:

```bash
cd docker/postgres-zhparser
docker build -t postgres-pgvector-jieba:pg15 .
docker run -d --name agentx-postgres -p 5432:5432 postgres-pgvector-jieba:pg15
```

### BM25 中文搜索改进

**问题**：原实现使用 pg_trgm similarity 匹配中文，效果差且不相关。

**解决方案**：使用 pg_jieba 分词 + ts_rank 评分：

### BM25 阈值过滤修复

**问题**：BM25 搜索完全忽略用户设置的阈值，返回所有结果。

**原因**：

- `BM25SearchService.searchBM25()` 方法没有 threshold 参数
- SQL 查询中没有 score 过滤条件
- DocumentServiceImpl 调用 BM25 时未传递 threshold

**解决方案**：

1. 更新接口签名：

```java
List<VectorSearchResult> searchBM25(String query, String knowledgeBaseId, 
    String documentId, int topK, Double threshold);
```

2. SQL 添加阈值过滤：

```sql
WHERE content_tsv_jieba @@ to_tsquery('jiebamp', :tsQuery)
  AND ts_rank(content_tsv_jieba, to_tsquery('jiebamp', :tsQuery)) > :threshold
```

3. DocumentServiceImpl 传递 threshold 参数。

**分数范围差异（重要）**：

| 检索方式          | 分数范围     | 建议阈值       |
|---------------|----------|------------|
| 向量检索（余弦相似度）   | 0-1      | 0.3-0.7    |
| BM25（ts_rank） | 0.01-0.5 | 0.01-0.1   |
| 混合检索（RRF）     | 0-0.05   | 0.005-0.02 |

### 前端 RAG 配置优化

**改动**：

1. **检索策略位置调整**：移到知识库选择之后，必填验证
2. **阈值范围动态调整**：根据检索策略显示不同的阈值范围
    - 向量检索：0-1，默认 0.3
    - BM25：0-0.5，默认 0.05
    - 混合检索：0-0.05，默认 0.01
3. **分数标签优化**：来源显示根据策略显示不同标签
    - 向量检索显示"相似度"
    - BM25 显示"BM25分数"
    - 混合检索显示"RRF分数"

**RRF 分数说明**：

混合检索使用 RRF (Reciprocal Rank Fusion) 算法融合两种检索结果：

```
RRF_score = Σ 1/(k + rank_i)，k=60
```

- 双检索排名第一的结果分数 ≈ 0.033
- 排名靠后的结果分数 ≈ 0.008
  | BM25（ts_rank） | 0.01-0.5 | 0.01-0.1 |

用户设置的 0.5 阈值对 BM25 过高（BM25 最高分才约 0.5），会导致几乎无结果返回。

```java
// BM25SearchServiceImpl.java
// 中文查询：使用 jiebamp parser（最大概率模式）
String sql = """
    SELECT dc.id, dc.content,
           ts_rank(dc.content_tsv_jieba, to_tsquery('jiebamp', :tsQuery)) as score
    FROM document_chunks dc
    WHERE dc.content_tsv_jieba @@ to_tsquery('jiebamp', :tsQuery)
    ORDER BY score DESC
    """;
```

**jiebamp vs jieba**:

- `jieba` - 混合模式，依赖字典文件，需正确配置路径
- `jiebamp` - 最大概率模式，使用 HMM 模型，不依赖字典

**数据结构变更**:

```sql
-- 新增中文 tsvector 列
ALTER TABLE document_chunks ADD COLUMN content_tsv_jieba tsvector;

-- 创建 GIN 索引
CREATE INDEX idx_document_chunks_tsv_jieba ON document_chunks USING GIN(content_tsv_jieba);

-- 创建触发器（自动更新）
CREATE TRIGGER trg_update_content_tsv_jieba
BEFORE INSERT OR UPDATE ON document_chunks
FOR EACH ROW EXECUTE FUNCTION update_content_tsv_jieba();
```

### 其他代码改动

**知识库重索引默认模型** (`KnowledgeBases/index.tsx`):

```typescript
// 打开重索引对话框时，默认选中当前知识库的 Embedding 模型
setReindexModelId(selectedKb.defaultEmbeddingModelId || null)
```

**对话 Embedding 模型继承** (`ChatServiceImpl.java`):

```java
// 创建对话时，RAG Embedding 模型自动继承知识库默认模型
if (Boolean.TRUE.equals(request.getEnableRag()) && request.getKnowledgeBaseId() != null) {
    KnowledgeBase kb = knowledgeBaseRepository.findById(request.getKnowledgeBaseId())
        .orElseThrow(...);
    ragEmbeddingModelId = kb.getDefaultEmbeddingModelId();
}
```

**前端改动** (`Chat/index.tsx`):

- Embedding 模型选择改为 disabled（不可修改，继承知识库）
- 添加提示信息说明继承规则

### 测试验证

**数据库测试**:

```sql
-- 中文分词测试
SELECT to_tsvector('jiebamp', '包邮吗');
-- 结果: '包':1 '邮':2

-- 搜索测试
SELECT ts_rank(content_tsv_jieba, to_tsquery('jiebamp', '配送')) 
FROM document_chunks WHERE content_tsv_jieba @@ to_tsquery('jiebamp', '配送');
-- 返回相关结果
```

**后端测试**: 233 个测试全部通过

---

## 2026-04-19 Chat与RAG功能合并 (v1.0.0)

### 功能概述

将独立的"对话调试"和"RAG对话"功能合并为统一的对话调试页面，RAG作为可选增强功能。

### 新增功能

**对话调试页面 RAG 配置**

- 新建对话时可配置 RAG 检索增强（可选开关）
- 编辑对话时可修改 RAG 配置
- RAG 配置项：
    - 知识库选择
    - Embedding 模型选择（自动关联知识库默认模型）
    - 检索数量(topK)配置（1-20）
    - 相似度阈值配置（0-1）
    - 检索策略选择（向量检索/BM25/混合检索）
- 消息显示 RAG 检索来源（折叠面板）

### 数据模型变更

**ChatSession 实体新增字段**：

```java
@Column(name = "enable_rag")
private Boolean enableRag;              // 是否启用RAG

@Column(name = "knowledge_base_id")
private String knowledgeBaseId;         // 关联知识库

@Column(name = "rag_top_k")
private Integer ragTopK;                // 检索数量

@Column(name = "rag_threshold")
private Double ragThreshold;            // 相似度阈值

@Column(name = "rag_strategy")
private String ragStrategy;             // 检索策略

@Column(name = "rag_embedding_model_id")
private String ragEmbeddingModelId;     // Embedding模型
```

**ChatMessage 实体新增字段**：

```java
@Column(name = "sources", columnDefinition = "TEXT")
private String sources;                 // RAG检索来源JSON
```

### 代码改动

**后端改动**：

| 文件                     | 改动                                                                      |
|------------------------|-------------------------------------------------------------------------|
| `ChatSession.java`     | 新增6个RAG配置字段                                                             |
| `ChatMessage.java`     | 新增sources字段                                                             |
| `ChatRequest.java`     | CreateSessionRequest/UpdateSessionRequest新增RAG字段                        |
| `ChatSessionDTO.java`  | 新增RAG配置字段                                                               |
| `ChatResponse.java`    | 新增sources字段                                                             |
| `ChatServiceImpl.java` | createSession/updateSession支持RAG字段；sendMessage/sendMessageStream集成RAG检索 |
| `RagService.java`      | 新增retrieve()方法（仅检索，不生成回答）                                               |
| `RagServiceImpl.java`  | 实现retrieve()方法                                                          |

**前端改动**：

| 文件                     | 改动                                                                         |
|------------------------|----------------------------------------------------------------------------|
| `types/chat.ts`        | ChatSession/ChatMessage/CreateSessionRequest新增RAG字段；新增VectorSearchResult类型 |
| `pages/Chat/index.tsx` | 新建/编辑对话弹窗增加RAG配置UI；消息渲染增加来源显示                                              |
| `App.tsx`              | 删除/RAG路由                                                                   |
| `Layout/index.tsx`     | 删除"RAG对话"菜单项                                                               |

**删除内容**：

- 前端 `pages/Rag/` 目录（独立RAG对话页面）
- 前端 `api/rag.ts`、`api/ragSession.ts`
- 前端 `types/rag.ts`

### 单元测试

新增 RAG 相关单元测试，覆盖核心方法：

| 测试类                   | 测试数 | 覆盖内容                                                                                    |
|-----------------------|-----|-----------------------------------------------------------------------------------------|
| `ChatServiceImplTest` | 41  | createSession RAG字段、sendMessage RAG启用/禁用、retrieveDocuments、buildSystemMessage、sources解析 |
| `RagServiceImplTest`  | 13  | retrieve()方法各种参数组合                                                                      |

**覆盖率**（RAG核心方法）：

- `retrieveDocuments()`: 100%
- `buildSystemMessage()`: 87%
- `createSession()`: 87%
- `getSessionAndModelContext()`: 100%
- `RagServiceImpl.retrieve()`: 100%

### 发布

- Release: v1.0.0
- 发布包: `ai-agent-admin-v1.0.0.zip`（含后端jar + 前端 + README）
- GitHub: https://github.com/KasonLee-marker/ai-agent-admin/releases/tag/v1.0.0

---

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