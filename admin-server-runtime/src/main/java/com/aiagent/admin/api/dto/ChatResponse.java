package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息响应 DTO
 * <p>
 * 返回单条聊天消息的详细信息，包括角色、内容、Token 数量等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 消息 ID
     */
    private String id;

    /** 所属会话 ID */
    private String sessionId;

    /** 消息角色（USER/ASSISTANT/SYSTEM） */
    private MessageRole role;

    /** 消息内容 */
    private String content;

    /** 使用的模型名称 */
    private String modelName;

    /** Token 使用数量 */
    private Integer tokenCount;

    /** 响应延迟（毫秒） */
    private Long latencyMs;

    /** 是否为错误消息 */
    private Boolean isError;

    /** 错误信息（失败时） */
    private String errorMessage;

    // ========== RAG 相关字段 ==========

    /**
     * RAG 检索来源（仅助手消息有此字段）
     */
    private List<VectorSearchResult> sources;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 会话列表响应 DTO
     * <p>
     * 返回分页的会话列表信息。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionListResponse {

        /** 会话列表 */
        private List<ChatSessionDTO> sessions;

        /** 总数量 */
        private long total;

        /** 当前页码 */
        private int page;

        /** 每页大小 */
        private int size;
    }

    /**
     * 消息列表响应 DTO
     * <p>
     * 返回指定会话的消息列表。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageListResponse {

        /** 会话 ID */
        private String sessionId;

        /** 消息列表 */
        private List<ChatResponse> messages;

        /** 消息总数 */
        private long total;
    }
}
