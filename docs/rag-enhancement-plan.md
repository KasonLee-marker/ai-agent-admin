# RAG 模块增强方案

## 背景

当前 RAG 模块存在以下问题：

1. 无法选择 Embedding 模型进行检索
2. 检索策略单一（仅向量检索）
3. 缺少 BM25、混合检索、重排序等高级功能
4. 知识库管理概念缺失

## 目标

构建完整的 RAG 系统，支持：

- 多种检索策略
- Embedding 模型灵活选择
- 知识库组织管理
- 高级检索功能（重排序、多轮对话）

---

## Phase 1: 知识库管理

### 问题

当前文档是散乱的，没有"知识库"概念组织文档。

### 解决方案

新增 `KnowledgeBase` 实体：

```java
@Entity
@Table(name = "knowledge_bases")
public class KnowledgeBase {
    @Id
    private String id;
    
    private String name;           // 知识库名称
    private String description;    // 描述
    
    private String defaultEmbeddingModelId;  // 默认 Embedding 模型
    
    private Integer documentCount; // 文档数量
    private Integer chunkCount;    // 分块总数
    
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Document 关联修改**：

```java
// Document 增加字段
@Column(name = "knowledge_base_id")
private String knowledgeBaseId;  // 所属知识库
```

**前端改动**：

- 新增知识库管理页面（创建、编辑、删除）
- 知识库详情页显示文档列表
- 文档上传时选择知识库

---

## Phase 2: Embedding 模型选择

### 问题

文档用特定 Embedding 模型索引后，无法切换模型检索。

### 解决方案 A: 多 Embedding 表索引（推荐）

同一文档支持多种 Embedding 模型索引：

```
document_embeddings_1024 (text-embedding-v3)
document_embeddings_1536 (text-embedding-ada-002)
document_embeddings_3072 (text-embedding-3-large)
```

**DocumentChunk 修改**：

```java
// 增加多向量存储字段
@Column(name = "embeddings_json", columnDefinition = "TEXT")
private String embeddingsJson;  // JSON: {"modelId1": "table1", "modelId2": "table2"}
```

**检索时选择模型**：

```java
// RagChatRequest 增加
private String embeddingModelId;  // 选择检索用的 Embedding 模型

// DocumentServiceImpl.searchSimilar 改造
public List<VectorSearchResult> searchSimilar(VectorSearchRequest request, String embeddingModelId) {
    // 根据选择的模型查询对应的向量表
    ModelConfig embeddingConfig = getEmbeddingConfig(embeddingModelId);
    String tableName = embeddingConfig.getEmbeddingTableName();
    // 检索...
}
```

### 解决方案 B: 知识库绑定模型

知识库指定默认 Embedding 模型，文档上传时自动使用：

```java
// KnowledgeBase
private String defaultEmbeddingModelId;

// 文档上传时
uploadDocument(file, knowledgeBaseId) {
    KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId);
    String embeddingModelId = kb.getDefaultEmbeddingModelId();
    // 用指定模型索引
}
```

---

## Phase 3: 多检索策略

### 当前

仅向量检索（余弦相似度）。

### 新增策略

| 策略         | 说明          | 适用场景      |
|------------|-------------|-----------|
| **VECTOR** | 纯向量检索       | 语义相似度高的场景 |
| **BM25**   | 关键词检索       | 精确匹配、术语查询 |
| **HYBRID** | BM25 + 向量融合 | 综合场景      |
| **RERANK** | 向量检索 + 重排序  | 高质量检索     |

### 实现

**BM25 检索**：

```java
// 使用 PostgreSQL 全文搜索
SELECT chunk_id, content, 
       ts_rank(content_tsvector, to_tsquery('query')) as score
FROM document_chunks
WHERE content_tsvector @@ to_tsquery('query')
ORDER BY score DESC
LIMIT topK;
```

**混合检索**：

```java
public List<VectorSearchResult> hybridSearch(String query, int topK, float vectorWeight, float bm25Weight) {
    // 1. 向量检索 topK*2
    List<VectorSearchResult> vectorResults = vectorSearch(query, topK * 2);
    
    // 2. BM25 检索 topK*2
    List<Bm25Result> bm25Results = bm25Search(query, topK * 2);
    
    // 3. 融合评分
    // Reciprocal Rank Fusion (RRF)
    // score = 1/(k + rank_vector) * vectorWeight + 1/(k + rank_bm25) * bm25Weight
    
    // 4. 合并排序返回 topK
}
```

**重排序**：

```java
// 使用 Reranker 模型（如 Cohere Rerank）
public List<VectorSearchResult> rerank(String query, List<VectorSearchResult> candidates) {
    // 调用 Reranker API
    List<RerankScore> scores = rerankerApi.rerank(query, candidates);
    // 按重排序分数重新排序
    return sortedResults;
}
```

---

## Phase 4: RAG 对话增强

### 当前

- 同步响应
- 无多轮对话
- 无历史记忆

### 新增功能

**1. 流式输出**

```java
// RagController
@PostMapping("/chat/stream")
public Flux<String> chatStream(RagChatRequest request) {
    // 检索 → 构建提示词 → 流式生成
    return chatClient.stream(prompt);
}
```

**2. 多轮对话**

```java
// RagChatRequest 增加
private String sessionId;  // 会话 ID

// RagSession 实体存储对话历史
@Entity
public class RagSession {
    private String id;
    private List<RagMessage> messages;  // 对话历史
    private String knowledgeBaseId;
    private LocalDateTime createdAt;
}
```

**3. 引用标注**

- 答案中标注来源段落（如 [1], [2]）
- 前端显示引用跳转

---

## Phase 5: 检索配置

### 当前

- topK 固定 5
- 阈值固定 0.5
- 无高级配置

### 新增配置

**RagChatRequest 增加**：

```java
private RetrievalStrategy strategy;  // VECTOR/BM25/HYBRID/RERANK
private Integer topK;                // 可配置
private Float threshold;             // 相似度阈值
private Float vectorWeight;          // 混合检索向量权重
private Float bm25Weight;            // 混合检索 BM25 权重
private String rerankModelId;        // 重排序模型
```

**前端配置面板**：

- 检索策略选择
- topK 滑块
- 阈值滑块
- 混合权重配置

---

## Phase 6: 知识库重索引

### 问题

切换 Embedding 模型后，旧文档无法检索。

### 解决方案

**重索引功能**：

```java
@PostMapping("/knowledge-bases/{id}/reindex")
public void reindexKnowledgeBase(String id, String newEmbeddingModelId) {
    // 1. 获取知识库所有文档
    // 2. 删除旧的向量数据
    // 3. 用新模型重新计算 embedding
    // 4. 存储到新向量表
}
```

---

## 实施优先级

| 优先级 | Phase     | 功能             | 工作量 |
|-----|-----------|----------------|-----|
| P0  | Phase 2   | Embedding 模型选择 | 中   |
| P0  | Phase 4.1 | 流式输出           | 低   |
| P1  | Phase 1   | 知识库管理          | 中   |
| P1  | Phase 5   | 检索配置           | 低   |
| P2  | Phase 3   | BM25/混合检索      | 高   |
| P2  | Phase 4.2 | 多轮对话           | 中   |
| P3  | Phase 3.4 | 重排序            | 高   |
| P3  | Phase 6   | 重索引            | 中   |

---

## 技术依赖

**BM25 实现**：

- PostgreSQL 全文搜索（tsvector/tsquery）
- 或 Elasticsearch

**重排序**：

- Cohere Rerank API
- 或本地 Reranker 模型（如 bge-reranker）

**混合检索融合**：

- Reciprocal Rank Fusion (RRF) 算法