package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 会话响应 DTO
 * <p>
 * 返回 RAG 会话的详细信息，包括知识库、模型配置、消息数量等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG会话响应")
public class RagSessionDTO {

    /**
     * 会话 ID
     */
    @Schema(description = "会话ID")
    private String id;

    /** 会话标题 */
    @Schema(description = "会话标题")
    private String title;

    /** 关联知识库 ID */
    @Schema(description = "知识库ID")
    private String knowledgeBaseId;

    /** 知识库名称 */
    @Schema(description = "知识库名称")
    private String knowledgeBaseName;

    /** 对话模型 ID */
    @Schema(description = "对话模型ID")
    private String modelId;

    /** 对话模型名称 */
    @Schema(description = "对话模型名称")
    private String modelName;

    /** Embedding 模型 ID */
    @Schema(description = "Embedding模型ID")
    private String embeddingModelId;

    /** Embedding 模型名称 */
    @Schema(description = "Embedding模型名称")
    private String embeddingModelName;

    /** 消息数量 */
    @Schema(description = "消息数量")
    private Integer messageCount;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 创建者 */
    @Schema(description = "创建人")
    private String createdBy;
}