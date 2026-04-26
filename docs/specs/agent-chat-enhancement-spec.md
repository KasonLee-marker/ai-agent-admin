# Agent对话测试优化设计

## 背景

Agent详情页的"测试对话"功能与独立的"对话调试"功能存在重叠，需要整合优化：

- 对话调试支持多轮对话、流式输出、RAG配置
- Agent测试仅支持单次执行、事后显示工具调用

## 设计方案

采用**复用对话调试**方案，Agent测试关联对话调试Session。

### 整体架构

```
Agent详情页                      对话调试页
┌─────────────────┐            ┌─────────────────┐
│ 测试Tab         │            │ 新建Session     │
│ ┌─────────────┐ │            │ ┌─────────────┐ │
│ │快速测试     │ │──跳转──→   │ │关联Agent    │ │
│ │(单次执行)   │ │            │ │(可选)       │ │
│ └─────────────┘ │            │ └─────────────┘ │
│ ┌─────────────┐ │            │                 │
│ │创建完整对话 │ │            │ 多轮对话        │
│ │(跳转chat)   │ │            │ 流式+ReAct     │
│ └─────────────┘ │            │ 工具调用可视化  │
└─────────────────┘            └─────────────────┘
```

---

## Phase 1: Agent关联对话调试

### 1.1 Agent详情页改动

**测试Tab重命名为"快速测试"：**

- 保留现有单次执行功能（用于快速验证工具绑定）
- 工具调用记录改为实时显示（执行过程中逐步展示）
- 增加"创建完整对话"按钮

**新增"创建完整对话"功能：**

- 点击跳转到 `/chat` 页面，携带参数 `agentId=xxx`
- 自动创建Session并关联Agent

### 1.2 对话调试页增强

**新建Session弹窗增加"关联Agent"选项：**

| 配置项   | 不关联Agent | 关联Agent后       |
|-------|----------|----------------|
| 模型    | 手动选择     | 自动填充Agent模型    |
| 系统提示词 | 手动输入     | 使用Agent预设（可追加） |
| 工具    | 无        | 显示Agent绑定的工具   |
| RAG   | 可配置      | 可配置（与Agent独立）  |

**Session关联Agent后：**

- 自动加载Agent的modelId、systemPrompt
- 显示Agent绑定的工具列表（不可修改，由Agent管理）
- 对话时可调用Agent工具

### 1.3 后端改动

**ChatSession实体新增字段：**

```java
@Column(name = "agent_id")
private String agentId;  // 关联的Agent ID（可选）
```

**ChatRequest新增字段：**

```java
private String agentId;  // 创建Session时可指定Agent
```

**ChatServiceImpl改动：**

- `createSession()`：支持agentId参数，自动加载Agent配置
- `sendMessage()`：如果Session关联Agent，注入Agent工具定义到Prompt
- `executeTool()`：优先调用Agent绑定的工具

---

## Phase 2: 流式输出 + ReAct可视化

### 2.1 SSE事件扩展

**后端sendMessageStream()发送多种事件：**

| 事件类型          | 内容   | 说明          |
|---------------|------|-------------|
| `content`     | 文本片段 | AI回复内容      |
| `thought`     | 思考文本 | LLM推理过程（可选） |
| `tool_call`   | JSON | 工具调用开始      |
| `tool_result` | JSON | 工具执行结果      |
| `done`        | -    | 流结束         |

**事件格式：**

```
data: {"type":"thought","content":"用户想查询天气..."}
data: {"type":"tool_call","name":"map_weather","args":{"city":"北京"}}
data: {"type":"tool_result","name":"map_weather","result":{"temp":25}}
data: {"type":"content","content":"北京今天天气..."}
data: {"type":"done"}
```

### 2.2 前端ReAct渲染

**Chat/index.tsx消息渲染改动：**

- 解析多种事件类型
- 思考过程显示为蓝色背景卡片
- 工具调用显示为折叠卡片：
    - 展开显示：工具名、参数、结果、耗时
    - 状态指示：执行中（加载动画）、成功、失败
- 内容实时流式渲染

**渲染示例：**

```
用户发送: "查询北京天气"
─────────────────────────────
┌───────────────────────────┐
│ 🤔 思考                    │
│ 用户想查询天气，我需要调   │
│ 用map_weather工具          │
└───────────────────────────┘
┌───────────────────────────┐
│ 🔧 执行工具: map_weather   │
│ 参数: {"city": "北京"}     │
│ ↳ 结果: {"temp": 25}       │
│ 耗时: 234ms                │
└───────────────────────────┘
💬 北京今天天气晴朗，气温25度...
```

### 2.3 后端改动

**ChatServiceImpl.sendMessageStream()：**

- 工具执行前发送`tool_call`事件
- 工具执行后发送`tool_result`事件
- 支持并发工具调用（多个工具同时执行）

**AgentExecutionServiceImpl：**

- 提取工具执行逻辑为独立方法
- 支持流式回调通知

---

## 实现计划

### Phase 1（预估1-2天）

1. 后端：ChatSession新增agentId字段
2. 后端：ChatServiceImpl支持Agent关联
3. 前端：Agent详情页增加跳转按钮
4. 前端：对话调试页新建Session支持Agent选择
5. 测试：Agent关联后对话可调用工具

### Phase 2（预估2-3天）

1. 后端：SSE事件扩展（tool_call、tool_result）
2. 前端：事件解析与ReAct渲染
3. 前端：工具调用卡片组件
4. 测试：流式输出+工具调用可视化

---

## 注意事项

1. **Agent工具与RAG独立**：Agent绑定的是计算类工具，RAG是知识检索，两者互补
2. **ReAct思考过程**：需要LLM输出思考内容，可在systemPrompt中引导
3. **工具并发**：多工具调用时可并发执行，但事件需按顺序发送
4. **Session状态**：Agent关联后不可修改工具列表，需在Agent管理页调整