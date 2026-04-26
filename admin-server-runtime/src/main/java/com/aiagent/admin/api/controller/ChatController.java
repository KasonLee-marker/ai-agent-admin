package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Agent;
import com.aiagent.admin.domain.entity.ChatSession;
import com.aiagent.admin.domain.repository.AgentRepository;
import com.aiagent.admin.domain.repository.ChatSessionRepository;
import com.aiagent.admin.service.ChatService;
import com.aiagent.admin.service.impl.AgentExecutionEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天调试 REST 控制器
 * <p>
 * 提供聊天会话和消息的管理 API：
 * <ul>
 *   <li>会话创建、查询、删除</li>
 *   <li>消息发送（同步和流式响应）</li>
 *   <li>会话历史查询</li>
 * </ul>
 * </p>
 * <p>
 * 流式响应通过 SSE（Server-Sent Events）格式输出，
 * 端点为 POST /messages/stream，响应类型为 text/event-stream。
 * </p>
 *
 * @see ChatService
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat Debug", description = "APIs for chat debugging and conversation management")
public class ChatController {

    private final ChatService chatService;
    private final AgentExecutionEngine executionEngine;
    private final AgentRepository agentRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建新的聊天会话
     *
     * @param request 创建会话请求，包含会话名称和可选的模型ID
     * @param userId  用户ID（从请求属性获取，用于关联会话）
     * @return 创建成功的会话信息
     */
    @PostMapping("/sessions")
    @Operation(summary = "Create a new chat session")
    public ApiResponse<ChatSessionDTO> createSession(
            @Valid @RequestBody ChatRequest.CreateSessionRequest request,
            @RequestAttribute(name = "userId", required = false) String userId) {
        String createdBy = userId != null ? userId : "anonymous";
        return ApiResponse.success(chatService.createSession(request, createdBy));
    }

    /**
     * 分页查询聊天会话列表
     * <p>
     * 支持按关键词搜索，结果按更新时间倒序排列。
     * </p>
     *
     * @param keyword 搜索关键词（可选，匹配会话名称）
     * @param page    页码（从0开始）
     * @param size    每页数量
     * @param userId  用户ID（用于筛选用户自己的会话）
     * @return 分页的会话列表
     */
    @GetMapping("/sessions")
    @Operation(summary = "List chat sessions with pagination")
    public ApiResponse<PageResponse<ChatSessionDTO>> listSessions(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @RequestAttribute(name = "userId", required = false) String userId) {
        String createdBy = userId != null ? userId : "anonymous";
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<ChatSessionDTO> sessions;
        if (keyword != null && !keyword.isEmpty()) {
            sessions = chatService.listSessionsByKeyword(createdBy, keyword, pageable);
        } else {
            sessions = chatService.listSessions(createdBy, pageable);
        }
        return ApiResponse.success(PageResponse.of(sessions, page, size, sessions.size()));
    }

    /**
     * 根据ID获取聊天会话详情
     *
     * @param id 会话ID
     * @return 会话详情信息
     */
    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get chat session by ID")
    public ApiResponse<ChatSessionDTO> getSession(
            @Parameter(description = "Session ID") @PathVariable String id) {
        return ApiResponse.success(chatService.getSession(id));
    }

    /**
     * 更新聊天会话信息
     *
     * @param id      会话ID
     * @param request 更新请求，包含新的会话名称或模型ID
     * @return 更新后的会话信息
     */
    @PutMapping("/sessions/{id}")
    @Operation(summary = "Update a chat session")
    public ApiResponse<ChatSessionDTO> updateSession(
            @Parameter(description = "Session ID") @PathVariable String id,
            @Valid @RequestBody ChatRequest.UpdateSessionRequest request) {
        return ApiResponse.success(chatService.updateSession(id, request));
    }

    /**
     * 删除聊天会话
     * <p>
     * 同时删除会话下的所有消息。
     * </p>
     *
     * @param id 会话ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Delete a chat session")
    public ApiResponse<Void> deleteSession(
            @Parameter(description = "Session ID") @PathVariable String id) {
        chatService.deleteSession(id);
        return ApiResponse.success();
    }

    /**
     * 发送消息并获取AI响应（同步模式）
     * <p>
     * 调用指定的AI模型生成回复，并保存用户消息和助手消息。
     * </p>
     *
     * @param request 消息请求，包含会话ID、消息内容、可选模型ID
     * @return 助手响应消息
     */
    @PostMapping("/messages")
    @Operation(summary = "Send a message and get AI response")
    public ApiResponse<ChatResponse> sendMessage(
            @Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.sendMessage(request));
    }

    /**
     * 发送消息并获取流式AI响应（SSE模式）
     * <p>
     * 支持普通对话和Agent对话的流式输出。
     * Agent对话会实时显示工具调用过程，使用结构化JSON事件格式。
     * </p>
     *
     * @param request 消息请求，包含会话ID、消息内容、可选模型ID
     * @return SSE Emitter
     */
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Send a message and get streaming AI response")
    public SseEmitter sendMessageStream(
            @Valid @RequestBody ChatRequest request) {

        log.info("sendMessageStream called: sessionId={}, content={}", request.getSessionId(), request.getContent());

        // 创建 SSE Emitter，超时 60 秒
        SseEmitter emitter = new SseEmitter(60000L);

        // 设置超时和错误回调
        emitter.onTimeout(() -> {
            log.warn("Chat SSE stream timeout for session: {}", request.getSessionId());
            try {
                emitter.send(SseEmitter.event().data("TIMEOUT"));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send timeout message: {}", e.getMessage());
            }
        });

        emitter.onError(e -> {
            log.error("Chat SSE emitter error: {}", e.getMessage());
        });

        emitter.onCompletion(() -> {
            log.info("Chat SSE stream completed for session: {}", request.getSessionId());
        });

        // 检查是否为 Agent 会话
        ChatSession session = chatSessionRepository.findById(request.getSessionId()).orElse(null);
        boolean isAgentSession = session != null && session.getAgentId() != null && !session.getAgentId().isEmpty();

        // 使用线程池执行流式任务
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                log.info("Starting stream execution for session: {}, isAgent: {}", request.getSessionId(), isAgentSession);

                if (isAgentSession) {
                    // Agent 会话：发送结构化 SSE 事件（JSON 格式）
                    executeAgentStreamWithEvents(session, request, emitter);
                } else {
                    // 普通会话：发送纯文本 SSE
                    chatService.sendMessageStreamWithCallback(request, content -> {
                        try {
                            log.debug("Sending SSE chunk: {}", content.substring(0, Math.min(50, content.length())));
                            emitter.send(SseEmitter.event().data(content));
                        } catch (IOException e) {
                            log.error("Failed to send SSE chunk: {}", e.getMessage());
                            throw new RuntimeException("SSE send failed", e);
                        }
                    });
                }

                log.info("Stream execution completed, calling emitter.complete()");
                emitter.complete();
            } catch (Exception e) {
                log.error("Stream execution error for session {}: {}", request.getSessionId(), e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().data("ERROR: " + e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("Failed to send error message: {}", ex.getMessage());
                    emitter.completeWithError(e);
                }
            } finally {
                executor.shutdown();
            }
        });

        log.info("Returning SseEmitter for session: {}", request.getSessionId());
        return emitter;
    }

    /**
     * 执行 Agent 会话的流式输出（结构化 SSE 事件）
     * <p>
     * 发送 JSON 格式的 SSE 事件，与 Agent 管理页面调试接口一致。
     * 完成后调用 Service 保存消息（用户消息 + 助手消息含 toolCalls）。
     * </p>
     */
    private void executeAgentStreamWithEvents(ChatSession session, ChatRequest request, SseEmitter emitter) {
        log.info("executeAgentStreamWithEvents: sessionId={}, agentId={}", session.getId(), session.getAgentId());

        // 1. 保存用户消息
        chatService.saveUserMessage(session.getId(), request.getContent());

        try {
            Agent agent = executionEngine.getAgent(session.getAgentId());

            // 2. 构建消息列表
            List<org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage> messages = new java.util.ArrayList<>();
            String systemPrompt = executionEngine.buildSystemPrompt(agent);
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage(
                        systemPrompt, org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
            }
            messages.add(new org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage(
                    request.getContent(), org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.Role.USER));

            // 3. 执行 Agent
            StringBuilder fullResponse = new StringBuilder();
            List<ToolCallRecord> toolCalls = new java.util.ArrayList<>();

            AgentExecutionEngine.ExecutionResult result = executionEngine.executeWithCallback(
                    session.getAgentId(), messages, new java.util.HashMap<>(),
                    event -> {
                        try {
                            // 发送 JSON 格式的 SSE 事件
                            String eventJson = objectMapper.writeValueAsString(event);
                            emitter.send(SseEmitter.event().data(eventJson));
                            log.debug("SSE event sent: type={}, sequence={}", event.getType(), event.getSequence());

                            // 收集响应内容和工具调用记录
                            if (event.getType() == StreamingEventType.MODEL_OUTPUT && event.getContent() != null) {
                                fullResponse.append(event.getContent());
                            } else if (event.getType() == StreamingEventType.TOOL_START && event.getToolCall() != null) {
                                toolCalls.add(event.getToolCall());
                            } else if (event.getType() == StreamingEventType.TOOL_END && event.getToolCall() != null) {
                                // 更新工具调用记录
                                for (int i = 0; i < toolCalls.size(); i++) {
                                    ToolCallRecord tc = toolCalls.get(i);
                                    if (tc.getToolName().equals(event.getToolCall().getToolName()) && tc.getSuccess() == null) {
                                        toolCalls.set(i, event.getToolCall());
                                        break;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            log.error("Failed to send SSE event: {}", e.getMessage());
                            throw new RuntimeException("SSE send failed", e);
                        } catch (Exception e) {
                            log.error("Failed to serialize event: {}", e.getMessage());
                        }
                    });

            log.info("Agent execution completed: success={}, toolCalls={}, contentLength={}",
                    result.success(), toolCalls.size(), fullResponse.length());

            // 4. 保存助手消息（含 toolCalls）
            chatService.saveAssistantMessageTransactional(
                    session.getId(),
                    null, // modelConfig 从 agent/session 自动获取
                    fullResponse.toString(),
                    null, // sources
                    toolCalls,
                    result.success(),
                    result.errorMessage()
            );

        } catch (Exception e) {
            log.error("Agent stream execution error: {}", e.getMessage(), e);
            // 即使出错也保存空消息
            chatService.saveAssistantMessageTransactional(
                    session.getId(), null, "", null, null, false, e.getMessage());
            throw e;
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    /**
     * 获取会话的所有消息
     *
     * @param id 会话ID
     * @return 消息列表响应，包含会话ID、消息列表和总数
     */
    @GetMapping("/sessions/{id}/messages")
    @Operation(summary = "Get all messages in a session")
    public ApiResponse<ChatResponse.MessageListResponse> getSessionMessages(
            @Parameter(description = "Session ID") @PathVariable String id) {
        List<ChatResponse> messages = chatService.getSessionMessages(id);
        ChatResponse.MessageListResponse response = ChatResponse.MessageListResponse.builder()
                .sessionId(id)
                .messages(messages)
                .total(messages.size())
                .build();
        return ApiResponse.success(response);
    }

    /**
     * 获取会话的对话历史（仅用户和助手消息）
     * <p>
     * 过滤掉系统消息，只返回用户(USER)和助手(ASSISTANT)角色的消息，
     * 用于构建对话上下文。
     * </p>
     *
     * @param id 会话ID
     * @return 对话历史消息列表
     */
    @GetMapping("/sessions/{id}/history")
    @Operation(summary = "Get conversation history (USER and ASSISTANT messages only)")
    public ApiResponse<ChatResponse.MessageListResponse> getConversationHistory(
            @Parameter(description = "Session ID") @PathVariable String id) {
        List<ChatResponse> messages = chatService.getConversationHistory(id);
        ChatResponse.MessageListResponse response = ChatResponse.MessageListResponse.builder()
                .sessionId(id)
                .messages(messages)
                .total(messages.size())
                .build();
        return ApiResponse.success(response);
    }
}
