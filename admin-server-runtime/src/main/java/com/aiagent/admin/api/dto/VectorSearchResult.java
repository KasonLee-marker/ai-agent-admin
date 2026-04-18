package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量搜索结果 DTO
 * <p>
 * 返回单个向量搜索结果，包含分块内容、相似度分数、元数据等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量搜索结果")
public class VectorSearchResult {

    /**
     * 分块 ID
     */
    @Schema(description = "分块ID")
    private String chunkId;

    /** 所属文档 ID */
    @Schema(description = "文档ID")
    private String documentId;

    /** 文档名称 */
    @Schema(description = "文档名称")
    private String documentName;

    /** 分块索引 */
    @Schema(description = "分块索引")
    private Integer chunkIndex;

    /** 分块内容 */
    @Schema(description = "内容")
    private String content;

    /** 相似度分数（0-1） */
    @Schema(description = "相似度分数")
    private Double score;

    /** 分块元数据 */
    @Schema(description = "元数据")
    private String metadata;
}