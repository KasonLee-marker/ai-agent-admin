# 数据库配置

## 环境信息

- **数据库**: PostgreSQL 15 + pgvector
- **容器名**: agentx-postgres
- **端口**: 5432
- **数据库名**: admindb
- **用户名**: adminuser
- **密码**: adminpass123

## 快速开始

### 1. 启动 PostgreSQL 容器

```bash
docker run -d \
  --name agentx-postgres \
  -e POSTGRES_USER=adminuser \
  -e POSTGRES_PASSWORD=adminpass123 \
  -e POSTGRES_DB=admindb \
  -p 5432:5432 \
  pgvector/pgvector:pg15
```

### 2. 初始化数据库

```bash
# 进入容器执行 SQL
docker exec -i agentx-postgres psql -U adminuser -d admindb < docs/database/schema.sql
```

### 3. 验证连接

```bash
# 本地连接
docker exec -it agentx-postgres psql -U adminuser -d admindb

# 或者使用 psql 客户端
psql -h localhost -U adminuser -d admindb
```

### 4. 导出数据库结构

```bash
# 导出建表语句（不含数据）
docker exec -i agentx-postgres pg_dump -U adminuser -d admindb --schema-only --no-owner --no-privileges > docs/database/schema.sql
```

## 表结构

### 核心模块

| 模块            | 表名                       | 说明                                  |
|---------------|--------------------------|-------------------------------------|
| **Prompt 管理** | prompt_templates         | Prompt 模板                           |
|               | prompt_versions          | Prompt 版本历史                         |
| **模型管理**      | model_config             | AI 模型配置（对话 + Embedding）             |
| **对话调试**      | chat_sessions            | 对话会话                                |
|               | chat_messages            | 对话消息                                |
| **数据集管理**     | datasets                 | 数据集                                 |
|               | dataset_items            | 数据集项（支持 context 和 expected_doc_ids） |
| **评估系统**      | evaluation_jobs          | 评估任务（支持 RAG 评估）                     |
|               | evaluation_results       | 评估结果（含语义相似度、忠实度等指标）                 |
| **知识库管理**     | knowledge_bases          | 知识库（支持重索引）                          |
| **文档检索**      | documents                | 文档（支持多种分块策略）                        |
|               | document_chunks          | 文档分块（文本 + 全文搜索）                     |
|               | document_embeddings_1024 | 文档向量（1024 维）                        |
| **RAG 对话**    | rag_sessions             | RAG 会话                              |
|               | rag_messages             | RAG 对话消息（含检索来源）                     |

## 数据库特性

### 1. 向量搜索

使用 pgvector 扩展支持向量相似度搜索：

```sql
-- 插入向量（1024 维）
INSERT INTO document_embeddings_1024 (chunk_id, document_id, embedding)
VALUES ('chunk-1', 'doc-1', '[0.1, 0.2, ...]'::vector(1024));

-- 余弦相似度搜索
SELECT chunk_id,
       document_id,
       embedding <=> '[0.1, 0.2, ...]'::vector(1024) AS distance
FROM document_embeddings_1024
ORDER BY distance
LIMIT 5;
```

### 2. 全文搜索

使用 PostgreSQL 内置的全文搜索功能：

```sql
-- 搜索文档分块
SELECT id, content, document_id
FROM document_chunks
WHERE content_tsv @@ to_tsquery('simple', '搜索关键词')
ORDER BY ts_rank(content_tsv, to_tsquery('simple', '搜索关键词')) DESC
LIMIT 10;
```

### 3. 约束检查

所有状态字段都使用 CHECK 约束保证数据一致性：

- **数据集状态**: DRAFT, ACTIVE, ARCHIVED, DELETED
- **评估任务状态**: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
- **评估结果状态**: PENDING, SUCCESS, FAILED
- **文档状态**: PROCESSING, SEMANTIC_PROCESSING, CHUNKED, EMBEDDING, COMPLETED, FAILED, DELETED
- **模型供应商**: OPENAI, ANTHROPIC, DASHSCOPE, DEEPSEEK, OLLAMA 等

## 应用配置

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/admindb
    username: adminuser
    password: adminpass123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # 使用 validate，不自动建表
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
    open-in-view: false
```

### 环境变量方式（推荐生产环境）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:admindb}
    username: ${DB_USERNAME:adminuser}
    password: ${DB_PASSWORD:adminpass123}
    driver-class-name: org.postgresql.Driver
```

## 常用维护命令

### 查看表大小

```sql
SELECT schemaname,
       tablename,
       pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC;
```

### 查看索引使用情况

```sql
SELECT schemaname,
       tablename,
       indexname,
       idx_scan,
       idx_tup_read,
       idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

### 清理过期数据

```sql
-- 删除已删除的数据集项
DELETE
FROM dataset_items
WHERE status = 'DELETED';

-- 删除已删除的文档及其分块
DELETE
FROM document_chunks
WHERE document_id IN (SELECT id
                      FROM documents
                      WHERE status = 'DELETED');
DELETE
FROM documents
WHERE status = 'DELETED';
```

## 索引说明

### 主要索引

| 索引名 | 表 | 字段 | 类型 | 用途 |
|--------|------|------|------|------|
| idx_dataset_id | dataset_items | dataset_id | B-Tree | 数据集项查询 |
| idx_dataset_version | dataset_items | dataset_id, version | B-Tree | 版本过滤 |
| idx_job_status | evaluation_jobs | status | B-Tree | 任务状态过滤 |
| idx_result_job_id | evaluation_results | job_id | B-Tree | 结果查询 |
| idx_document_chunks_tsv | document_chunks | content_tsv | GIN | 全文搜索 |
| idx_document_embeddings_1024_embedding | document_embeddings_1024 | embedding | IVFFlat | 向量相似度搜索 |

### 向量索引参数

- **lists=100**: IVFFlat 索引的列表数量
- 适用于数据集 < 1M 条记录
- 更大数据集建议使用 HNSW 索引

## 数据备份与恢复

### 备份

```bash
# 完整备份（含数据）
docker exec -i agentx-postgres pg_dump -U adminuser -d admindb > backup_$(date +%Y%m%d).sql

# 仅结构备份
docker exec -i agentx-postgres pg_dump -U adminuser -d admindb --schema-only > schema_backup.sql
```

### 恢复

```bash
# 从备份恢复
docker exec -i agentx-postgres psql -U adminuser -d admindb < backup_20260418.sql
```

## 故障排查

### 连接问题

```bash
# 检查容器状态
docker ps | grep postgres

# 查看容器日志
docker logs agentx-postgres

# 测试连接
docker exec -it agentx-postgres psql -U adminuser -d admindb -c "SELECT version();"
```

### 向量扩展问题

```sql
-- 检查 vector 扩展
SELECT *
FROM pg_extension
WHERE extname = 'vector';

-- 如果没有安装
CREATE EXTENSION IF NOT EXISTS vector;
```

## 开发建议

1. **使用 validate 模式**: 生产环境使用 `ddl-auto: validate`，由 schema.sql 管理表结构
2. **版本控制**: 所有数据库变更通过 schema.sql 版本控制
3. **定期导出**: 开发完成后及时导出最新结构到 schema.sql
4. **索引优化**: 根据查询模式添加合适的索引，避免过度索引
5. **向量维度**: 保持 embedding 维度与模型配置一致（常用 1024 或 1536）
