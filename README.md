# AI Agent Admin

企业内网 AI Agent 管理平台，基于 Spring Boot + Spring AI + React 构建。

## 功能特性

- 📝 **Prompt 管理** - 模板 CRUD、版本控制、分类标签
- 🤖 **模型管理** - 多模型配置、动态切换、参数调整
- 💬 **对话调试** - 多轮对话、流式响应、历史记录
- 📊 **数据集管理** - 导入导出、版本控制、数据项管理
- 📈 **评估系统** - 批量实验、结果分析、A/B 测试
- 🔍 **可观测性** - 调用日志、链路追踪（简化版）

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端 | Spring Boot + Spring AI | 3.2.x |
| 前端 | React + Ant Design | 18.x |
| 数据库 | MySQL / H2 | 8.0+ |
| 缓存 | Redis | 7.x |
| AI 模型 | 百炼 DashScope | qwen3.5-omni-plus |

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Maven 3.8+
- Docker & Docker Compose（可选）

### 本地开发

```bash
# 克隆项目
git clone https://github.com/KasonLee-marker/ai-agent-admin.git
cd ai-agent-admin

# 启动后端（开发模式）
cd src/backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 启动前端
cd src/frontend
npm install
npm run dev
```

### Docker 部署

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
│   └── guides/               # 使用指南
├── src/
│   ├── backend/              # Spring Boot 后端
│   │   ├── prompt-service/   # Prompt 管理服务
│   │   ├── model-service/    # 模型管理服务
│   │   ├── chat-service/     # 对话服务
│   │   ├── dataset-service/  # 数据集服务
│   │   └── eval-service/     # 评估服务
│   └── frontend/             # React 前端
│       └── src/
├── tasks/                    # 任务跟踪
│   ├── active/               # 进行中
│   ├── completed/            # 已完成
│   └── backlog/              # 待办
├── tests/                    # 测试
└── .agent-harness/           # Agent Harness 配置
    ├── config.yaml
    └── constraints/
```

## 配置说明

### 百炼 API 配置

```yaml
# application.yml
spring:
  ai:
    alibaba:
      dashscope:
        api-key: ${DASHSCOPE_API_KEY}
        base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
        model: qwen3.5-omni-plus-2026-03-15
```

### 数据库配置

```yaml
# 开发环境使用 H2
spring:
  datasource:
    url: jdbc:h2:mem:aiagent
    driver-class-name: org.h2.Driver
    username: sa
    password:

# 生产环境使用 MySQL
# spring:
#   datasource:
#     url: jdbc:mysql://localhost:3306/aiagent
#     username: root
#     password: password
```

## Agent Harness 工作流

本项目采用 Agent-first 开发模式：

1. **Human steers** - 人类定义需求和约束
2. **Agent executes** - Agent 自主实现功能
3. **Checkpoint review** - 检查点审查和反馈
4. **Iterate** - 迭代优化

查看 [AGENTS.md](./AGENTS.md) 了解项目导航和任务状态。

## 开发计划

| 模块 | 状态 | 优先级 |
|------|------|--------|
| Prompt 管理 | 🔄 进行中 | P0 |
| 模型管理 | ⏳ 待开始 | P0 |
| 对话调试 | ⏳ 待开始 | P0 |
| 数据集管理 | ⏳ 待开始 | P1 |
| 评估系统 | ⏳ 待开始 | P1 |
| 可观测性 | ⏳ 待开始 | P2 |

## 贡献指南

1. 查看 `tasks/active/` 了解当前任务
2. 阅读 `AGENTS.md` 了解项目约束
3. 遵循 `.agent-harness/constraints/` 的代码规范
4. 提交 PR 前确保测试通过

## License

Apache License 2.0

## 相关链接

- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)
- [百炼 DashScope](https://dashscope.aliyun.com/)
- [Agent Harness 文档](./docs/agent-harness.md)
