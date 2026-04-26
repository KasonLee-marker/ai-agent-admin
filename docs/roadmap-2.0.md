# AI Agent Admin 2.0 迭代规划

## 核心目标

将平台从「AI 工具管理平台」升级为「Agent 开发与协作平台」：

- Agent = LLM + Tools + KB + Prompt（组合配置）
- MCP 双向集成（Server + Client）
- 混合式多 Agent 协作（编排 + 自主协商）

---

## Phase 1: Agent 基础设施（预估 2 周）

### 目标

用户可创建、配置、运行单 Agent，支持工具绑定。

### 核心功能

#### 1.1 Agent 定义与管理

```
Agent 实体设计：
- id, name, description, version
- modelId (绑定模型)
- systemPrompt (系统提示词，可引用 Prompt 模板)
- tools: List<ToolBinding> (绑定的工具列表)
- config: AgentConfig (温度、max_tokens 等运行参数)
- status: DRAFT | PUBLISHED | ARCHIVED
```

**关于知识库（KB）的处理**：
知识库检索有两种设计方式：

| 方式            | 说明                                      | 适用场景               |
|---------------|-----------------------------------------|--------------------|
| **方式 A：工具化**  | `knowledge_retrieval` 作为独立工具，Agent 主动调用 | Agent 可灵活决定是否/何时检索 |
| **方式 B：固定绑定** | Agent 绑定 KB，对话时自动注入检索结果                 | 强依赖知识库的 Agent（如客服） |

**建议**：Phase 1 采用**方式 A（工具化）**，更灵活且复用现有 RAG 服务。
后续可扩展方式 B（Agent 实体增加 `defaultKnowledgeBaseId` 字段）。`

**Agent CRUD**：

- 创建：选择模型 → 编写系统提示词 → 绑定知识库（可选）→ 选择工具
- 编辑：修改配置、工具增删
- 版本：支持版本快照（可选）
- 测试：在线对话测试 Agent

#### 1.2 工具系统基础

```
Tool 实体设计：
- id, name, description, category
- type: BUILTIN | CUSTOM | MCP (来源类型)
- schema: JSON Schema (输入输出定义)
- executor: 执行器标识
- config: 工具配置（如 API endpoint、认证信息）
- permissions: 权限控制（哪些 Agent 可用）
```

**内置工具（首批）**：
| 工具 | 功能 | 执行模式 |
|------|------|----------|
| `shell_command` | 命令行执行 | **沙盒模式（Docker）** |
| `knowledge_retrieval` | 知识库检索 | 调用现有 RAG 服务 |
| `web_search` | 网络搜索 | HTTP API |
| `calculator` | 数学计算 | 本地 JVM 执行 |
| `datetime` | 获取时间 | 本地 JVM 执行 |
| `http_request` | HTTP 请求代理 | 平台转发 |

**命令行执行器（沙盒模式）**：

```
设计要点：
1. Docker 容器隔离执行
2. 白名单命令配置（只允许执行指定命令）
3. 超时控制（默认 30s，可配置）
4. 输出截断（防止超大输出）
5. 资源限制（CPU/内存/磁盘）
6. 执行日志审计

配置示例：
shell_command:
  sandbox:
    enabled: true
    container_image: "alpine:3.18"  # 轻量镜像
    timeout_seconds: 30
    max_output_bytes: 10000
    allowed_commands:
      - "ls"
      - "cat"
      - "grep"
      - "find"
      - "git"
      - "python"
      - "node"
    resource_limits:
      cpu_quota: 50000  # 50% CPU
      memory_mb: 256
```

**自定义工具**：

- 用户定义工具名称、描述、JSON Schema
- 配置 HTTP API endpoint + 认证
- 平台代理调用

#### 1.3 Agent 执行引擎

```
执行流程：
1. 用户发送消息
2. 构建 Prompt（系统提示词 + 知识库检索结果 + 工具定义）
3. 调用 LLM 获取响应
4. 检测 tool_calls
5. 执行工具，获取结果
6. 将工具结果加入对话，再次调用 LLM
7. 重复 4-6 直到无 tool_calls
8. 返回最终响应
```

**技术要点**：

- 使用 Spring AI 的 `Function Calling` 机制
- 工具执行支持同步/异步
- 流式输出支持（边执行边返回）
- 执行日志记录（用于调试）

### 数据库设计

```sql
-- Agent 表
CREATE TABLE agents (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    version VARCHAR(20) DEFAULT '1.0.0',
    model_id UUID REFERENCES model_configs(id),
    system_prompt TEXT,
    config JSONB,  -- {temperature, maxTokens, etc}
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Tool 表
CREATE TABLE tools (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(30),  -- SEARCH, CALCULATION, KNOWLEDGE, SHELL, HTTP
    type VARCHAR(20) NOT NULL,  -- BUILTIN, CUSTOM, MCP
    schema JSONB NOT NULL,  -- JSON Schema（输入参数定义）
    executor VARCHAR(100),  -- 执行器标识（如 shell_executor, http_executor）
    config JSONB,  -- 工具配置（沙盒设置、白名单命令等）
    created_at TIMESTAMP
);

-- Agent-Tool 绑定表
CREATE TABLE agent_tools (
    id UUID PRIMARY KEY,
    agent_id UUID REFERENCES agents(id) ON DELETE CASCADE,
    tool_id UUID REFERENCES tools(id),
    enabled BOOLEAN DEFAULT true,
    config JSONB,  -- Agent 级别的工具配置覆盖（如特定的 KB ID）
    created_at TIMESTAMP
);

-- Agent 执行日志表
CREATE TABLE agent_execution_logs (
    id UUID PRIMARY KEY,
    agent_id UUID REFERENCES agents(id),
    session_id UUID,
    input TEXT,
    output TEXT,
    tool_calls JSONB,  -- 记录调用的工具和参数
    duration_ms INTEGER,
    created_at TIMESTAMP
);

-- Shell 命令执行审计表（沙盒执行记录）
CREATE TABLE shell_execution_logs (
    id UUID PRIMARY KEY,
    agent_execution_log_id UUID REFERENCES agent_execution_logs(id),
    command TEXT NOT NULL,
    arguments JSONB,
    exit_code INTEGER,
    stdout TEXT,
    stderr TEXT,
    timeout_occurred BOOLEAN DEFAULT false,
    container_id VARCHAR(100),  -- Docker 容器 ID
    duration_ms INTEGER,
    created_at TIMESTAMP
);
```

### API 设计

```
POST   /api/v1/agents              创建 Agent
GET    /api/v1/agents              Agent 列表
GET    /api/v1/agents/{id}         Agent 详情
PUT    /api/v1/agents/{id}         更新 Agent
DELETE /api/v1/agents/{id}         删除 Agent
POST   /api/v1/agents/{id}/test    测试 Agent（对话）

GET    /api/v1/tools               工具列表（含内置+自定义）
POST   /api/v1/tools               创建自定义工具
GET    /api/v1/tools/{id}          工具详情
PUT    /api/v1/tools/{id}          更新工具
DELETE /api/v1/tools/{id}          删除工具

POST   /api/v1/agents/{id}/execute 执行 Agent（单次）
POST   /api/v1/agents/{id}/chat    Agent 对话（流式）
```

### 前端页面

- **Agent 管理**：列表、创建/编辑表单、测试对话面板
- **工具管理**：内置工具展示、自定义工具创建、工具详情
- **Agent 详情**：配置预览、绑定工具列表、对话测试

---

## Phase 2: MCP 生态集成（预估 2 周）

### 目标

平台支持 MCP 双向集成：作为 Server 暴露工具，作为 Client 连接外部 MCP Server。

### 2.1 MCP Server（平台作为工具提供方）

**实现方案**：

- 基于 Spring WebFlux 实现 MCP 协议（JSON-RPC 2.0 over SSE）
- 将平台内置工具和自定义工具暴露为 MCP Tools
- 支持外部 Agent/Client 通过 MCP 协议调用

```
MCP Server 端点：
POST /mcp/v1/tools/list        列出可用工具
POST /mcp/v1/tools/call        调用工具
GET  /mcp/v1/sse               SSE 连接（用于实时通知）
```

**配置**：

```yaml
mcp:
  server:
    enabled: true
    name: "ai-agent-admin"
    version: "2.0.0"
    tools:
      include_builtin: true
      include_custom: true
    auth:
      type: api_key  # 或 oauth
```

### 2.2 MCP Client（平台作为工具消费方）

**实现方案**：

- MCP Client SDK（连接外部 MCP Server）
- 劯态发现并注册远程工具
- 工具调用代理转发

```
MCP Server 连接配置：
- serverId, name, endpointUrl
- authType: NONE | API_KEY | OAUTH
- authConfig: 认证信息
- status: CONNECTED | DISCONNECTED | ERROR
- tools: 动态发现的工具列表（缓存）
```

**数据库设计**：

```sql
CREATE TABLE mcp_servers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    endpoint_url VARCHAR(500) NOT NULL,
    auth_type VARCHAR(20),
    auth_config JSONB,
    status VARCHAR(20) DEFAULT 'DISCONNECTED',
    discovered_tools JSONB,  -- 缓存的工具列表
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP
);
```

**同步机制**：

- 连接时调用 `tools/list` 发现工具
- 定时刷新工具列表（可配置间隔）
- 工具变更时更新本地缓存

### 2.3 工具协议统一

```
工具来源统一处理：
Tool.type:
- BUILTIN: 本地执行器
- CUSTOM: HTTP API 代理
- MCP: MCP Client 代理调用

执行器接口：
interface ToolExecutor {
    ToolResult execute(ToolCall call);
}
```

### 前端页面

- **MCP Server 管理**：连接外部 MCP Server、查看发现的工具、同步状态
- **工具来源标识**：区分内置/自定义/MCP 工具

---

## Phase 3: Multi-Agent 协作（预估 2-3 周）

### 目标

支持多 Agent 协作执行复杂任务，混合编排式 + 自主协商。

### 3.1 Agent 注册表

每个 Agent 启动时注册到 Registry，声明能力标签：

```
Agent Registry：
- agentId, name, capabilities: List<String>
- status: ONLINE | BUSY | OFFLINE
- load: 当前负载（任务数）
- endpoint: 调用地址（本地 Agent 或远程 Agent）
```

### 3.2 任务编排器（Orchestrator）

**编排式流程**：

```
用户任务 → Orchestrator 分析 → 拆解子任务 → 分配 Agent → 监控执行 → 结果汇总
```

```
Workflow 实体设计：
- id, name, description
- steps: List<WorkflowStep>
  - stepId, agentId, inputMapping, outputMapping
  - condition: 执行条件（可选）
- triggerType: MANUAL | SCHEDULED | EVENT
- status: DRAFT | ACTIVE | ARCHIVED
```

**示例 Workflow**：

```
任务：市场分析报告
Step 1: data_collector Agent（收集数据）
Step 2: analyzer Agent（分析数据）[依赖 Step 1]
Step 3: writer Agent（撰写报告）[依赖 Step 2]
Step 4: reviewer Agent（审核报告）[可选，条件触发]
```

### 3.3 Agent 间通信

**通信机制**：

- **消息总线**：Agent 间通过消息队列通信
- **直接调用**：Agent A 直接调用 Agent B
- **共享上下文**：工作流级别的共享数据

```sql
CREATE TABLE agent_messages (
    id UUID PRIMARY KEY,
    workflow_id UUID,
    from_agent_id UUID,
    to_agent_id UUID,
    message_type VARCHAR(20),  -- REQUEST, RESPONSE, NOTIFY
    content JSONB,
    created_at TIMESTAMP
);
```

### 3.4 自主协商模式

当编排流程不确定时，启用 Agent 自主协商：

```
协商流程：
1. Orchestrator 发送任务给 Leader Agent
2. Leader Agent 分析任务，决定是否需要协作
3. Leader Agent 向其他 Agent 发送协作请求
4. 其他 Agent 响应（接受/拒绝/提议）
5. Leader Agent 汇总响应，分配子任务
6. 执行并返回结果
```

**关键设计**：

- Leader Agent 选择策略（能力匹配 + 负载均衡）
- 协商超时机制
- 决策仲裁（冲突时 Orchestrator 介入）

### 3.5 协作执行引擎

```
MultiAgentExecutor：
- 解析 Workflow 或接收协商任务
- 分配任务给 Agent
- 监控执行进度
- 处理异常和重试
- 汇总结果
```

### 前端页面

- **Workflow 编辑器**：可视化编排 Agent 流程（拖拽式）
- **协作监控**：实时查看 Agent 协作状态、消息流
- **协作历史**：查看历史协作任务和结果

---

## Phase 4: Agent 发布平台（可选，预估 1 周）

### 目标

Agent 可发布到市场，供其他用户订阅使用。

### 功能

- **Agent 市场**：浏览、搜索、筛选 Agent
- **Agent 详情页**：功能介绍、使用示例、评分评价
- **订阅机制**：订阅 Agent 后可在自己的项目中使用
- **使用统计**：调用次数、成功率、平均耗时
- **版本管理**：Agent 更新后订阅者可选择升级

### 数据库设计

```sql
CREATE TABLE agent_publishments (
    id UUID PRIMARY KEY,
    agent_id UUID REFERENCES agents(id),
    publisher_id UUID,
    visibility VARCHAR(20),  -- PUBLIC, PRIVATE, ORGANIZATION
    tags JSONB,
    rating_aggregate DECIMAL(3,2),
    usage_count INTEGER DEFAULT 0,
    published_at TIMESTAMP
);

CREATE TABLE agent_subscriptions (
    id UUID PRIMARY KEY,
    agent_id UUID,
    subscriber_id UUID,
    subscribed_at TIMESTAMP
);
```

---

## 技术栈新增

| 功能          | 技术选择                          |
|-------------|-------------------------------|
| MCP 协议      | Spring WebFlux + JSON-RPC 2.0 |
| 工具执行        | Spring AI Function Calling    |
| **沙盒执行**    | Docker Java SDK + 临时容器        |
| Agent 通信    | 内存消息总线（可扩展为 Redis/Kafka）      |
| Workflow 编排 | 自定义引擎（可选 Camunda/Zeebe）       |
| 流式输出        | SSE + Flux                    |

**沙盒执行架构**：

```
┌───────────────────────────────────────────────────────┐
│                  Shell Command Executor                │
├───────────────────────────────────────────────────────┤
│  1. 校验命令是否在白名单                                │
│  2. 创建临时 Docker 容器（alpine + 必要工具）            │
│  3. 设置资源限制（CPU/内存/超时）                        │
│  4. 执行命令，捕获 stdout/stderr                        │
│  5. 容器自动销毁（执行完成后）                           │
│  6. 记录审计日志                                        │
└───────────────────────────────────────────────────────┘
```

---

## 2026-04-25 进度

### MVP-1 & MVP-2 完成 ✅

**已实现**：

- Agent CRUD API（创建、查询、更新、删除、状态管理）
- Tool CRUD API（内置工具查询、自定义工具管理）
- Agent-Tool 绑定 API（绑定、解绑、配置更新）
- 3 个内置工具执行器（calculator、datetime、knowledgeRetrieval）
- 启动自动注册内置工具（BuiltInToolInitializer）

**数据库表**：

- agents（Agent 配置表）
- tools（工具定义表）
- agent_tools（绑定关系表）

**验证通过**：283 tests, 0 failures，API curl 测试成功

---

### Phase 2 MCP 集成完成 ✅

**已实现**：

- MCP Server 配置管理（创建、查询、更新、删除）
- JSON 配置解析（自动识别 SSE/Stdio transport）
- MCP Client 实现（SseMcpClient + StdioMcpClient）
- 工具发现与注册（调用 tools/list，注册到 Tool 表）
- McpToolExecutor（统一 MCP 工具调用入口）
- 工具列表查看优化（点击标签直接查看已保存工具）
- 前端 MCP Server 管理页面
- Agent 工具绑定（按 Server 分组显示 MCP 工具包）

**数据库表**：

- mcp_servers（MCP Server 配置表）

**验证通过**：

- 成功连接百度 MCP Playground（SSE）
- 发现并注册 10 个地图工具
- MCP 工具执行测试成功（map_geocode）
- 前端工具预览功能正常

**API 端点**：

- `POST /api/v1/mcp-servers/from-json` - JSON 配置创建
- `PUT /api/v1/mcp-servers/{id}` - 更新基本信息
- `PUT /api/v1/mcp-servers/{id}/from-json` - JSON 配置更新
- `POST /api/v1/mcp-servers/{id}/refresh-tools` - 刷新工具列表
- `GET /api/v1/mcp-servers/{id}/tools` - 获取已保存工具列表

---

## 依赖关系

```
Phase 1 (Agent + Tools) → Phase 2 (MCP) → Phase 3 (Multi-Agent) → Phase 4 (Marketplace)
     ↓                       ↓                 ↓
  基础执行              工具生态扩展         协作能力          生态闭环
```

---

## 风险与考量

1. **Spring AI Function Calling 兼容性**：需验证 ModelProvider 是否支持（DashScope/OpenAI 兼容模式应支持）
2. **MCP 协议标准**：参考 Anthropic MCP 规范，确保兼容 Claude 等
3. **沙盒执行安全**：
    - Docker 容器必须无特权模式
    - 白名单命令必须严格校验（防止 `rm -rf /` 等）
    - 网络隔离（容器无外网访问，除非显式配置）
    - 定期清理残留容器和镜像
4. **多 Agent 性能**：协作时 LLM 调用次数多，需考虑成本控制
5. **前端复杂度**：Workflow 编辑器开发量大，可先简化为文本配置
6. **Docker 依赖**：沙盒执行需要 Docker 环境，生产部署需考虑

---

## 下一步建议

请确认：

1. Phase 1 是否需要调整优先级？
2. 是否需要先做技术验证（如 Spring AI Function Calling demo）？
3. 是否需要我创建 Phase 1 的详细任务文件？