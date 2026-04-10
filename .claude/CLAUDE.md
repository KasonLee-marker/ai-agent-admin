# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Harness Engineering Workflow

本项目采用 **Agent-first 开发模式**，基于 Harness Engineering 理论构建。

### 核心原则

1. **Human steers** - 人类定义需求和约束
2. **Agent executes** - Agent 自主实现功能
3. **Checkpoint review** - 检查点审查和反馈
4. **Iterate** - 迭代优化

### 任务驱动开发

任务文件位于 `tasks/` 目录：

- `tasks/active/` - 进行中的任务
- `tasks/completed/` - 已完成的任务
- `tasks/backlog/` - 待办任务

**任务文件结构**：

```markdown
# Task: <任务名称>

## 目标

## 上下文

## 需求（功能需求 + 技术需求）

## 约束

## 完成标准

## 检查点 (Checkpoint 1, 2, 3...)

## 进度日志 (Progress Log)

## 状态: [PENDING|IN_PROGRESS|BLOCKED|READY_FOR_REVIEW|COMPLETED]
```

### 检查点机制

每个任务划分为多个 Checkpoint，完成后在任务文件的 **进度日志** 中记录：

```markdown
### Checkpoint N - <timestamp>

Status: [COMPLETE|IN_PROGRESS|BLOCKED]
Branch: feature/<name>
Completed:

- <item 1>
- <item 2>
  Next: <next step>
  Blockers: <blockers or "None">
```

### 质量门禁

代码提交前必须通过：

- [ ] 测试覆盖率 >= 70%
- [ ] 无 SpotBugs 高危警告
- [ ] 代码风格合规
- [ ] API 文档完整
- [ ] 分层架构正确 (domain → service → api)
- [ ] 无禁止的依赖关系
- [ ] 配置外置（无硬编码）

### 沟通规则

在任务文件中使用特定标记：

- **进度更新**: 追加到 Progress Log
- **阻塞问题**: 标记 `[BLOCKER]` 前缀
- **疑问**: 标记 `[QUESTION]` 前缀
- **完成**: 标记 `[READY_FOR_REVIEW]`

### 架构约束

来自 `.agent-harness/config.yaml`：

```
分层架构:
  domain → 无外部依赖
  service → 可依赖 domain
  api → 可依赖 service + domain

禁止:
  - service 层导入 api 层
  - API 层直接访问数据库
  - Nacos/服务注册/Redis（内网单节点部署）
```

### 工作流程

1. 读取 `tasks/active/<task>.md` 获取任务上下文
2. 创建 feature 分支: `git checkout -b feature/<task-name>`
3. 按 Checkpoint 顺序实现功能
4. 每个 Checkpoint 完成后更新进度日志
5. 自检通过质量门禁: `mvn test`
6. 标记 `[READY_FOR_REVIEW]`，等待人工审查
7. 审查通过后合并到 main，删除 feature 分支
8. 移动任务文件到 `tasks/completed/`

## Build Commands

### Backend (Maven Multi-Module)

```bash
# Compile all modules
mvn clean compile

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PromptServiceImplTest

# Run single test method
mvn test -Dtest=PromptServiceImplTest#createPrompt_Success

# Package (skip tests for faster builds)
mvn clean package -DskipTests

# Start application (dev profile with H2)
cd admin-server-start && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Start application (prod profile with PostgreSQL)
cd admin-server-start && mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Frontend (React + Vite)

```bash
cd frontend
npm install          # Install dependencies
npm run dev          # Dev server on port 5173 (proxies /api to :8080)
npm run build        # Production build (tsc + vite)
npm run lint         # ESLint check
npm run preview      # Preview production build
```

## Architecture Overview

### Module Structure (Maven)

```
admin-server-start → admin-server-runtime → admin-server-core
```

- **core**: Entities, enums, constants (no Spring dependencies except JPA)
- **runtime**: Controllers, services, repositories, DTOs, mappers
- **start**: Spring Boot main class, application.yml, profile-specific configs

### Backend Patterns

- **Controllers**: REST endpoints under `/api/v1/`, use `@RestController`, return `ApiResponse<T>` wrapper
- **Services**: Interface + Impl pattern (e.g., `PromptService` + `PromptServiceImpl`)
- **Entities**: Lombok `@Data @Builder`, JPA with UUID primary keys, `@CreationTimestamp/@UpdateTimestamp`
- **Mappers**: MapStruct for DTO ↔ Entity conversion
- **Testing**: JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`, mock repositories and mappers

### Frontend Patterns

- **State**: Zustand stores in `src/stores/` (e.g., `authStore.ts`)
- **API**: Axios client in `src/api/client.ts`, baseURL `/api/v1`, token in localStorage
- **Routing**: React Router with `ProtectedRoute` wrapper for auth
- **Path alias**: `@/` maps to `src/`

## Key Configuration

### Spring AI (DashScope/OpenAI Compatible)

```yaml
spring.ai.openai:
  base-url: https://coding.dashscope.aliyuncs.com/v1
  api-key: ${ANTHROPIC_AUTH_TOKEN}  # Environment variable
```

### Database Profiles

- **dev**: H2 in-memory (auto-created), `ddl-auto: update`
- **prod**: PostgreSQL at `localhost:5432/admindb`, requires manual setup

### Maven Repositories

- Spring Milestones: `https://repo.spring.io/milestone` (required for Spring AI 0.8.1)

## Known Issues & Solutions

1. **Lombok 1.18.30 + JDK 24**: Incompatible → Use Lombok 1.18.36 or JDK 17
2. **Double scale attribute**: JPA doesn't support scale on Double → Remove precision/scale
3. **Azure OpenAI auto-config**: Causes errors → Use OpenAI-compatible mode instead
4. **pgvector store import**: Spring AI pgvector has issues → Custom simplified implementation

## API Response Format

All endpoints return `ApiResponse<T>`:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    ...
  }
}
```

## Test Credentials

- Frontend login: `admin` / `admin123`

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`