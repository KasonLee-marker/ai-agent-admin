# db-query

快速查询数据库表数据

## 使用方式

```
/db-query <表名> [limit]
```

示例：

```
/db-query model_config        # 查询前 10 条
/db-query model_config 20     # 查询前 20 条
```

## 执行命令

```bash
docker exec agentx-postgres psql -U adminuser -d admindb -c "SELECT * FROM <表名> LIMIT <数量>;"
```

默认 LIMIT 为 10。

## 常用查询示例

### 模型配置

```bash
docker exec agentx-postgres psql -U adminuser -d admindb -c "SELECT id, name, provider, model_name, is_default, health_status FROM model_config;"
```

### 评估任务

```bash
docker exec agentx-postgres psql -U adminuser -d admindb -c "SELECT id, name, status, total_items, completed_items FROM evaluation_jobs ORDER BY created_at DESC LIMIT 10;"
```

### 数据集

```bash
docker exec agentx-postgres psql -U adminuser -d admindb -c "SELECT id, name, status, item_count FROM datasets;"
```

### 知识库文档

```bash
docker exec agentx-postgres psql -U adminuser -d admindb -c "SELECT id, name, status, chunk_count FROM documents;"
```

### 对话会话

```bash
docker exec agentx-postgres psql -U adminuser -d admindb -c "SELECT id, title, status, created_at FROM chat_sessions ORDER BY created_at DESC LIMIT 5;"
```