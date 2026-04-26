# AI Agent Admin - 变更日志

## v2.0.0 (2026-04-25) Agent MVP 版本

### 功能概述

实现 Agent 核心能力：创建、配置、运行单 Agent，支持工具绑定和执行。为后续 MCP 集成和多 Agent 协作奠定基础。

### MVP-1: Agent 实体与基础 CRUD

**新增实体**：

- `Agent` - Agent 配置实体（名称、描述、模型、系统提示词、配置、状态）
- `AgentTool` - Agent-Tool 绑定关系（工具ID、启用状态、配置）
- `AgentStatus` 枚举 - DRAFT、PUBLISHED、ARCHIVED

**数据库表**：

```sql
CREATE TABLE agents (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    version VARCHAR(20) DEFAULT '1.0.0',
    model_id VARCHAR(64) NOT NULL,
    system_prompt TEXT,
    config JSONB DEFAULT '{"temperature": 0.7, "maxTokens": 4096}',
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE agent_tools (
    id VARCHAR(64) PRIMARY KEY,
    agent_id VARCHAR(64) NOT NULL REFERENCES agents(id),
    tool_id VARCHAR(64) NOT NULL REFERENCES tools(id),
    enabled BOOLEAN DEFAULT true,
    config JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agent_id, tool_id)
);
```

**API 端点**：

- `POST /api/v1/agents` - 创建 Agent
- `GET /api/v1/agents` - Agent 列表（支持状态、模型、关键词筛选）
- `GET /api/v1/agents/{id}` - Agent 详情
- `PUT /api/v1/agents/{id}` - 更新 Agent
- `DELETE /api/v1/agents/{id}` - 删除 Agent
- `PUT /api/v1/agents/{id}/status` - 状态管理（DRAFT → PUBLISHED → ARCHIVED）
- `POST /api/v1/agents/{id}/tools` - 绑定工具
- `DELETE /api/v1/agents/{id}/tools/{toolId}` - 解绑工具
- `PUT /api/v1/agents/{id}/tools/{toolId}` - 更新工具配置

---

### MVP-2: Tool 实体与内置工具

**新增实体**：

- `Tool` - 工具定义实体（名称、描述、类型、类别、Schema）
- `ToolType` 枚举 - BUILTIN、CUSTOM、MCP
- `ToolCategory` 枚举 - GENERAL、SEARCH、CALCULATION、KNOWLEDGE、SHELL、HTTP

**内置工具**：

| 工具                   | 描述      | 执行器                        |
|----------------------|---------|----------------------------|
| `calculator`         | 数学表达式计算 | CalculatorExecutor         |
| `datetime`           | 获取当前时间  | DatetimeExecutor           |
| `knowledgeRetrieval` | 知识库检索   | KnowledgeRetrievalExecutor |

**启动自动注册**：

- `BuiltInToolInitializer` - 实现 ApplicationRunner，启动时自动注册内置工具

**工具执行架构**：

- `ToolExecutor` 接口 - 工具执行器接口
- `ExecutionContext` - 执行上下文（agentId、agentToolConfig、userContext）
- `ToolResult` - 执行结果包装器

---

### MVP-3: Agent 执行引擎

**新增实体**：

- `AgentExecutionLog` - 执行日志（输入、输出、工具调用记录、耗时）

**执行流程**：

1. 构建 Prompt（系统提示词 + 工具定义）
2. 调用 LLM
3. 检测 JSON 格式 tool_calls
4. 执行工具，获取结果
5. 将工具结果加入对话，再次调用 LLM
6. 重复直到无 tool_calls（最大 5 次）
7. 返回响应并保存执行日志

**API 端点**：

- `POST /api/v1/agents/{id}/execute` - 执行 Agent（同步）
- `GET /api/v1/agents/{id}/logs` - 执行日志列表
- `GET /api/v1/agents/{id}/logs/{logId}` - 日志详情

**工具调用格式**（LLM 响应）：

```json
```json
{"tool": "calculator", "args": {"expression": "2+3"}}
```

```

---

### MVP-4: 前端 Agent 管理

**新增页面**：
- `/agents` - Agent 列表（表格、搜索、创建按钮）
- `/agents/:id` - Agent 详情（四个 Tab）

**详情页 Tab**：
- 基本信息 - 名称、描述、模型、状态、系统提示词
- 工具绑定 - 工具列表、添加/解绑工具
- 测试对话 - 输入框、AI响应、工具调用可视化
- 执行日志 - 日志列表（输入摘要、输出摘要、工具调用数）

**前端文件**：
- `frontend/src/types/agent.ts` - Agent 类型定义
- `frontend/src/api/agent.ts` - Agent API 调用
- `frontend/src/pages/Agents/index.tsx` - Agent 列表页
- `frontend/src/pages/Agents/AgentDetail.tsx` - Agent 详情页

---

### 验证结果

```

后端测试: Tests run: 283, Failures: 0, Errors: 0
前端构建: ✓ built in 11.08s

Agent 执行测试:

- 输入: "计算 5+7*2"
- 输出: "计算结果是19，按照数学运算优先级先计算乘法7×2=14，再计算加法5+14=19"
- 工具调用: calculator(expression="5+7*2", result=19)
- 执行耗时: ~3000ms

```

---

## v2.0.1 (2026-04-25) MCP Server 集成（Phase 2）

### 功能概述

实现 MCP (Model Context Protocol) 服务器集成，让 Agent 可以调用外部 MCP 工具。支持 stdio 和 SSE 双 Transport，成功连接百度 MCP Playground。

### Phase 2-1: MCP Client 核心

**新增实体**：
- `McpServer` - MCP Server 配置（名称、描述、transportType、command/url、args、env）

**数据库表**：
```sql
CREATE TABLE mcp_servers (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    transport_type VARCHAR(20) DEFAULT 'stdio',
    command VARCHAR(200),
    url VARCHAR(500),
    args JSONB DEFAULT '[]',
    env JSONB DEFAULT '{}',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**MCP Client 接口**：

```java
public interface McpClient {
    void connect(McpServerConfig config);
    boolean isConnected();
    List<McpTool> listTools();
    McpToolResult callTool(String toolName, Map<String, Object> args);
    void disconnect();
    String getServerName();
}
```

**Transport 实现**：

- `StdioMcpClient` - 通过 stdin/stdout 与 MCP Server 进程通信
- `SseMcpClient` - 通过 SSE 长连接 + POST message endpoint 通信

**API 端点**：

- `POST /api/v1/mcp-servers` - 创建 MCP Server
- `GET /api/v1/mcp-servers` - MCP Server 列表
- `GET /api/v1/mcp-servers/{id}` - 详情
- `DELETE /api/v1/mcp-servers/{id}` - 删除
- `POST /api/v1/mcp-servers/{id}/refresh-tools` - 刷新工具列表

---

### Phase 2-2: MCP 工具注册

**工具注册流程**：

1. 连接 MCP Server
2. 调用 `tools/list` 获取工具列表
3. 解析 inputSchema
4. 注册到 Tool 表（type = MCP, executor = mcpToolExecutor）

**McpToolExecutor**：

- 从 Tool.config 获取 MCP Server 配置
- 建立/复用 MCP Client 连接
- 调用 `tools/call` 执行工具
- 返回结果包装器

---

### Phase 2-3: 前端 MCP Server 管理

**新增页面**：

- `/mcp-servers` - MCP Server 配置列表
- 创建弹窗（Transport 类型选择、SSE URL/Stdio 命令输入）

**前端文件**：

- `frontend/src/types/mcp.ts` - MCP 类型定义
- `frontend/src/api/mcp.ts` - MCP API 调用
- `frontend/src/pages/McpServers/index.tsx` - MCP Server 管理页

---

### SSE 连接测试

**百度 MCP Playground**：

- URL: `https://mcp-playground.baidu.com/.../sse`
- 发现 10 个地图工具：
    - `map_directions` - 路线规划
    - `map_distance_matrix` - 距离矩阵
    - `map_geocode` - 地址转坐标
    - `map_ip_location` - IP 定位
    - `map_place_details` - POI 详情
    - `map_poi_extract` - POI 标注
    - `map_reverse_geocode` - 坐标转地址
    - `map_road_traffic` - 道路拥堵
    - `map_search_places` - 地点搜索
    - `map_weather` - 天气查询

---

### Phase 2-4: 工具列表查看优化

**新增功能**：

- 工具刷新后自动保存到 Tool 表
- 点击工具数量标签直接查看已保存的工具列表
- 无需每次连接 MCP Server 获取工具
- 刷新按钮仍可用于更新工具列表

**新增 API 端点**：

- `GET /api/v1/mcp-servers/{id}/tools` - 获取已保存的工具列表

**数据库查询**：

```sql
-- PostgreSQL JSON 查询：从 config 字段提取 mcpServerId
SELECT * FROM tools 
WHERE type = 'MCP' 
AND config::json->>'mcpServerId' = :mcpServerId
```

**前端改进** (`McpServers/index.tsx`)：

- 工具数量标签改为可点击（cursor=pointer）
- 点击触发 `handleViewTools()` 查询已保存工具
- Tooltip 提示区分：
    - 有工具："点击查看工具列表"
    - 无工具："点击刷新后可查看工具列表"

**后端改动**：

- `ToolRepository.java` - 新增 `findByMcpServerId()` 原生 SQL 查询
- `McpServerController.java` - 新增 `/{id}/tools` GET 端点
- `ToolMapper.java` - 新增 `toResponseList()` 方法

**验证结果**：

```
后端构建: mvn clean install -DskipTests 成功
前端构建: ✓ built in 5.11s
API 测试: GET /api/v1/mcp-servers/{id}/tools 返回 10 个工具
前端测试: 点击工具标签直接显示工具列表 Modal
```

---

### 关键修复

1. **SseMcpClient URL 字段**：使用 `config.getUrl()` 替代 `config.getCommand()`
2. **messageEndpoint 相对路径**：转换为完整 URL（从 SSE URL 提取 baseUrl）
3. **EntityScan 包路径**：修复为 `com.aiagent.admin.domain.entity`
4. **McpToolExecutor SSE 支持**：注入 StdioMcpClient + SseMcpClient，根据 transportType 选择
5. **MCP 配置继承**：AgentExecutionServiceImpl 合并 Tool.config 到 agentToolConfig

---

### 验证结果

```
后端构建: mvn clean install -DskipTests 成功
前端构建: ✓ built in 9.98s
SSE 连接: 成功连接百度 MCP Playground
工具发现: 10 个地图工具已注册

MCP 工具执行测试:
- 输入: "查询北京市海淀区的经纬度坐标"
- 工具调用: map_geocode(address="北京市海淀区")
- MCP 响应: lng=116.3054340544974, lat=39.96548984110075
- 执行耗时: 2738ms (MCP) + 6603ms (总)
```

---

## v2.0.2 (2026-04-26) Bug Fixes

### Agent 工具调用修复

**问题**：Agent 执行时无法正确调用工具，LLM 返回 tool_calls 但未被解析执行。

**根本原因**：Spring AI Function 构造器参数顺序错误。`Function` record 期望 `(description, name, parameters)`，但代码传入了
`(name, description, parameters)`。

**修复位置**：`AgentExecutionServiceImpl.java:229-233`

```java
// 修复前（错误）
Function function = new Function(
    tool.getName(),          // name 作为第一个参数（错误）
    tool.getDescription(),   // description 作为第二个参数（错误）
    parameters
);

// 修复后（正确）
Function function = new Function(
    tool.getDescription(),   // description 作为第一个参数（正确）
    tool.getName(),          // name 作为第二个参数（正确）
    parameters
);
```

**验证结果**：

```
curl 测试: POST /api/v1/agents/{id}/execute
输入: "现在几点了？"
工具调用: datetime(result="2026-04-25 16:48:18 UTC")
输出: "现在是2026年4月25日16:48:18 UTC时间"
```

---

### 对话调试页面按钮布局修复

**问题**：对话调试页面上方创建对话按钮显示不全，两个按钮并排只显示部分内容。

**根本原因**：`Space` 组件使用水平布局（`direction="horizontal"`），按钮未设置 `block` 属性。

**修复位置**：`frontend/src/pages/Chat/index.tsx`

```tsx
// 修复前（水平布局，按钮显示不全）
<Space>
    <Button icon={<MessageOutlined />}>普通对话</Button>
    <Button type="primary" icon={<RobotOutlined />}>Agent对话</Button>
</Space>

// 修复后（垂直布局，block 按钮）
<Space direction="vertical" style={{width: '100%'}} size="small">
    <Button block icon={<MessageOutlined />}>普通对话</Button>
    <Button type="primary" block icon={<RobotOutlined />}>Agent对话</Button>
</Space>
```

---

## v1.0.1 (2026-04-19) HanLP 中文句子分割集成

### 功能概述

集成 HanLP 中文 NLP 库，修复 LangChain4j 文档分块器对中文文本的 overlap 不生效问题。

### 问题背景

LangChain4j 的 overlapFrom 方法依赖英文标点符号（`.`、`!`、`?`）检测句子边界，对中文标点（`。`、`！`、`？`）无法识别，导致：
- 中文文档分块后相邻块之间无重叠
- 语义分块依赖 Embedding API 计算句子相似度，但句子分割不准确

### 解决方案

**引入 HanLP 中文句子分割**

- 依赖: `com.hankcs:hanlp:portable-1.8.6`
- API: `SentencesUtil.toSentenceList(text)` - 准确识别中文句子边界
- 所有分块策略内部均使用 HanLP 进行句子分割

**依赖升级**

| 依赖           | 原版本   | 新版本   |
|--------------|-------|-------|
| LangChain4j  | 0.36.2 | 1.13.0 |
| HanLP (新增)   | -     | portable-1.8.6 |

### 分块策略改动

| 策略          | 改动                                       |
|-------------|------------------------------------------|
| FIXED_SIZE  | 使用 HanLP 分句，再按字符数组合，实现正确 overlap        |
| PARAGRAPH   | 使用 HanLP 分句，按段落边界分块，段落过长时递归分割           |
| SENTENCE    | 使用 HanLP 分句，按句子边界分块                     |
| RECURSIVE   | 使用 HanLP 分句，段落→句子递归分割                   |
| SEMANTIC    | 使用 HanLP 分句 → Embedding 相似度计算 → 语义边界分割 |

### 前端改动

- 移除语义分块的 Embedding 模型选择框（自动使用知识库默认模型）
- 文档上传页面：移除 SEMANTIC 策略的 embedding 选择
- 知识库管理页面：移除 SEMANTIC 策略的 embedding 提示

### 代码改动

| 文件                         | 改动                                          |
|----------------------------|---------------------------------------------|
| `pom.xml`                  | 版本升级 1.0.0 → 1.0.1                         |
| `admin-server-runtime/pom.xml` | LangChain4j 0.36.2 → 1.13.0, 新增 HanLP     |
| `DocumentAsyncService.java` | 所有策略使用 HanLP 分句，新增 `splitBySentenceWithOverlap()` 等方法 |
| `DocumentServiceImpl.java` | 语义分块自动使用知识库默认 Embedding 模型                 |
| `Documents/index.tsx`      | 移除 SEMANTIC 策略的 embedding 选择框              |
| `KnowledgeBases/index.tsx` | 移除 SEMANTIC 策略的 embedding 提示              |

---

## 2026-04-19 pg_jieba 中文分词与 BM25 搜索优化

### 功能概述

为 PostgreSQL 数据库安装 pg_jieba 中文分词扩展，优化 BM25 中文全文搜索效果。

### Docker 镜像重构

**新镜像**: `postgres-pgvector-jieba:pg15`

基于 `pgvector/pgvector:pg15`，集成：

- pgvector - 向量存储扩展
- pg_jieba - 中文分词扩展（结巴分词）
- pg_trgm - 三元组模糊匹配扩展

**构建文件** (`docker/postgres-zhparser/`):

```
Dockerfile          - 镜像构建脚本
init.sql            - 初始化脚本（创建扩展、配置）
pg_jieba.zip        - pg_jieba 源码（Gitee镜像）
cppjieba.zip        - cppjieba 分词库（Gitee镜像）
limonp.zip          - limonp 工具库（GitHub）
```

**关键配置**:

```sql
-- init.sql
ALTER SYSTEM SET shared_preload_libraries = 'pg_jieba';
CREATE EXTENSION IF NOT EXISTS pg_jieba;
```

**构建命令**:

```bash
cd docker/postgres-zhparser
docker build -t postgres-pgvector-jieba:pg15 .
docker run -d --name agentx-postgres -p 5432:5432 postgres-pgvector-jieba:pg15
```

### BM25 中文搜索改进

**问题**：原实现使用 pg_trgm similarity 匹配中文，效果差且不相关。

**解决方案**：使用 pg_jieba 分词 + ts_rank 评分：

### BM25 阈值过滤修复

**问题**：BM25 搜索完全忽略用户设置的阈值，返回所有结果。

**原因**：

- `BM25SearchService.searchBM25()` 方法没有 threshold 参数
- SQL 查询中没有 score 过滤条件
- DocumentServiceImpl 调用 BM25 时未传递 threshold

**解决方案**：

1. 更新接口签名：

```java
List<VectorSearchResult> searchBM25(String query, String knowledgeBaseId, 
    String documentId, int topK, Double threshold);
```

2. SQL 添加阈值过滤：

```sql
WHERE content_tsv_jieba @@ to_tsquery('jiebamp', :tsQuery)
  AND ts_rank(content_tsv_jieba, to_tsquery('jiebamp', :tsQuery)) > :threshold
```

3. DocumentServiceImpl 传递 threshold 参数。

**分数范围差异（重要）**：

| 检索方式          | 分数范围     | 建议阈值       |
|---------------|----------|------------|
| 向量检索（余弦相似度）   | 0-1      | 0.3-0.7    |
| BM25（ts_rank） | 0.01-0.5 | 0.01-0.1   |
| 混合检索（RRF）     | 0-0.05   | 0.005-0.02 |

### 前端 RAG 配置优化

**改动**：

1. **检索策略位置调整**：移到知识库选择之后，必填验证
2. **阈值范围动态调整**：根据检索策略显示不同的阈值范围
    - 向量检索：0-1，默认 0.3
    - BM25：0-0.5，默认 0.05
    - 混合检索：0-0.05，默认 0.01
3. **分数标签优化**：来源显示根据策略显示不同标签
    - 向量检索显示"相似度"
    - BM25 显示"BM25分数"
    - 混合检索显示"RRF分数"

**RRF 分数说明**：

混合检索使用 RRF (Reciprocal Rank Fusion) 算法融合两种检索结果：

```
RRF_score = Σ 1/(k + rank_i)，k=60
```

- 双检索排名第一的结果分数 ≈ 0.033
- 排名靠后的结果分数 ≈ 0.008
  | BM25（ts_rank） | 0.01-0.5 | 0.01-0.1 |

用户设置的 0.5 阈值对 BM25 过高（BM25 最高分才约 0.5），会导致几乎无结果返回。

```java
// BM25SearchServiceImpl.java
// 中文查询：使用 jiebamp parser（最大概率模式）
String sql = """
    SELECT dc.id, dc.content,
           ts_rank(dc.content_tsv_jieba, to_tsquery('jiebamp', :tsQuery)) as score
    FROM document_chunks dc
    WHERE dc.content_tsv_jieba @@ to_tsquery('jiebamp', :tsQuery)
    ORDER BY score DESC
    """;
```

**jiebamp vs jieba**:

- `jieba` - 混合模式，依赖字典文件，需正确配置路径
- `jiebamp` - 最大概率模式，使用 HMM 模型，不依赖字典

**数据结构变更**:

```sql
-- 新增中文 tsvector 列
ALTER TABLE document_chunks ADD COLUMN content_tsv_jieba tsvector;

-- 创建 GIN 索引
CREATE INDEX idx_document_chunks_tsv_jieba ON document_chunks USING GIN(content_tsv_jieba);

-- 创建触发器（自动更新）
CREATE TRIGGER trg_update_content_tsv_jieba
BEFORE INSERT OR UPDATE ON document_chunks
FOR EACH ROW EXECUTE FUNCTION update_content_tsv_jieba();
```

### 其他代码改动

**知识库重索引默认模型** (`KnowledgeBases/index.tsx`):

```typescript
// 打开重索引对话框时，默认选中当前知识库的 Embedding 模型
setReindexModelId(selectedKb.defaultEmbeddingModelId || null)
```

**对话 Embedding 模型继承** (`ChatServiceImpl.java`):

```java
// 创建对话时，RAG Embedding 模型自动继承知识库默认模型
if (Boolean.TRUE.equals(request.getEnableRag()) && request.getKnowledgeBaseId() != null) {
    KnowledgeBase kb = knowledgeBaseRepository.findById(request.getKnowledgeBaseId())
        .orElseThrow(...);
    ragEmbeddingModelId = kb.getDefaultEmbeddingModelId();
}
```

**前端改动** (`Chat/index.tsx`):

- Embedding 模型选择改为 disabled（不可修改，继承知识库）
- 添加提示信息说明继承规则

### 测试验证

**数据库测试**:

```sql
-- 中文分词测试
SELECT to_tsvector('jiebamp', '包邮吗');
-- 结果: '包':1 '邮':2

-- 搜索测试
SELECT ts_rank(content_tsv_jieba, to_tsquery('jiebamp', '配送')) 
FROM document_chunks WHERE content_tsv_jieba @@ to_tsquery('jiebamp', '配送');
-- 返回相关结果
```

**后端测试**: 233 个测试全部通过

---

## 2026-04-19 Chat与RAG功能合并 (v1.0.0)

### 功能概述

将独立的"对话调试"和"RAG对话"功能合并为统一的对话调试页面，RAG作为可选增强功能。

### 新增功能

**对话调试页面 RAG 配置**

- 新建对话时可配置 RAG 检索增强（可选开关）
- 编辑对话时可修改 RAG 配置
- RAG 配置项：
    - 知识库选择
    - Embedding 模型选择（自动关联知识库默认模型）
    - 检索数量(topK)配置（1-20）
    - 相似度阈值配置（0-1）
    - 检索策略选择（向量检索/BM25/混合检索）
- 消息显示 RAG 检索来源（折叠面板）

### 数据模型变更

**ChatSession 实体新增字段**：

```java
@Column(name = "enable_rag")
private Boolean enableRag;              // 是否启用RAG

@Column(name = "knowledge_base_id")
private String knowledgeBaseId;         // 关联知识库

@Column(name = "rag_top_k")
private Integer ragTopK;                // 检索数量

@Column(name = "rag_threshold")
private Double ragThreshold;            // 相似度阈值

@Column(name = "rag_strategy")
private String ragStrategy;             // 检索策略

@Column(name = "rag_embedding_model_id")
private String ragEmbeddingModelId;     // Embedding模型
```

**ChatMessage 实体新增字段**：

```java
@Column(name = "sources", columnDefinition = "TEXT")
private String sources;                 // RAG检索来源JSON
```

### 代码改动

**后端改动**：

| 文件                     | 改动                                                                      |
|------------------------|-------------------------------------------------------------------------|
| `ChatSession.java`     | 新增6个RAG配置字段                                                             |
| `ChatMessage.java`     | 新增sources字段                                                             |
| `ChatRequest.java`     | CreateSessionRequest/UpdateSessionRequest新增RAG字段                        |
| `ChatSessionDTO.java`  | 新增RAG配置字段                                                               |
| `ChatResponse.java`    | 新增sources字段                                                             |
| `ChatServiceImpl.java` | createSession/updateSession支持RAG字段；sendMessage/sendMessageStream集成RAG检索 |
| `RagService.java`      | 新增retrieve()方法（仅检索，不生成回答）                                               |
| `RagServiceImpl.java`  | 实现retrieve()方法                                                          |

**前端改动**：

| 文件                     | 改动                                                                         |
|------------------------|----------------------------------------------------------------------------|
| `types/chat.ts`        | ChatSession/ChatMessage/CreateSessionRequest新增RAG字段；新增VectorSearchResult类型 |
| `pages/Chat/index.tsx` | 新建/编辑对话弹窗增加RAG配置UI；消息渲染增加来源显示                                              |
| `App.tsx`              | 删除/RAG路由                                                                   |
| `Layout/index.tsx`     | 删除"RAG对话"菜单项                                                               |

**删除内容**：

- 前端 `pages/Rag/` 目录（独立RAG对话页面）
- 前端 `api/rag.ts`、`api/ragSession.ts`
- 前端 `types/rag.ts`

### 单元测试

新增 RAG 相关单元测试，覆盖核心方法：

| 测试类                   | 测试数 | 覆盖内容                                                                                    |
|-----------------------|-----|-----------------------------------------------------------------------------------------|
| `ChatServiceImplTest` | 41  | createSession RAG字段、sendMessage RAG启用/禁用、retrieveDocuments、buildSystemMessage、sources解析 |
| `RagServiceImplTest`  | 13  | retrieve()方法各种参数组合                                                                      |

**覆盖率**（RAG核心方法）：

- `retrieveDocuments()`: 100%
- `buildSystemMessage()`: 87%
- `createSession()`: 87%
- `getSessionAndModelContext()`: 100%
- `RagServiceImpl.retrieve()`: 100%

### 发布

- Release: v1.0.0
- 发布包: `ai-agent-admin-v1.0.0.zip`（含后端jar + 前端 + README）
- GitHub: https://github.com/KasonLee-marker/ai-agent-admin/releases/tag/v1.0.0

---

## 2026-04-18 移除检索得分功能

### 功能调整

**移除原因**：检索得分（Recall@K）需要用户在数据集项中预先标注期望文档ID，但评估任务创建时可动态选择知识库，两者无法匹配，导致该功能实际无法使用。

**删除内容**：

| 模块   | 删除项                           |
|------|-------------------------------|
| 数据集项 | `expectedDocIds`、`context` 字段 |
| 评估结果 | `retrievalScore` 字段           |
| 评估指标 | `averageRetrievalScore` 统计    |
| 前端   | 数据集表单中的"期望文档ID"和"参考上下文"输入框    |
| 前端   | 评估详情表格中的"检索得分"列               |

**数据库变更**：

```sql
ALTER TABLE dataset_items DROP COLUMN IF EXISTS expected_doc_ids;
ALTER TABLE dataset_items DROP COLUMN IF EXISTS context_data;
ALTER TABLE evaluation_results DROP COLUMN IF EXISTS retrieval_score;
```

**保留的评估指标**：

- AI 得分（0-100）- LLM-as-Judge 质量评分
- 语义相似度（0-1）- Embedding 余弦相似度
- 忠实度（0-1）- 答案是否忠实于检索内容

---

## 2026-04-15 语义切分异步处理修复与优化

### Bug修复

**临时文件清理问题**

异步处理时 Tomcat 清理临时上传文件导致 `NoSuchFileException`：

```
java.nio.file.NoSuchFileException: C:\Users\Administrator\AppData\Local\Temp\tomcat...
```

**问题原因**：

- HTTP 请求结束后 Tomcat 自动清理临时上传文件
- 异步线程在事务提交后执行（请求已结束），临时文件已不存在

**解决方案**：事件驱动架构 + byte[] 存储

```
上传请求 → 读取 file.getBytes() → 发布事件（存储 byte[]）→ 事务提交
         → 异步线程收到事件 → 创建 MockMultipartFile → 处理文档
```

**代码改动**：

1. `DocumentUploadEvent.java` - 存储 `byte[]` 而不是 `MultipartFile`
2. `DocumentServiceImpl.java` - 在发布事件前读取 `file.getBytes()`
3. `DocumentProcessingEventListener.java` - 使用 `MockMultipartFile` 包装 byte 数组
4. `pom.xml` - 添加 `spring-test` 依赖（MockMultipartFile 所在包）

### 功能优化

**进度显示优化**

- 当 `semanticProgressTotal` 为 0 或未初始化时，显示"准备中..."而不是"0/0 句子"
- 避免显示无意义的进度数字

**轮询刷新优化**

- 状态变成 `CHUNKED` 或 `FAILED` 时，重新获取完整文档列表
- 解决只刷新状态、不刷新分块数量的问题

**前端改动** (`Documents/index.tsx`)：

```typescript
// 轮询逻辑增加判断
if (['CHUNKED', 'FAILED'].includes(res.data.status)) {
    needRefreshAll = true  // 重新获取完整列表
}

// 进度显示优化
{record.semanticProgressTotal && record.semanticProgressTotal > 0 ? (
    // 显示进度条
) : (
    <span style={{color: '#999'}}>准备中...</span>
)}
```

---

## 2026-04-15 语义切分异步处理

### 新增功能

**语义切分异步处理**

将语义切分改为异步处理，避免上传接口阻塞：

- **新增状态**: `SEMANTIC_PROCESSING` - 语义切分处理中
- **进度显示**: 文档列表显示"语义切分中 (50/100 句子)"
- **进度 API**: `GET /api/v1/documents/{id}/semantic-progress`
- **前端轮询**: 每2秒自动更新进度

**数据模型变更**

```java
// Document 实体新增字段
@Column(name = "semantic_progress_current")
private Integer semanticProgressCurrent;  // 已处理句子数

@Column(name = "semantic_progress_total")
private Integer semanticProgressTotal;     // 总句子数
```

**处理流程**

```
上传文档 (SEMANTIC)
    → 创建文档记录 (status=SEMANTIC_PROCESSING)
    → 返回响应（快速，< 1秒）
    → 异步线程开始处理：
        1. 提取文本
        2. 分批调用 Embedding API（每批10个）
        3. 每批完成后更新进度到数据库
        4. 完成后更新状态为 CHUNKED
```

---

## 2026-04-15 文档分块策略增强

### 新增功能

**引入 LangChain4j 框架进行文档分块**

使用 LangChain4j 0.36.2 的官方分块器，替代自定义实现：

| 策略             | LangChain4j 实现                  | 说明                          |
|----------------|---------------------------------|-----------------------------|
| **FIXED_SIZE** | `DocumentByCharacterSplitter`   | 按字符分块                       |
| **PARAGRAPH**  | `DocumentByParagraphSplitter`   | 按段落，过长段落递归分割                |
| **SENTENCE**   | `DocumentBySentenceSplitter`    | 按句子分块                       |
| **RECURSIVE**  | `DocumentSplitters.recursive()` | 段落→句子→词→字符递归分割              |
| **SEMANTIC**   | 自实现 + Embedding API             | 调用 Embedding 计算句子相似度，在断点处分割 |

**语义分块实现（基于 Embedding API）**

- 将文本拆成句子列表
- 批量调用 Embedding API 计算每个句子的向量
- 计算相邻句子的余弦相似度
- 找到相似度断点（低于第30百分位数）
- 在断点处分割，形成语义连贯的文本块
- 设置最大分块限制（默认1000字符）防止过长

**界面改进**

- 每种策略旁边显示小问号按钮（Tooltip介绍）
- 根据策略自动显示/隐藏分块大小和重叠输入框
- SEMANTIC策略需选择Embedding模型

### 依赖变更

```xml
<!-- 新增 LangChain4j -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.36.2</version>
</dependency>
```

### 代码改动

**后端** (`DocumentServiceImpl.java`)

- 引入 LangChain4j 分块器类
- `splitText()` 使用 LangChain4j 分块器替代自定义实现
- 删除自定义分块方法（splitFixedSize, splitByParagraph等）
- 保留语义分块方法 `splitBySemanticWithEmbedding()`

**前端** (`Documents/index.tsx`, `api/documents.ts`)

- 新增 Tooltip 组件介绍每种策略
- 策略选择动态显示/隐藏参数输入
- SEMANTIC策略显示Embedding模型选择
- API增加 `embeddingModelId` 参数

### 测试结果

- 后端测试：172 个测试全部通过
- 前端构建：成功

---

## 2026-04-13 RAG评估系统改造

### 已完成功能

- EmbeddingService：文本向量计算服务
- 向量检索：pgvector集成，余弦相似度搜索
- 数据集增强：expectedDocIds/context字段
- 评估任务增强：documentId/enableRag字段
- 评估结果增强：semanticSimilarity/retrievalScore/faithfulness字段
- 多指标评估：AI评分 + 语义相似度 + 检索得分 + 忠实度

---

## 2026-04-26 MCP Server 管理优化

### 功能概述

优化 MCP Server 创建流程和删除体验，支持创建时直接传入描述，删除时检查 Agent 引用并自动解绑。

### 改动详情

#### 1. MCP Server 创建优化

**后端** (`McpServerJsonRequest.java`, `McpServerServiceImpl.java`)

- `McpServerJsonRequest` 新增 `description` 字段
- `createFromJson()` 和 `updateFromJson()` 优先使用请求中的 description
- 创建时不再需要二次调用更新接口

**前端** (`api/mcp.ts`, `pages/McpServers/index.tsx`)

- `createMcpServerFromJson(configJson, description?)` 支持可选描述参数
- `updateMcpServerFromJson(id, configJson, description?)` 支持可选描述参数
- 创建和编辑都只需一次 API 调用
- Modal 添加 `confirmLoading` 状态

#### 2. MCP Server 删除优化

**后端** (`McpServerServiceImpl.java`, `McpServerController.java`)

- 新增 `GET /{id}/referencing-agents` 接口查询引用该 Server 的 Agent 列表
- `delete()` 方法先删除工具与 Agent 的绑定，再删除工具和 Server
- 使用 `AgentToolRepository` 查询和删除绑定关系

**前端** (`pages/McpServers/index.tsx`)

- 点击删除时先调用 `getReferencingAgents()` 检查引用
- 弹窗显示引用的 Agent 列表
- 确认删除后才调用 `deleteMcpServer()`
- 无引用时直接显示确认对话框

#### 3. Agent 详情页 Tab 保持

**前端** (`pages/Agents/AgentDetail.tsx`)

- 添加 `activeTab` 状态控制当前选中 Tab
- 工具绑定/解绑后只刷新工具列表，不刷新整个 Agent
- 操作完成后保持在"工具绑定" Tab

#### 4. 错误提示优化

**后端** (`StdioMcpClient.java`, `McpServerServiceImpl.java`, `McpExceptionHandler.java`)

- `StdioMcpClient.startProcess()` 增加命令存在性检查
- `connect()` 方法提取原始错误信息，避免多层包装
- `refreshTools()` 直接抛出原始异常而非包装成 RuntimeException
- 新增 `McpExceptionHandler` 全局异常处理器，返回友好错误信息

#### 5. MCP Client 单例问题修复

**后端** (`McpServerServiceImpl.java`)

- `getClient()` 每次创建新的 Client 实例
- 解决多个 Server 连接状态冲突问题

### 文件变更

| 类型 | 文件路径                                                                                           |
|----|------------------------------------------------------------------------------------------------|
| 新增 | `admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/AgentInfoDTO.java`               |
| 新增 | `admin-server-runtime/src/main/java/com/aiagent/admin/api/advice/McpExceptionHandler.java`     |
| 修改 | `admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/McpServerJsonRequest.java`       |
| 修改 | `admin-server-runtime/src/main/java/com/aiagent/admin/service/McpServerService.java`           |
| 修改 | `admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/McpServerServiceImpl.java`  |
| 修改 | `admin-server-runtime/src/main/java/com/aiagent/admin/api/controller/McpServerController.java` |
| 修改 | `admin-server-runtime/src/main/java/com/aiagent/admin/service/mcp/StdioMcpClient.java`         |
| 修改 | `frontend/src/api/mcp.ts`                                                                      |
| 修改 | `frontend/src/types/agent.ts`                                                                  |
| 修改 | `frontend/src/pages/McpServers/index.tsx`                                                      |
| 修改 | `frontend/src/pages/Agents/AgentDetail.tsx`                                                    |

### 待完成功能

- MCP 连接错误提示进一步细化（需要重启后端验证）
