package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.entity.KnowledgeBase.ReindexStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重索引进度响应
 * <p>
 * 返回知识库重索引的状态、进度百分比、错误信息等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "重索引进度响应")
public class ReindexProgressResponse {

    @Schema(description = "知识库 ID")
    private String knowledgeBaseId;

    @Schema(description = "重索引状态（NONE/IN_PROGRESS/COMPLETED/FAILED）")
    private ReindexStatus status;

    @Schema(description = "已处理分块数")
    private Integer current;

    @Schema(description = "总分块数")
    private Integer total;

    @Schema(description = "进度百分比（0-100）")
    private Integer percentage;

    @Schema(description = "重索引开始时间")
    private String startedAt;

    @Schema(description = "重索引完成时间")
    private String completedAt;

    @Schema(description = "错误信息（失败时）")
    private String errorMessage;

    @Schema(description = "当前 Embedding 模型 ID")
    private String currentEmbeddingModelId;

    @Schema(description = "新 Embedding 模型 ID（重索引目标）")
    private String newEmbeddingModelId;
}