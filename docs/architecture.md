# AI Agent Admin 架构文档

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (React 18)                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Prompt 管理  │  │ 对话调试     │  │ 数据集/评估         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    API 网关层 (Spring Boot)                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Prompt API  │  │ Chat API    │  │ Dataset/Eval API    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ RAG API     │  │ Vector API  │  │ Document API        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    服务层 (Spring Service)                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │PromptService│  │ ChatService │  │ DatasetService      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ModelService │  │ EvalService │  │ VectorService       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │RAGService   │  │ DocService  │  │ Observability       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      数据层                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │    MySQL    │  │ PostgreSQL  │  │   本地文件存储       │  │
│  │  (主存储)    │  │ (向量+文档)  │  │   (数据集/日志)      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    AI 模型层                                  │
│         Spring AI 支持的多供应商 (OpenAI/DashScope等)         │
└─────────────────────────────────────────────────────────────┘
```

## 模块说明

### 1. Prompt 管理模块
- Prompt 模板 CRUD
- 版本控制（历史版本回溯）
- 分类标签管理
- 变量占位符支持

### 2. 模型管理模块
- 多模型配置（OpenAI、DashScope、DeepSeek 等）
- 动态参数调整（temperature、max_tokens 等）
- API Key 管理
- 模型健康检查

### 3. 对话调试模块
- 多轮对话会话
- 流式响应 (SSE)
- 对话历史记录
- Prompt 效果对比

### 4. 数据集管理模块
- 数据导入（JSON、CSV、Excel）
- 数据版本控制
- 数据项 CRUD
- 数据集导出

### 5. 评估模块
- 评估器配置（准确率、延迟、token 消耗）
- 批量实验执行
- 结果统计和可视化
- A/B 测试支持

### 6. 文档检索/RAG 模块 ⭐ 新增
- 文档上传（PDF、Word、TXT、Markdown）
- 自动分块和向量化
- 向量检索（相似度搜索）
- RAG 对话（检索增强生成）
- 支持 text-embedding-v1 等 Embedding 模型

### 7. 可观测性模块（简化）
- 调用日志记录
- 性能指标监控

## 技术选型

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.x |
| AI 框架 | Spring AI | 1.0.x |
| 主数据库 | MySQL / H2 | 8.0+ |
| 向量数据库 | PostgreSQL + pgvector | 15+ |
| 前端 | React | 18.x |
| UI 组件 | Ant Design | 5.x |
| 构建工具 | Maven / npm | - |

## 部署架构

```
┌─────────────────────────────────────────────┐
│              Docker Compose                  │
│  ┌─────────┐  ┌─────────┐  ┌─────────────┐ │
│  │  App    │  │  MySQL  │  │ PostgreSQL  │ │
│  │ Server  │  │         │  │  (pgvector) │ │
│  └─────────┘  └─────────┘  └─────────────┘ │
│  ┌─────────┐                                │
│  │  React  │                                │
│  │  Nginx  │                                │
│  └─────────┘                                │
└─────────────────────────────────────────────┘
```

单节点部署，适合企业内网使用。

## RAG/文档检索设计

### 数据流

```
文档上传 → 文本提取 → 分块 → Embedding → 向量存储(PostgreSQL)
                                              ↓
用户提问 → Embedding → 向量检索 → 召回TopK → 拼接Prompt → LLM生成
```

### 核心表结构

```sql
-- 文档表
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    content_type VARCHAR(50),
    total_chunks INT,
    metadata JSONB,
    created_at TIMESTAMP
);

-- 文档分块表（向量表）
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID REFERENCES documents(id),
    chunk_index INT,
    content TEXT,
    embedding VECTOR(1536),  -- text-embedding-v1 是 1536 维
    metadata JSONB,
    created_at TIMESTAMP
);

-- 创建向量索引
CREATE INDEX ON document_chunks USING ivfflat (embedding vector_cosine_ops);
```

### Embedding 模型支持

| 模型 | 维度 | 供应商 |
|------|------|--------|
| text-embedding-v1 | 1536 | DashScope |
| text-embedding-v2 | 1536 | DashScope |
| text-embedding-3-small | 1536 | OpenAI |
| text-embedding-3-large | 3072 | OpenAI |

### API 设计

```
# 文档管理
POST   /api/v1/documents              # 上传文档
GET    /api/v1/documents              # 列表
DELETE /api/v1/documents/{id}         # 删除
GET    /api/v1/documents/{id}/chunks  # 查看分块

# 向量检索
POST   /api/v1/vector/search          # 相似度搜索
POST   /api/v1/vector/rag             # RAG 对话

# Embedding 配置
GET    /api/v1/vector/models          # 支持的模型列表
PUT    /api/v1/vector/config          # 配置 Embedding 模型
```

## 与 Spring AI Alibaba 对比

| 功能 | Spring AI Alibaba | 本项目 |
|------|-------------------|--------|
| Prompt 管理 | ✅ | ✅ |
| 模型管理 | ✅ | ✅ |
| 对话调试 | ✅ | ✅ |
| 数据集管理 | ✅ | ✅ |
| 评估系统 | ✅ | ✅ |
| 文档检索/RAG | ✅ | ✅ (PostgreSQL + pgvector) |
| 向量存储 | 阿里云向量检索服务 | PostgreSQL pgvector |
| 服务注册 | Nacos | ❌ 内网单节点 |
| 分布式配置 | Nacos | ❌ 本地配置 |
| 链路追踪 | ARMS/OpenTelemetry | 简化版日志 |

## API 设计原则

1. RESTful 风格
2. 统一响应格式
3. 分页查询支持
4. 错误码规范

## 安全设计

1. 内网部署，简化认证
2. API Key 加密存储
3. 请求限流保护（本地内存限流）
4. 操作日志审计
