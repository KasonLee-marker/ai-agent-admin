# 数据库配置

## 环境信息

- **数据库**: PostgreSQL 15 + pgvector
- **容器名**: agent-postgres
- **端口**: 5432
- **数据库名**: admindb
- **用户名**: adminuser
- **密码**: adminpass123

## 快速开始

### 1. 启动 PostgreSQL 容器

```bash
docker run -d \
  --name agent-postgres \
  -e POSTGRES_USER=adminuser \
  -e POSTGRES_DB=admindb \
  -p 5432:5432 \
  pgvector/pgvector:pg15
```

### 2. 初始化数据库

```bash
# 进入容器执行 SQL
docker exec -i agent-postgres psql -U adminuser -d admindb < docs/database/schema.sql
```

### 3. 验证连接

```bash
# 本地连接
docker exec -it agent-postgres psql -U adminuser -d admindb

# 或者使用 psql 客户端
psql -h localhost -U adminuser -d admindb
```

## 表结构

### 核心模块

| 表名 | 说明 |
|------|------|
| prompt_templates | Prompt 模板 |
| prompt_versions | Prompt 版本历史 |
| model_configs | AI 模型配置 |
| chat_sessions | 对话会话 |
| chat_messages | 对话消息 |
| datasets | 数据集 |
| dataset_items | 数据集项 |
| documents | 文档（RAG） |

## 应用配置

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
```

## 向量搜索示例

```sql
-- 插入带向量的文档
INSERT INTO documents (name, content, embedding)
VALUES ('doc1', '内容', '[0.1, 0.2, ...]'::vector);

-- 相似度搜索
SELECT id, name, content, 
       embedding <=> '[0.1, 0.2, ...]'::vector AS distance
FROM documents
ORDER BY distance
LIMIT 5;
```
