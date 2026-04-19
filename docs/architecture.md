# AI Agent Admin 架构文档

## 系统架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    前端层 (React 18 + TypeScript)            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Prompt 管理  │  │ 对话调试     │  │ 数据集/评估         │  │
│  │             │  │ (含RAG增强)  │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐                            │
│  │ 知识库管理   │  │ 模型管理     │                            │
│  └─────────────┘  └─────────────┘                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    API 网关层 (Spring Boot)                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Prompt API  │  │ Chat API    │  │ Dataset/Eval API    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Knowledge   │  │ Vector API  │  │ Document API        │  │
│  │ Base API    │  │             │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    服务层 (Spring Service)                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │PromptService│  │ ChatService │  │ DatasetService      │  │
│  │             │  │ (含RAG集成) │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ModelService │  │ EvalService │  │ VectorService       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │RAGService   │  │ DocService  │  │ KnowledgeBaseService│  │
│  │(检索组件)   │  │             │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐                                              │
│  │EmbeddingSvc │                                              │
│  └─────────────┘                                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      数据层                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │    H2       │  │ PostgreSQL  │  │   本地文件存储       │  │
│  │  (开发)      │  │ (生产+向量)  │  │   (数据集/日志)      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    AI 模型层                                  │
│         Spring AI 支持的多供应商 (OpenAI/DashScope等)         │
└─────────────────────────────────────────────────────────────┘
```

### 后端模块结构 (Multi-Module Maven)

```
ai-agent-admin/
├── pom.xml                          # Parent POM - 依赖管理
├── admin-server-core/               # 核心模块 - 实体、枚举、常量
│   └── src/main/java/com/aiagent/admin/domain/
│       ├── entity/
│       │   ├── PromptTemplate.java  # Prompt 模板实体
│       │   ├── PromptVersion.java   # Prompt 版本实体
│       │   └── ModelConfig.java     # 模型配置实体
│       └── enums/
│           └── ModelProvider.java   # 模型供应商枚举
├── admin-server-runtime/            # 运行时模块 - 业务逻辑
│   └── src/main/java/com/aiagent/admin/
│       ├── api/
│       │   ├── controller/          # REST API 控制器
│       │   │   ├── PromptController.java
│       │   │   └── ModelController.java
│       │   ├── dto/                 # 数据传输对象
│       │   │   ├── PromptTemplateCreateRequest.java
│       │   │   ├── PromptTemplateResponse.java
│       │   │   ├── CreateModelRequest.java
│       │   │   └── ModelResponse.java
│       │   └── exception/
│       │       └── GlobalExceptionHandler.java
│       ├── domain/repository/       # JPA 仓储接口
│       │   ├── PromptTemplateRepository.java
│       │   ├── PromptVersionRepository.java
│       │   └── ModelConfigRepository.java
│       ├── service/                 # 业务服务层
│       │   ├── PromptService.java
│       │   ├── PromptServiceImpl.java
│       │   ├── ModelConfigService.java
│       │   ├── ModelService.java
│       │   ├── EncryptionService.java
│       │   ├── IdGenerator.java
│       │   └── HealthCheckService.java
│       └── service/mapper/          # MapStruct 映射器
│           ├── PromptMapper.java
│           └── ModelMapper.java
└── admin-server-start/              # 启动模块
    └── src/main/java/com/aiagent/admin/
        └── AdminApplication.java    # Spring Boot 主类
```

### 模块依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                    模块依赖图                                │
│                                                             │
│   ┌──────────────────┐                                      │
│   │ admin-server-start│  ← 启动入口                         │
│   └────────┬─────────┘                                      │
│            │ depends on                                     │
│            ▼                                                │
│   ┌──────────────────┐                                      │
│   │admin-server-runtime│ ← 业务逻辑、API、仓储               │
│   └────────┬─────────┘                                      │
│            │ depends on                                     │
│            ▼                                                │
│   ┌──────────────────┐                                      │
│   │ admin-server-core │ ← 实体、枚举、常量                   │
│   └──────────────────┘                                      │
│                                                             │
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

### 4. 数据集管理模块 ⭐ 增强

#### 基础功能
- 数据导入（JSON、CSV、Excel）
- 数据版本控制
- 数据项 CRUD
- 数据集导出

### 5. 评估模块 ⭐ 增强

#### 基础评估模式
- 评估器配置（准确率、延迟、token 消耗）
- 批量实验执行
- 结果统计和可视化
- A/B 测试支持

#### RAG评估模式 ✅ 新增

- **知识库关联**: 评估任务可关联知识库进行RAG评估
- **语义相似度**: Embedding余弦相似度计算
- **事实忠实度**: LLM评估答案是否忠实于检索内容
- **多维度评分**: AI评分 + 相似度 + 忠实度

#### 评估指标体系 ✅ 新增

| 指标    | 方法           | 范围    | 说明          |
|-------|--------------|-------|-------------|
| AI得分  | LLM-as-Judge | 0-100 | 质量评分        |
| 语义相似度 | Embedding余弦  | 0-1   | 期望与实际的向量相似度 |
| 忠实度   | LLM评估        | 0-1   | 答案是否忠实于上下文  |

详细功能文档: [evaluation-rag-feature.md](evaluation-rag-feature.md)

#### 模型类型区分 ⏳ 待完成

系统中存在两种模型类型：

- **Chat 模型**: 用于对话、评估生成（OPENAI, ANTHROPIC, DASHSCOPE 等）
- **Embedding 模型**: 用于向量计算（OPENAI_EMBEDDING, DASHSCOPE_EMBEDDING）

需要完善的功能：

1. 模型管理页面分组显示 Provider
2. 支持设置默认 Embedding 模型
3. EmbeddingService 只使用 Embedding 类型配置

### 6. 文档检索/RAG 模块 ⭐ 增强

#### 基础功能
- 文档上传（PDF、Word、TXT、Markdown）
- 自动分块和向量化
- 向量检索（相似度搜索）
- RAG 对话（检索增强生成）

#### Embedding服务 ✅ 新增

- **EmbeddingService**: 提供文本向量计算能力
- **向量存储**: DocumentChunk.embedding 字段存储向量
- **相似度搜索**: 余弦相似度计算，支持TopK检索
- **语义相似度**: 文本语义相似度计算

#### 支持的Embedding模型

| Provider            | 模型                     | 维度   |
|---------------------|------------------------|------|
| OPENAI_EMBEDDING    | text-embedding-ada-002 | 1536 |
| OPENAI_EMBEDDING    | text-embedding-3-small | 1536 |
| OPENAI_EMBEDDING    | text-embedding-3-large | 3072 |
| DASHSCOPE_EMBEDDING | text-embedding-v1      | 1024 |
| DASHSCOPE_EMBEDDING | text-embedding-v2      | 1536 |
| DASHSCOPE_EMBEDDING | text-embedding-v3      | 1024 |

### 7. 可观测性模块（简化）
- 调用日志记录
- 性能指标监控

## 技术选型

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.12 |
| AI 框架 | Spring AI | 0.8.1 |
| Java 版本 | OpenJDK | 17 |
| 主数据库 | H2 (dev) / PostgreSQL (prod) | - |
| 向量数据库 | PostgreSQL + pgvector | 15+ |
| ORM 框架 | Spring Data JPA | - |
| API 文档 | SpringDoc OpenAPI | 2.3.0 |
| 对象映射 | MapStruct | 1.5.5 |
| 加密 | Jasypt | 3.0.5 |
| 前端 | React | 18.x |
| UI 组件 | Ant Design | 5.x |
| 构建工具 | Maven | 3.9+ |

## 部署架构

```
┌─────────────────────────────────────────────┐
│              Docker Compose                  │
│  ┌─────────┐  ┌─────────┐  ┌─────────────┐ │
│  │  App    │  │   H2    │  │ PostgreSQL  │ │
│  │ Server  │  │  (dev)  │  │  (pgvector) │ │
│  │ (JAR)   │  │         │  │   (prod)    │ │
│  └─────────┘  └─────────┘  └─────────────┘ │
│  ┌─────────┐                                │
│  │  React  │                                │
│  │  Nginx  │                                │
│  └─────────┘                                │
└─────────────────────────────────────────────┘
```

单节点部署，适合企业内网使用。

## 构建与运行

### 本地开发

```bash
# 1. 克隆项目
git clone <repo-url>
cd ai-agent-admin

# 2. 编译所有模块
mvn clean compile

# 3. 运行测试
mvn test

# 4. 启动应用
cd admin-server-start
mvn spring-boot:run

# 5. 访问 API 文档
open http://localhost:8080/swagger-ui.html
```

### 生产打包

```bash
# 打包所有模块
mvn clean package -DskipTests

# 生成的 JAR 文件位置
admin-server-start/target/admin-server-start-1.0.0-SNAPSHOT.jar
```

### Docker 部署

```bash
# 构建镜像
docker build -t ai-agent-admin:latest .

# 运行容器
docker run -p 8080:8080 \
  -e JASYPT_PASSWORD=your-secret \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=admindb \
  -e DB_USER=admin \
  -e DB_PASSWORD=secret \
  ai-agent-admin:latest
```

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
