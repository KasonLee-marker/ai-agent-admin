# Task: 文档检索/RAG 服务实现

## 目标
实现文档检索和 RAG（检索增强生成）模块，支持文档上传、自动向量化、相似度检索和 RAG 对话。

## 上下文
参考 Spring AI Alibaba 的文档检索功能，使用 PostgreSQL + pgvector 作为向量数据库，支持 text-embedding-v1 等 Embedding 模型。

## 需求

### 功能需求
- [ ] 文档上传（PDF、Word、TXT、Markdown）
- [ ] 文本提取和清洗
- [ ] 自动分块（可配置分块大小）
- [ ] 向量化存储（PostgreSQL pgvector）
- [ ] 相似度检索（向量搜索）
- [ ] RAG 对话（检索 + LLM 生成）
- [ ] Embedding 模型配置（支持多模型切换）

### 技术需求
- [ ] PostgreSQL 15+ with pgvector 扩展
- [ ] Spring AI Vector Store 抽象
- [ ] 支持 text-embedding-v1 (1536维)
- [ ] 向量索引优化（IVFFlat）
- [ ] 单元测试覆盖率 > 70%

## 数据模型

```java
// Document 实体
@Entity
public class Document {
    @Id
    private String id;
    private String name;
    private String contentType;
    private Long fileSize;
    private String filePath;
    private Integer totalChunks;
    private String embeddingModel; // 使用的模型
    private DocumentStatus status; // PROCESSING, COMPLETED, FAILED
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// DocumentChunk 实体（向量存储）
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {
    @Id
    private String id;
    private String documentId;
    private Integer chunkIndex;
    @Column(length = 5000)
    private String content;
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding; // pgvector 存储
    private String metadata; // JSON
    private LocalDateTime createdAt;
}
```

## API 设计

```
# 文档管理
POST   /api/v1/documents              # 上传文档
GET    /api/v1/documents              # 列表（支持分页、筛选）
GET    /api/v1/documents/{id}         # 详情
DELETE /api/v1/documents/{id}         # 删除
GET    /api/v1/documents/{id}/chunks  # 查看分块
GET    /api/v1/documents/{id}/status  # 处理状态

# 向量检索
POST   /api/v1/vector/search          # 相似度搜索
POST   /api/v1/vector/rag             # RAG 对话

# Embedding 配置
GET    /api/v1/vector/models          # 支持的模型列表
GET    /api/v1/vector/config          # 当前配置
PUT    /api/v1/vector/config          # 更新配置
```

## 配置

```yaml
# application.yml
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: ivfflat
        distance-type: cosine_distance
        dimensions: 1536
        table-name: document_chunks
        schema-name: public
        initialize-schema: true
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      embedding:
        options:
          model: text-embedding-v1
```

## PostgreSQL 初始化

```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 文档表
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50),
    file_size BIGINT,
    file_path VARCHAR(500),
    total_chunks INT DEFAULT 0,
    embedding_model VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PROCESSING',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 文档分块表（向量表）
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(1536),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 向量索引（IVFFlat，适合中等规模数据）
CREATE INDEX ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 文档ID索引
CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);
```

## 约束

- 使用 PostgreSQL + pgvector，不依赖外部向量服务
- 支持多种 Embedding 模型（可配置）
- 文档处理异步执行（避免阻塞）
- 单文件大小限制（如 50MB）

## 完成标准

- [ ] 所有 API 实现并通过测试
- [ ] 支持 PDF、Word、TXT、Markdown 格式
- [ ] 向量化检索准确率 > 90%
- [ ] RAG 对话功能完整
- [ ] 单元测试覆盖率 > 70%

## 检查点

- Checkpoint 1: PostgreSQL + pgvector 环境搭建
- Checkpoint 2: 文档上传和文本提取
- Checkpoint 3: 分块和向量化实现
- Checkpoint 4: 向量检索和 RAG 对话
- Checkpoint 5: 测试和文档完成

## 进度日志

### Checkpoint 1 - 待开始
Status: [PENDING]
Completed: 
Next: 搭建 PostgreSQL + pgvector 环境
Blockers: None
