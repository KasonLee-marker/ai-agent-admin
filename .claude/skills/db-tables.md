# db-tables

查看数据库所有表和表结构

## 使用方式

```
/db-tables [表名]
```

- 不带参数：显示所有表列表
- 带表名：显示指定表的结构

## 执行步骤

### 查看所有表

```bash
docker exec agent-postgres psql -U adminuser -d admindb -c "\dt"
```

### 查看表结构

```bash
docker exec agent-postgres psql -U adminuser -d admindb -c "\d <表名>"
```

## 常用表名

- `model_config` - 模型配置
- `prompt_templates` - 提示词模板
- `datasets` - 数据集
- `dataset_items` - 数据集项
- `evaluation_jobs` - 评估任务
- `evaluation_results` - 评估结果
- `documents` - 文档（知识库）
- `document_chunks` - 文档分块
- `chat_sessions` - 对话会话
- `chat_messages` - 对话消息