# AI Agent Admin

企业内网 AI Agent 管理平台，基于 Spring Boot + Spring AI + React 构建。

## 功能特性

- 📝 **Prompt 管理** - 模板 CRUD、版本控制、分类标签
- 🤖 **模型管理** - 多模型配置、动态切换、参数调整
- 💬 **对话调试** - 多轮对话、流式响应、RAG 检索增强、历史记录
- 📋 **知识库管理** - Embedding 模型绑定、文档上传、向量检索
- 📊 **数据集管理** - 导入导出、版本控制、数据项管理
- 📈 **评估系统** - 批量实验、RAG 评估、结果分析、多指标评分
- 🔍 **文档检索/RAG** - 文档上传、向量化、相似度检索、语义切分

## 技术栈

| 层级      | 技术                              | 版本                          |
|---------|---------------------------------|-----------------------------|
| 后端      | Spring Boot + Spring AI         | 3.2.12 + 0.8.1              |
| 前端      | React + TypeScript + Ant Design | 18.x + 5.3 + 5.x            |
| 主数据库    | PostgreSQL + pgvector           | 15+                         |
| 开发数据库   | H2                              | -                           |
| AI 模型   | Spring AI 支持的多供应商               | OpenAI、DashScope、DeepSeek 等 |
| 构建工具    | Maven                           | 3.9+                        |
| Java 版本 | OpenJDK                         | 17                          |

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Maven 3.9+
- Docker & Docker Compose（可选）

### 1. 克隆项目

```bash
git clone https://github.com/KasonLee-marker/ai-agent-admin.git
cd ai-agent-admin
```

### 2. 初始化数据库

#### 2.1 启动 PostgreSQL + pgvector + pg_jieba

```bash
# 构建带中文分词的镜像
cd docker/postgres-zhparser
docker build -t postgres-pgvector-jieba:pg15 .

# 启动容器（含 pgvector + pg_jieba 中文分词）
docker run -d \
  --name agentx-postgres \
  -e POSTGRES_USER=adminuser \
  -e POSTGRES_DB=admindb \
  -e POSTGRES_PASSWORD=adminpass123 \
  -p 5432:5432 \
  postgres-pgvector-jieba:pg15
```

> **包含扩展**：
> - pgvector - 向量存储，用于 Embedding 检索
> - pg_jieba - 中文分词，用于 BM25 中文全文搜索
> - pg_trgm - 三元组匹配，用于模糊搜索后备
>
> 详见 [docker/postgres-zhparser/README.md](./docker/postgres-zhparser/README.md)

#### 2.2 初始化数据库表结构

```bash
docker exec -i agentx-postgres psql -U adminuser -d admindb < docs/database/schema.sql
```

详见 [docs/database/README.md](./docs/database/README.md)

### 3. 本地开发

```bash
# 编译所有模块
mvn clean compile

# 运行测试
mvn test

# 启动后端（开发模式）
cd admin-server-start
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 启动前端（在另一个终端）
cd frontend
npm install
npm run dev
```

### 4. Docker 部署

```bash
# 启动所有服务
docker-compose up -d

# 访问地址
http://localhost:8080
```

## 项目结构

```
ai-agent-admin/
├── AGENTS.md                 # 项目导航（Agent Harness）
├── docs/                     # 文档
│   ├── architecture.md       # 架构文档
│   ├── api/                  # API 文档
│   ├── database/             # 数据库配置和初始化脚本
│   └── guides/               # 使用指南
├── admin-server-core/        # Core 模块（实体、枚举、常量）
│   └── src/main/java/com/aiagent/admin/domain/
│       ├── entity/           # PromptTemplate, ModelConfig, ChatSession, Dataset, DatasetItem...
│       └── enums/            # ModelProvider, MessageRole...
├── admin-server-runtime/     # Runtime 模块（业务逻辑、API、仓储）
│   └── src/main/java/com/aiagent/admin/
│       ├── api/controller/   # PromptController, ModelController, ChatController, DatasetController...
│       ├── api/dto/          # Request/Response DTOs
│       ├── domain/repository/# JPA Repositories
│       ├── service/          # Business services
│       └── service/mapper/   # MapStruct mappers
├── admin-server-start/       # Startup 模块（Spring Boot 主类）
│   └── src/main/java/com/aiagent/admin/AdminApplication.java
├── frontend/                 # React 前端
│   └── src/
├── tasks/                    # 任务跟踪
│   ├── active/               # 进行中
│   ├── completed/            # 已完成
│   └── backlog/              # 待办
└── .agent-harness/           # Agent Harness 配置
    ├── config.yaml
    └── constraints/
```

## 配置说明

### AI 模型配置

Spring AI 支持多种模型供应商，在 `application.yml` 中配置：

```yaml
# OpenAI 示例
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com
      chat:
        options:
          model: gpt-4

# 或 DashScope 示例
spring:
  ai:
    alibaba:
      dashscope:
        api-key: ${DASHSCOPE_API_KEY}
        base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
```

更多配置参考 [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)。


### 数据库配置

#### 开发环境（H2）

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:aiagent
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

#### 生产环境（PostgreSQL + pgvector）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aiagent
    username: aiagent_admin
    password: AiAgent@2026
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

**数据库初始化**：
```bash
# 使用提供的 SQL 脚本初始化数据库
docker exec -i agentx-postgres psql -U agentx -d aiagent < docs/database/schema.sql
```

详见 [docs/database/README.md](./docs/database/README.md)

## Agent Harness 工作流

本项目采用 Agent-first 开发模式：

1. **Human steers** - 人类定义需求和约束
2. **Agent executes** - Agent 自主实现功能
3. **Checkpoint review** - 检查点审查和反馈
4. **Iterate** - 迭代优化

查看 [AGENTS.md](./AGENTS.md) 了解项目导航和任务状态。

## 开发计划

| 模块        | 状态    | 优先级 |
|-----------|-------|-----|
| Prompt 管理 | ✅ 已完成 | P0  |
| 模型管理      | ✅ 已完成 | P0  |
| 对话调试      | ✅ 已完成 | P0  |
| 知识库管理     | ✅ 已完成 | P1  |
| 数据集管理     | ✅ 已完成 | P1  |
| 评估系统      | ✅ 已完成 | P1  |
| 文档检索/RAG  | ✅ 已完成 | P1  |
| 前端        | ✅ 已完成 | P1  |
| 可观测性      | ⏳ 待开始 | P2  |

## 贡献指南

1. 查看 `tasks/active/` 了解当前任务
2. 阅读 `AGENTS.md` 了解项目约束
3. 遵循 `.agent-harness/constraints/` 的代码规范
4. 提交 PR 前确保测试通过

## License

Apache License 2.0

## 相关链接

- [Spring AI](https://spring.io/projects/spring-ai)
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)
- [Agent Harness 文档](./docs/agent-harness.md)
