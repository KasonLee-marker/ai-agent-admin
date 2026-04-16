package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 会话响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG会话响应")
public class RagSessionDTO {

    @Schema(description = "会话ID")
    private String id;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "知识库ID")
    private String knowledgeBaseId;

    @Schema(description = "知识库名称")
    private String knowledgeBaseName;

    @Schema(description = "对话模型ID")
    private String modelId;

    @Schema(description = "对话模型名称")
    private String modelName;

    @Schema(description = "Embedding模型ID")
    private String embeddingModelId;

    @Schema(description = "Embedding模型名称")
    private String embeddingModelName;

    @Schema(description = "消息数量")
    private Integer messageCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "创建人")
    private String createdBy;
}