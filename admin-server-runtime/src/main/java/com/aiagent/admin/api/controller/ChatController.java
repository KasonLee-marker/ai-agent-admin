package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.ChatService;
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
import reactor.core.publisher.Flux;

import java.util.List;

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
     * 返回 Server-Sent Events 格式的流式响应，
     * 前端可实时接收并渲染AI生成的文本。
     * </p>
     *
     * @param request 消息请求，包含会话ID、消息内容、可选模型ID
     * @return SSE 流式响应
     */
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Send a message and get streaming AI response")
    public Flux<String> sendMessageStream(
            @Valid @RequestBody ChatRequest request) {
        return chatService.sendMessageStream(request);
    }

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
