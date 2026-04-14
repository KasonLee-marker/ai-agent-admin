# RAG 评估系统功能文档

> 更新时间：2026年4月13日
> 状态: 进行中

---

## Context

原有评估系统仅支持基础评估模式（提示词 + 模型 + 数据集），缺少以下关键功能：

1. **没有知识库参与** - RAG场景下评估应测试检索+生成
2. **评分方式单一** - 只有AI打分，缺少embedding相似度等客观指标
3. **数据集缺少RAG字段** - 没有expectedDocIds/context
4. **Embedding模型配置不清晰** - 用户不知道如何配置embedding模型

本次改造目标是构建完整的 RAG 评估体系。

---

## Progress Overview

| Phase     | 内容                      | 状态    |
|-----------|-------------------------|-------|
| Phase 1.1 | Embedding Provider 类型定义 | ✅ 已完成 |
| Phase 1.2 | EmbeddingService 服务实现   | ✅ 已完成 |
| Phase 1.3 | 向量检索改造                  | ✅ 已完成 |
| Phase 1.4 | 模型管理页面支持 Embedding 类型   | ⏳ 待完成 |
| Phase 2   | 数据集增强                   | ✅ 已完成 |
| Phase 3   | 评估任务增强                  | ✅ 已完成 |
| Phase 4   | 评估结果增强                  | ✅ 已完成 |
| Phase 5   | 评估服务改造                  | ✅ 已完成 |
| Phase 6   | Embedding 模型引用逻辑完善      | ⏳ 待完成 |

---

## Checkpoint 1: Embedding 服务实现 ✅

### 1.1 ModelProvider 新增 Embedding 类型 ✅

**文件**: `admin-server-core/src/main/java/com/aiagent/admin/domain/enums/ModelProvider.java`

新增两个 Embedding Provider:

| Provider            | DisplayName      | DefaultBaseUrl                                 | 内置模型                                                                                           |
|---------------------|------------------|------------------------------------------------|------------------------------------------------------------------------------------------------|
| OPENAI_EMBEDDING    | OpenAI Embedding | https://api.openai.com                         | text-embedding-ada-002 (1536维), text-embedding-3-small (1536维), text-embedding-3-large (3072维) |
| DASHSCOPE_EMBEDDING | 阿里云百炼 Embedding  | https://dashscope.aliyuncs.com/compatible-mode | text-embedding-v1/v2/v3                                                                        |

### 1.2 EmbeddingService 接口和实现 ✅

**文件**:

- `admin-server-runtime/src/main/java/com/aiagent/admin/service/EmbeddingService.java`
- `admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/EmbeddingServiceImpl.java`

**接口方法**:

| 方法                                         | 说明          |
|--------------------------------------------|-------------|
| `embed(String text)`                       | 单文本向量计算     |
| `embedBatch(List<String> texts)`           | 批量向量计算（更高效） |
| `cosineSimilarity(float[] v1, float[] v2)` | 余弦相似度计算     |
| `semanticSimilarity(String t1, String t2)` | 文本语义相似度     |
| `getEmbeddingDimension()`                  | 获取向量维度      |

### 1.3 向量检索改造 ✅

**文件**: `admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/DocumentServiceImpl.java`

**改造内容**:

- 文档上传后自动计算分块的 embedding 并存储到 `DocumentChunk.embedding`
- `searchSimilar()` 使用向量相似度搜索替代文本匹配

**检索流程**:

```
查询文本 → Embedding → 计算与所有分块的相似度 → 排序 → TopK结果
```

---

## Checkpoint 1.4: 模型管理页面支持 Embedding 类型 ⏳ 待完成

### 问题

当前模型管理页面的问题：

1. **没有区分模型类型**: 用户不知道 Provider 是对话模型还是 Embedding 模型
2. **缺少类型标签**: Provider 下拉选项没有显示类型信息
3. **没有类型默认值**: 未指定类型时应默认为对话模型

### 需求

1. **Provider 分组显示**:
    - 对话模型组: OPENAI, ANTHROPIC, DASHSCOPE, DEEPSEEK 等
    - Embedding 模型组: OPENAI_EMBEDDING, DASHSCOPE_EMBEDDING

2. **类型标签**: 显示 Provider 的类型（Chat / Embedding）

3. **默认类型**:
    - 没有明确指定时默认为 Chat 类型
    - Embedding 类型需要明确选择

### 关键文件

- `frontend/src/pages/Models/index.tsx` - 前端模型管理页面
- `frontend/src/types/model.ts` - 模型类型定义
- `admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/ProviderResponse.java` - Provider 响应 DTO（需增加 type
  字段）

---

## Checkpoint 2: 数据集增强 ✅

### 新增字段

| 字段               | 类型            | 说明                      |
|------------------|---------------|-------------------------|
| `expectedDocIds` | String (JSON) | 期望检索到的文档ID列表，用于计算检索评估指标 |
| `context`        | String        | 参考上下文，用于评估答案忠实度         |

### 关键文件

```
admin-server-core/src/main/java/com/aiagent/admin/domain/entity/DatasetItem.java
admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/DatasetItemCreateRequest.java
admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/DatasetItemResponse.java
frontend/src/pages/Datasets/index.tsx
frontend/src/types/dataset.ts
```

---

## Checkpoint 3: 评估任务增强 ✅

### 新增字段

| 字段           | 类型      | 说明          |
|--------------|---------|-------------|
| `documentId` | String  | 关联的知识库ID    |
| `enableRag`  | Boolean | 是否启用RAG评估模式 |

### 关键文件

```
admin-server-core/src/main/java/com/aiagent/admin/domain/entity/EvaluationJob.java
admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/EvaluationJobCreateRequest.java
admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/EvaluationJobResponse.java
frontend/src/pages/Evaluations/index.tsx
frontend/src/types/evaluation.ts
```

---

## Checkpoint 4: 评估结果增强 ✅

### 新增字段

| 字段                   | 类型            | 说明                  |
|----------------------|---------------|---------------------|
| `semanticSimilarity` | Float         | Embedding语义相似度（0-1） |
| `retrievedDocIds`    | String (JSON) | 实际检索到的文档ID列表        |
| `retrievalScore`     | Float         | 检索评估得分（Recall）      |
| `faithfulness`       | Float         | 事实忠实度（0-1）          |

### 关键文件

```
admin-server-core/src/main/java/com/aiagent/admin/domain/entity/EvaluationResult.java
admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/EvaluationResultResponse.java
frontend/src/pages/Evaluations/index.tsx
frontend/src/types/evaluation.ts
```

---

## Checkpoint 5: 评估服务改造 ✅

### 改造内容

`EvaluationServiceImpl.evaluateItem()` 支持两种评估模式：

**基础评估流程**:

```
渲染提示词 → 调用模型 → 计算语义相似度 → AI评分 → 保存结果
```

**RAG评估流程**:

```
检索文档 → 计算检索指标 → 构建RAG提示词 → 调用模型 → 
计算语义相似度 → 计算忠实度 → AI评分 → 保存结果
```

### 关键文件

```
admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/EvaluationServiceImpl.java
```

---

## Checkpoint 6: Embedding 模型引用逻辑完善 ⏳ 待完成

### 问题分析

Embedding 模型在系统中被多处调用，但引用逻辑不清晰：

| 调用场景     | 当前逻辑                                               | 问题                     |
|----------|----------------------------------------------------|------------------------|
| 文档上传分块   | EmbeddingService.getEmbeddingModelConfig()         | 用户不知道配置哪个 embedding 模型 |
| RAG 对话检索 | DocumentService.searchSimilar() → EmbeddingService | 同上                     |
| 评估相似度计算  | EvaluationServiceImpl → EmbeddingService           | 同上                     |

### 当前 EmbeddingService 查找逻辑

`EmbeddingServiceImpl.getEmbeddingModelConfig()`:

```java
1. 查找 OPENAI_EMBEDDING 或 DASHSCOPE_EMBEDDING 类型的配置（任选第一个）
2. 没找到 → 查找默认模型（可能是 chat 模型，不合适）
3. 还没有 → 使用系统默认配置（需要配置 API Key）
```

### 问题

1. **没有指定默认 Embedding 模型**: 如果用户没配置 embedding 模型，会错误地使用 chat 模型
2. **没有全局 Embedding 配置入口**: 用户不知道在哪里配置 embedding 模型
3. **各处调用没有明确指定**: 文档处理、RAG检索、评估都应该用同一个 embedding 配置

### 解决方案

**方案 A: 全局 Embedding 配置**

- 新增"系统设置"页面，配置默认 Embedding 模型
- 或者模型管理页面可设置"默认 Embedding 模型"（类似默认对话模型）

**方案 B: 文档关联 Embedding 模型**

- Document 实体增加 `embeddingModelId` 字段
- 上传文档时选择使用的 Embedding 模型
- 检索时使用该文档配置的 Embedding 模型

**方案 C: 组合方案**

- 全局默认 Embedding 模型（系统级）
- 文档可单独指定（覆盖默认）

### 需要改造的文件

| 文件                                    | 改造内容                                             |
|---------------------------------------|--------------------------------------------------|
| `ModelConfig.java`                    | 新增 `modelType` 字段（CHAT/EMBEDDING）或使用 Provider 区分 |
| `ModelConfigRepository.java`          | 新增查询默认 Embedding 模型的方法                           |
| `Document.java`                       | 新增 `embeddingModelId` 字段（可选）                     |
| `DocumentServiceImpl.java`            | 使用明确的 Embedding 配置                               |
| `EmbeddingServiceImpl.java`           | 改进查找逻辑                                           |
| `frontend/src/pages/Models/index.tsx` | 支持设置默认 Embedding 模型                              |

---

## Embedding 模型使用链路图

```
┌─────────────────────────────────────────────────────────────────┐
│                    Embedding 模型配置                            │
│  ┌───────────────┐                                              │
│  │ 模型管理页面   │  → 配置 OPENAI_EMBEDDING 或 DASHSCOPE_EMBEDDING │
│  │ 设置默认       │  → 标记为"默认 Embedding 模型"                  │
│  └───────────────┘                                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EmbeddingService                              │
│  getEmbeddingModelConfig() → 查找默认 Embedding 模型配置          │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│ 文档上传     │       │ RAG 检索    │       │ 评估相似度   │
│ 分块Embedding │       │ 查询Embedding│       │ 文本Embedding│
└─────────────┘       └─────────────┘       └─────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
 DocumentChunk.embedding   检索TopK             semanticSimilarity
```

---

## 评估指标说明

### 1. AI评分 (score) ✅

- **范围**: 0-100
- **方法**: LLM-as-a-Judge
- **说明**: AI对比期望输出和实际输出给出质量评分

### 2. 语义相似度 (semanticSimilarity) ✅

- **范围**: 0-1
- **方法**: Embedding余弦相似度
- **说明**: 计算期望输出与实际输出的向量相似度
- **依赖**: EmbeddingService（需要配置 Embedding 模型）

### 3. 检索得分 (retrievalScore) ✅

- **范围**: 0-1
- **方法**: Recall@K
- **说明**: 期望文档被检索到的比例
- **公式**: `命中期望文档数 / 期望文档总数`

### 4. 忠实度 (faithfulness) ✅

- **范围**: 0-1
- **方法**: LLM评估
- **说明**: 答案是否忠实于检索到的上下文内容

---

## 评估流程详解

### 基础评估模式 (enableRag = false)

```
1. 渲染提示词模板
2. 调用 Chat 模型获取响应
3. 计算语义相似度（如有期望输出）→ EmbeddingService
4. AI评分（如有期望输出）→ LLM-as-Judge
5. 保存结果
```

### RAG评估模式 (enableRag = true)

```
1. 检索文档 → DocumentService.searchSimilar() → EmbeddingService
2. 记录检索结果 (retrievedDocIds)
3. 计算检索得分（如有 expectedDocIds）→ Recall
4. 构建 RAG 提示词（包含检索上下文）
5. 调用 Chat 模型获取响应
6. 计算语义相似度 → EmbeddingService
7. 计算忠实度 → LLM评估
8. AI评分 → LLM-as-Judge
9. 保存结果
```

---

## Completion Criteria

### Phase 1.4: 模型管理页面支持 Embedding 类型

- [ ] Provider 下拉框分组显示（Chat 组 / Embedding 组）
- [ ] Provider 显示类型标签
- [ ] 模型列表显示类型列
- [ ] 支持设置"默认 Embedding 模型"

### Phase 6: Embedding 模型引用逻辑完善

- [ ] ModelConfig 或 Provider 增加 modelType 字段
- [ ] ModelConfigRepository 增加查询默认 Embedding 模型方法
- [ ] EmbeddingServiceImpl 改进查找逻辑（只查 Embedding 类型）
- [ ] 前端支持设置默认 Embedding 模型
- [ ] 文档上传时使用明确的 Embedding 配置

---

## Verification Plan

### 1. 后端测试

```bash
mvn clean compile
mvn test -Dtest=EmbeddingServiceImplTest
mvn test -Dtest=DocumentServiceImplTest
mvn test -Dtest=EvaluationServiceImplTest
```

### 2. 前端测试

```bash
cd frontend
npm run dev
```

### 3. 功能验证

- 配置 Embedding 模型（模型管理页面）
- 上传文档（检查 embedding 是否正确存储）
- 创建 RAG 评估任务（检查检索得分、相似度等指标）
- 对比不同配置的评估结果

---

*文档更新于 2026年4月13日*