package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天会话 DTO
 * <p>
 * 表示一个聊天会话的基本信息，包括标题、模型配置、消息数量等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDTO {

    /**
     * 会话 ID
     */
    private String id;

    /** 会话标题 */
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    /** 默认模型配置 ID */
    private String modelId;

    /** 关联的提示词模板 ID */
    private String promptId;

    /** 系统消息 */
    @Size(max = 500, message = "System message must not exceed 500 characters")
    private String systemMessage;

    /** 消息数量 */
    private Integer messageCount;

    /** 是否活跃 */
    private Boolean isActive;

    // ========== RAG 配置字段 ==========

    /**
     * 是否启用 RAG 检索增强
     */
    private Boolean enableRag;

    /**
     * 关联知识库 ID
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
     * RAG 检索策略
     */
    private String ragStrategy;

    /**
     * RAG Embedding 模型 ID
     */
    private String ragEmbeddingModelId;

    // ========== Agent 关联字段 ==========

    /**
     * 关联 Agent ID（可选）
     */
    private String agentId;

    /**
     * Agent 名称（用于显示）
     */
    private String agentName;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 创建者 */
    private String createdBy;
}
