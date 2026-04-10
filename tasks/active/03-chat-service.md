# 任务：对话调试模块开发

## 状态：✅ 已完成

## 任务清单

### Core 层 (admin-server-core)
- [x] 创建 MessageRole 枚举 (USER/ASSISTANT/SYSTEM)
- [x] 创建 ChatSession 实体
- [x] 创建 ChatMessage 实体

### Runtime 层 (admin-server-runtime)
- [x] 创建 ChatSessionRepository
- [x] 创建 ChatMessageRepository
- [x] 创建 ChatService 接口
- [x] 创建 ChatServiceImpl 实现
- [x] 创建 ChatController
- [x] 创建 DTO (ChatRequest, ChatResponse, ChatSessionDTO)

### API 功能
- [x] POST /api/chat/sessions - 创建会话
- [x] POST /api/chat/messages - 发送消息
- [x] GET /api/chat/sessions/{id}/messages - 获取会话历史
- [x] GET /api/chat/sessions - 获取会话列表

### 测试
- [x] ChatServiceImplTest - 11 个测试用例
- [x] ChatControllerTest - 10 个测试用例

### 文档
- [x] 更新 AGENTS.md 模块状态

## 文件清单

### Core 层
1. `admin-server-core/src/main/java/com/aiagent/admin/domain/enums/MessageRole.java`
2. `admin-server-core/src/main/java/com/aiagent/admin/domain/entity/ChatSession.java`
3. `admin-server-core/src/main/java/com/aiagent/admin/domain/entity/ChatMessage.java`

### Runtime 层
4. `admin-server-runtime/src/main/java/com/aiagent/admin/domain/repository/ChatSessionRepository.java`
5. `admin-server-runtime/src/main/java/com/aiagent/admin/domain/repository/ChatMessageRepository.java`
6. `admin-server-runtime/src/main/java/com/aiagent/admin/service/ChatService.java`
7. `admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/ChatServiceImpl.java`
8. `admin-server-runtime/src/main/java/com/aiagent/admin/api/controller/ChatController.java`
9. `admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/ChatSessionDTO.java`
10. `admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/ChatRequest.java`
11. `admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/ChatResponse.java`

### 测试
12. `admin-server-runtime/src/test/java/com/aiagent/admin/service/ChatServiceImplTest.java`
13. `admin-server-runtime/src/test/java/com/aiagent/admin/api/controller/ChatControllerTest.java`

## 测试通过率
- **总测试数**: 74
- **通过**: 74
- **失败**: 0
- **跳过**: 0
- **通过率**: 100%

## 技术实现

### 技术约束
- 使用百炼 API (qwen3.5-omni-plus) - 通过 ModelConfig 配置
- 使用 Spring AI 的 OpenAiChatClient 调用模型
- 参考已有 PromptService 和 ModelService 的实现风格

### API 端点

| 方法     | 端点                                  | 描述                         |
|--------|-------------------------------------|----------------------------|
| POST   | /api/v1/chat/sessions               | 创建新会话                      |
| GET    | /api/v1/chat/sessions               | 获取会话列表（支持关键词搜索）            |
| GET    | /api/v1/chat/sessions/{id}          | 获取单个会话详情                   |
| DELETE | /api/v1/chat/sessions/{id}          | 删除会话                       |
| POST   | /api/v1/chat/messages               | 发送消息并获取 AI 响应（同步）          |
| POST   | /api/v1/chat/messages/stream        | 发送消息并获取流式响应（SSE）           |
| GET    | /api/v1/chat/sessions/{id}/messages | 获取会话所有消息                   |
| GET    | /api/v1/chat/sessions/{id}/history  | 获取对话历史（仅 USER 和 ASSISTANT） |

### 核心功能
1. **会话管理**: 创建、查询、删除对话会话
2. **消息发送**: 支持调用百炼 API 获取 AI 响应
3. **流式输出**: SSE 流式返回，实时显示生成内容
4. **历史记录**: 保存和查询对话历史
5. **模型切换**: 支持为不同会话配置不同模型
6. **错误处理**: 模型调用失败时记录错误信息

## 流式输出实现 (2026-04-11)

### 后端实现

- `ChatServiceImpl.sendMessageStream()` 使用 `OpenAiChatClient.stream()` 返回 `Flux<String>`
- SSE 格式: `data: xxx\n`
- 流式完成后自动保存完整响应到数据库

### 关键代码

```java
return chatClient.stream(prompt)
    .map(response -> {
        String content = response.getResult().getOutput().getContent();
        if (content != null) {
            fullResponse.append(content);
            return content;
        }
        return "";
    })
    .doOnComplete(() -> saveAssistantMessage(session, modelConfig, fullResponse.toString()));
```

## 前端流式渲染 (2026-04-11)

### 实现方案

- fetch + ReadableStream 读取 SSE
- react-markdown 实时渲染 Markdown
- 临时消息显示 + 完成后刷新真实数据

### 相关文件

- `frontend/src/api/chat.ts` - `sendMessageStream()` SSE 解析
- `frontend/src/pages/Chat/index.tsx` - 流式消息渲染
