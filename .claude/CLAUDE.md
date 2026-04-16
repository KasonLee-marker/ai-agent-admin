# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Behavioral Guidelines (Karpathy Principles)

基于 Andrej Karpathy 对 LLM 编码陷阱的观察，以下是核心行为准则：

**注意：这些准则偏向谨慎而非速度。对于简单任务，请自行判断。**

### 1. Think Before Coding（先思考后编码）

**不要假设。不要隐藏困惑。展示权衡。**

在实现之前：

- 明确陈述假设。如果不确定，先询问。
- 如果存在多种解释，展示它们 - 不要默默选择。
- 如果存在更简单的方法，说出来。必要时提出反对意见。
- 如果有不清楚的地方，停下来。指出困惑之处。询问。

### 2. Simplicity First（简单优先）

**最小化代码解决问题。不做推测性工作。**

- 不添加超出需求的功能。
- 单次使用的代码不做抽象。
- 不添加未请求的"灵活性"或"可配置性"。
- 不为不可能发生的场景做错误处理。
- 如果写了200行代码但其实50行就够了，重写它。

问自己："资深工程师会说这过度复杂吗？"如果是，简化。

### 3. Surgical Changes（精确修改）

**只修改必须的。只清理自己造成的混乱。**

编辑现有代码时：

- 不要"改进"相邻的代码、注释或格式。
- 不要重构没有问题的代码。
- 匹配现有风格，即使你习惯不同写法。
- 如果注意到无关的死代码，提到它 - 不要删除它。

当你的修改产生无用代码时：

- 删除 YOUR 修改导致无用的 imports/variables/functions。
- 不要删除预存在的死代码，除非被要求。

测试标准：每个修改的行都应该能追溯到用户的请求。

### 4. Goal-Driven Execution（目标驱动执行）

**定义成功标准。循环验证。**

将任务转换为可验证的目标：

- "添加验证" → "为无效输入写测试，然后让它们通过"
- "修复bug" → "写一个能复现它的测试，然后让它通过"
- "重构X" → "确保重构前后测试都通过"

对于多步骤任务，陈述简要计划：

```
1. [步骤] → 验证: [检查]
2. [步骤] → 验证: [检查]
3. [步骤] → 验证: [检查]
```

强成功标准让你可以独立循环。弱标准（"让它工作"）需要持续澄清。

---

**这些准则有效标志：** diff 中更少不必要的修改，更少因过度复杂导致的重写，澄清问题在实现之前提出而非错误之后。

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
- [ ] **代码注释完整**（见下方规范）
- [ ] **新增功能必须有对应的单元测试，且测试通过**

### 测试用例规范

**每次新增功能或修改核心逻辑时，必须编写相应的单元测试。**

**要求：**

1. **测试覆盖范围**
   - 新增的 Service 方法必须有对应测试
   - 核心业务逻辑（如 embedding 计算、向量存储、健康检查）必须测试
   - 边界条件和异常情况必须测试

2. **测试命名规范**
   - 测试类命名：`{ClassName}Test`
   - 测试方法命名：`{methodName}_{scenario}` 或 `{methodName}_should{expectedBehavior}_when{condition}`
   - 示例：`embedBatchWithModel_shouldReturnVectors_whenValidInput`

3. **测试结构**
   - 使用 `@ExtendWith(MockitoExtension.class)` 进行 Mock
   - Mock 外部依赖（Repository、其他 Service）
   - 测试正常流程、边界条件、异常情况

4. **测试运行**
   - 编写完成后必须运行 `mvn test` 确保通过
   - 单个测试类：`mvn test -Dtest={ClassName}Test`

**示例：**

```java
@ExtendWith(MockitoExtension.class)
class VectorTableServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private VectorTableServiceImpl vectorTableService;

    @Test
    void ensureTableExists_shouldCreateTable_whenNotExists() {
        // Given
        int dimension = 1536;
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyString()))
            .thenReturn(false);

        // When
        String tableName = vectorTableService.ensureTableExists(dimension);

        // Then
        assertThat(tableName).isEqualTo("document_embeddings_1536");
        verify(jdbcTemplate, times(1)).execute(anyString());
    }
}
```

### 代码注释规范

**所有代码、方法必须写清楚注释。**

代码可读性和维护性是项目质量的基础。没有注释的代码增加理解成本，尤其在团队协作和后续维护时。

**要求：**

1. **类级别注释**
   - 每个 public 类必须有 Javadoc 注释
   - 说明类的职责、主要功能、使用场景
   - 使用 `@see` 引用相关类

2. **方法级别注释**
   - 每个公共方法必须有 Javadoc 注释
   - 说明用途、参数含义 (`@param`)、返回值 (`@return`)
   - 可能抛出的异常 (`@throws`)
   - 复杂方法需要说明执行流程（使用 `<ol>` 列表）

3. **字段注释**
   - 重要字段和常量需要注释说明用途
   - 使用 `/** ... */` 格式

4. **行内注释**
   - 复杂逻辑需要有行内注释解释意图
   - 避免无意义的注释（如重复代码本身）

**示例：**

```java
/**
 * 聊天服务实现类
 * <p>
 * 提供聊天会话管理和消息处理的核心功能：
 * <ul>
 *   <li>会话创建、查询、删除</li>
 *   <li>消息发送（同步和流式）</li>
 *   <li>AI 模型调用与响应处理</li>
 * </ul>
 * </p>
 *
 * @see ChatService
 * @see OpenAiChatClient
 */
@Service
public class ChatServiceImpl implements ChatService {

    /**
     * 发送消息并获取 AI 响应（同步模式）
     * <p>
     * 执行流程：
     * <ol>
     *   <li>获取会话和模型配置</li>
     *   <li>保存用户消息</li>
     *   <li>构建包含系统消息、历史消息和当前消息的 Prompt</li>
     *   <li>调用 AI 模型获取响应</li>
     *   <li>保存助手消息并更新会话统计</li>
     * </ol>
     * </p>
     *
     * @param request 消息请求，包含会话ID、消息内容、可选模型ID
     * @return 助手响应消息 DTO
     */
    public ChatResponse sendMessage(ChatRequest request) {
        // ...
    }
}
```

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

**启动前端项目时，确保端口 5173 可用。如端口被占用，先杀掉占用进程再启动。**

```bash
# 启动前端（端口 5173）
cd frontend
npm install          # Install dependencies
npm run dev          # Dev server on port 5173 (proxies /api to :8080)

# 如端口 5173 被占用，查找并杀掉占用进程：
netstat -ano | grep ":5173" | grep LISTENING  # 找到 PID（最后一列数字）
taskkill /F /PID <PID>                          # 杀掉该进程

npm run build        # Production build (tsc + vite)
npm run lint         # ESLint check
npm run preview      # Preview production build
```

**注意：前端必须运行在 5173 端口，因为 Vite 配置的 API 代理指向后端 8080 端口。**

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

### Docker Database Operations

**PostgreSQL 数据库运行在 Docker 容器中。执行 SQL 请使用 Docker 命令。**

```bash
# Find PostgreSQL container
docker ps | grep postgres

# Execute SQL (modify schema, etc.)
docker exec -it <container_name> psql -U postgres -d admindb -c "ALTER TABLE xxx ..."

# Interactive SQL session
docker exec -it <container_name> psql -U adminuser -d admindb

# Connection details from application.yml:
# - Host: localhost:5432
# - Database: admindb
# - User: adminuser / adminpass123
```

### Maven Repositories

- Spring Milestones: `https://repo.spring.io/milestone` (required for Spring AI 0.8.1)

## Known Issues & Solutions

1. **Lombok 1.18.30 + JDK 24**: Incompatible → Use Lombok 1.18.36 or JDK 17
2. **Double scale attribute**: JPA doesn't support scale on Double → Remove precision/scale
3. **Azure OpenAI auto-config**: Causes errors → Use OpenAI-compatible mode instead
4. **pgvector store import**: Spring AI pgvector has issues → Custom simplified implementation
5. **RAG evaluation retrieval returns empty**:
   - Cause 1: `retrieveDocuments()` didn't pass `embeddingModelId`, using wrong model/dimension table
   - Cause 2: Default threshold 0.7 was too high (70% similarity), filtering out valid results
   - Solution: Pass `embeddingModelId` from evaluation job, lower threshold to 0.3
   - Also added fallback: if job has no embeddingModelId, use knowledge base's `default_embedding_model_id`

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

## Browser Automation Testing

**使用 MCP Playwright 进行浏览器自动化测试，不要通过 npx 安装独立的 Playwright。**

MCP Playwright 工具已集成在环境中，提供以下能力：

- `browser_navigate` - 导航到指定 URL
- `browser_snapshot` - 获取页面可访问性快照（用于元素定位）
- `browser_click` - 点击元素
- `browser_type` - 输入文本
- `browser_fill_form` - 批量填写表单
- `browser_take_screenshot` - 截图
- `browser_console_messages` - 查看控制台日志
- `browser_network_requests` - 查看网络请求

**测试流程示例**：

1. 导航到页面: `browser_navigate("http://localhost:5175/login")`
2. 获取页面快照: `browser_snapshot()` 查找元素 ref
3. 填写表单: `browser_type(ref, "admin")`
4. 点击按钮: `browser_click(ref)`
5. 检查结果: `browser_snapshot()` 或 `browser_console_messages()`

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`