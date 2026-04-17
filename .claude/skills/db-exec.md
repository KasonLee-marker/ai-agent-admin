# db-exec

执行 PostgreSQL 数据库 SQL 命令（Docker 环境）

## 使用方式

```
/db-exec <SQL语句>
```

示例：

```
/db-exec SELECT * FROM model_config;
/db-exec ALTER TABLE model_config ADD COLUMN new_field VARCHAR(100);
```

## 数据库配置

| 参数        | 值                |
|-----------|------------------|
| Docker 容器 | `agent-postgres` |
| 用户名       | `adminuser`      |
| 数据库       | `admindb`        |
| 端口        | `5432`           |

## 执行命令模板

```bash
docker exec agent-postgres psql -U adminuser -d admindb -c "<SQL语句>"
```

## 常用操作示例

### 查询表结构

```bash
docker exec agent-postgres psql -U adminuser -d admindb -c "\d <表名>"
```

### 查询约束

```bash
docker exec agent-postgres psql -U adminuser -d admindb -c "SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = '<表名>'::regclass;"
```

### 执行 DDL

```bash
docker exec agent-postgres psql -U adminuser -d admindb -c "ALTER TABLE <表名> ADD CONSTRAINT ..."
```

### 查看所有表

```bash
docker exec agent-postgres psql -U adminuser -d admindb -c "\dt"
```

### 查看索引

```bash
docker exec agent-postgres psql -U adminuser -d admindb -c "\di"
```

## 注意事项

- DDL 操作（ALTER TABLE、DROP 等）需要谨慎执行
- 建议先备份重要数据
- 使用 `-c` 参数执行单条 SQL，或 `-f` 执行文件中的 SQL