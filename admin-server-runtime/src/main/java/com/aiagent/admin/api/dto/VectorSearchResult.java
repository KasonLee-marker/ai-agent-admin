package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量搜索结果")
public class VectorSearchResult {

    @Schema(description = "分块ID")
    private String chunkId;

    @Schema(description = "文档ID")
    private String documentId;

    @Schema(description = "文档名称")
    private String documentName;

    @Schema(description = "分块索引")
    private Integer chunkIndex;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "相似度分数")
    private Double score;

    @Schema(description = "元数据")
    private String metadata;
}