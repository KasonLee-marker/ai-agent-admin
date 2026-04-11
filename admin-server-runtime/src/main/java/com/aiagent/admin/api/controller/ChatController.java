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

    @PostMapping("/sessions")
    @Operation(summary = "Create a new chat session")
    public ApiResponse<ChatSessionDTO> createSession(
            @Valid @RequestBody ChatRequest.CreateSessionRequest request,
            @RequestAttribute(name = "userId", required = false) String userId) {
        String createdBy = userId != null ? userId : "anonymous";
        return ApiResponse.success(chatService.createSession(request, createdBy));
    }

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

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get chat session by ID")
    public ApiResponse<ChatSessionDTO> getSession(
            @Parameter(description = "Session ID") @PathVariable String id) {
        return ApiResponse.success(chatService.getSession(id));
    }

    @PutMapping("/sessions/{id}")
    @Operation(summary = "Update a chat session")
    public ApiResponse<ChatSessionDTO> updateSession(
            @Parameter(description = "Session ID") @PathVariable String id,
            @Valid @RequestBody ChatRequest.UpdateSessionRequest request) {
        return ApiResponse.success(chatService.updateSession(id, request));
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Delete a chat session")
    public ApiResponse<Void> deleteSession(
            @Parameter(description = "Session ID") @PathVariable String id) {
        chatService.deleteSession(id);
        return ApiResponse.success();
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a message and get AI response")
    public ApiResponse<ChatResponse> sendMessage(
            @Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.sendMessage(request));
    }

    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Send a message and get streaming AI response")
    public Flux<String> sendMessageStream(
            @Valid @RequestBody ChatRequest request) {
        return chatService.sendMessageStream(request);
    }

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
