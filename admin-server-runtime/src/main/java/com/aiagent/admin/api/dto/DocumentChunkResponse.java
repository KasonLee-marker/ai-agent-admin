package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档分块响应")
public class DocumentChunkResponse {

    @Schema(description = "分块ID")
    private String id;

    @Schema(description = "文档ID")
    private String documentId;

    @Schema(description = "分块索引")
    private Integer chunkIndex;

    @Schema(description = "分块内容")
    private String content;

    @Schema(description = "元数据")
    private String metadata;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}