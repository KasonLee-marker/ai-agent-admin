package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息发送请求 DTO
 * <p>
 * 用于向指定会话发送消息，获取 AI 响应。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 会话 ID（必填）
     */
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    /** 消息内容（必填，最大 10000 字符） */
    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    /** 模型配置 ID（可选，不指定则使用会话默认模型） */
    private String modelId;

    /** 是否启用流式输出（可选） */
    private Boolean stream;

    /**
     * 创建聊天会话请求 DTO
     * <p>
     * 用于创建新的聊天会话，指定标题、模型和系统消息等。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSessionRequest {

        /** 会话标题（必填，最大 200 字符） */
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        private String title;

        /** 默认模型配置 ID（可选） */
        private String modelId;

        /** 关联的提示词模板 ID（可选） */
        private String promptId;

        /** 系统消息（可选，最大 500 字符） */
        @Size(max = 500, message = "System message must not exceed 500 characters")
        private String systemMessage;

        // ========== RAG 配置字段（可选） ==========

        /**
         * 是否启用 RAG 检索增强
         */
        private Boolean enableRag;

        /**
         * 关联知识库 ID（用于 RAG 检索）
         */
        private String knowledgeBaseId;

        /**
         * RAG 检索数量（topK）
         */
        private Integer ragTopK;

        /**
         * RAG 相似度阈值
         */
        private Double ragThreshold;

        /**
         * RAG 检索策略（VECTOR/BM25/HYBRID）
         */
        private String ragStrategy;

        /**
         * RAG Embedding 模型 ID
         */
        private String ragEmbeddingModelId;

        /**
         * 关联 Agent ID（可选）
         * <p>
         * 如果设置了 Agent，会话将使用 Agent 的模型、系统提示词和工具。
         * </p>
         */
        private String agentId;
    }

    /**
     * 更新会话请求 DTO
     * <p>
     * 用于更新现有会话的标题、模型、提示词模板和系统消息。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSessionRequest {

        /** 新标题（可选，最大 200 字符） */
        @Size(max = 200, message = "Title must not exceed 200 characters")
        private String title;

        /** 新模型配置 ID（可选） */
        private String modelId;

        /** 新提示词模板 ID（可选） */
        private String promptId;

        /** 新系统消息（可选，最大 5000 字符） */
        @Size(max = 5000, message = "System message must not exceed 5000 characters")
        private String systemMessage;

        // ========== RAG 配置字段（可选） ==========

        /**
         * 是否启用 RAG 检索增强
         */
        private Boolean enableRag;

        /**
         * 关联知识库 ID（用于 RAG 检索）
         */
        private String knowledgeBaseId;

        /**
         * RAG 检索数量（topK）
         */
        private Integer ragTopK;

        /**
         * RAG 相似度阈值
         */
        private Double ragThreshold;

        /** RAG 检索策略（VECTOR/BM25/HYBRID） */
        private String ragStrategy;

        /** RAG Embedding 模型 ID */
        private String ragEmbeddingModelId;

        /**
         * 关联 Agent ID（可选）
         * <p>
         * 如果设置了 Agent，会话将使用 Agent 的模型、系统提示词和工具。
         * </p>
         */
        private String agentId;
    }
}
