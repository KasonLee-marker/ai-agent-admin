package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.entity.KnowledgeBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库响应 DTO
 * <p>
 * 用于返回知识库详情信息，包括重索引状态。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库响应")
public class KnowledgeBaseResponse {

    /**
     * 知识库 ID
     */
    @Schema(description = "知识库 ID")
    private String id;

    /**
     * 知识库名称
     */
    @Schema(description = "知识库名称")
    private String name;

    /**
     * 知识库描述
     */
    @Schema(description = "知识库描述")
    private String description;

    /**
     * 默认 Embedding 模型 ID
     */
    @Schema(description = "默认 Embedding 模型 ID")
    private String defaultEmbeddingModelId;

    /**
     * 默认 Embedding 模型名称
     */
    @Schema(description = "默认 Embedding 模型名称")
    private String defaultEmbeddingModelName;

    /**
     * 文档数量
     */
    @Schema(description = "文档数量")
    private Integer documentCount;

    /**
     * 分块总数
     */
    @Schema(description = "分块总数")
    private Integer chunkCount;

    /**
     * 重索引状态
     */
    @Schema(description = "重索引状态：NONE/IN_PROGRESS/COMPLETED/FAILED")
    private KnowledgeBase.ReindexStatus reindexStatus;

    /**
     * 重索引进度：已处理分块数
     */
    @Schema(description = "重索引进度：已处理分块数")
    private Integer reindexProgressCurrent;

    /**
     * 重索引进度：总分块数
     */
    @Schema(description = "重索引进度：总分块数")
    private Integer reindexProgressTotal;

    /**
     * 重索引错误信息
     */
    @Schema(description = "重索引错误信息")
    private String reindexErrorMessage;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createdBy;
}