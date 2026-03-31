# AGENTS.md - AI Agent Admin 项目导航

## 项目概述

- **名称**: AI Agent Admin
- **技术栈**: Spring Boot 3.x + Spring AI + React 18 + MySQL/H2
- **目的**: 企业内网 AI Agent 管理平台（Prompt 管理、模型调用、对话调试、数据集管理、评估系统）

## 快速链接

- 架构文档: docs/architecture.md
- API 文档: docs/api/
- 活跃任务: tasks/active/
- 约束规则: .agent-harness/constraints/

## 关键约束

1. **内网部署**: 不使用 Nacos、服务注册等分布式组件
2. **百炼 API**: 使用 DashScope (qwen3.5-omni-plus) 作为默认模型
3. **简化架构**: 单节点部署，配置本地化
4. **质量门禁**: 所有代码需通过单元测试和代码审查

## 模块清单

| 模块 | 状态 | 优先级 |
|------|------|--------|
| Prompt 管理 | ✅ 已完成 | P0 |
| 模型管理 | ✅ 已完成 | P0 |
| 对话调试 | ✅ 已完成 | P0 |
| 数据集管理 | 🔄 待开始 | P1 |
| 评估系统 | 🔄 待开始 | P1 |
| 文档检索/RAG | 🔄 待开始 | P1 |
| 可观测性 | 🔄 待开始 | P2 |

## 项目结构 (Multi-Module Maven)

```
ai-agent-admin/
├── pom.xml                          # Parent POM
├── admin-server-core/               # Core module (entities, enums, constants)
│   └── src/main/java/com/aiagent/admin/domain/
│       ├── entity/                  # PromptTemplate, PromptVersion, ModelConfig, ChatSession, ChatMessage
│       └── enums/                   # ModelProvider, MessageRole
├── admin-server-runtime/            # Runtime module (services, repositories, controllers)
│   └── src/main/java/com/aiagent/admin/
│       ├── api/controller/          # PromptController, ModelController, ChatController
│       ├── api/dto/                 # Request/Response DTOs
│       ├── api/exception/           # GlobalExceptionHandler
│       ├── domain/repository/       # JPA Repositories
│       ├── service/                 # Business services
│       └── service/mapper/          # MapStruct mappers
├── admin-server-start/              # Startup module (main class)
│   └── src/main/java/com/aiagent/admin/AdminApplication.java
└── frontend/                        # Frontend (React)
```

## 模块依赖关系

```
admin-server-start -> admin-server-runtime -> admin-server-core
```

## 技术栈

- **后端**: Spring Boot 3.2.x + Spring AI 0.8.1
- **前端**: React 18 + Ant Design
- **主数据库**: H2 (dev) / PostgreSQL (prod)
- **构建工具**: Maven 3.9+
- **Java版本**: 17

## 构建与运行

```bash
# 编译所有模块
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package

# 运行应用
cd admin-server-start
mvn spring-boot:run
```

## 技术栈

- **后端**: Spring Boot 3.2.x + Spring AI
- **前端**: React 18 + Ant Design
- **主数据库**: MySQL / H2
- **向量数据库**: PostgreSQL + pgvector
- **Embedding**: text-embedding-v1 (DashScope) 等
- **部署**: Docker Compose 单节点

## 任务工作流

1. 读取 tasks/active/<task>.md 获取上下文
2. 按照约束规则实现功能
3. 自审查通过质量门禁
4. 更新任务状态并报告完成
5. 将任务移动到 tasks/completed/

## 百炼 API 配置

```yaml
model: qwen3.5-omni-plus-2026-03-15
api-key: sk-852f050ac5514871b39b3e8d7ffcc490
base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
```

## 沟通方式

- 进度更新: 追加到活跃任务文件
- 阻塞问题: 立即标记 [BLOCKER] 前缀
- 疑问: 在任务文件的 [QUESTION] 部分提出
