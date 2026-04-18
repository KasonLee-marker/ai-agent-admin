package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档分块响应 DTO
 * <p>
 * 返回文档分块的详细信息，包括分块索引、内容、元数据等。
 * 用于文档内容检索和分块管理。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档分块响应")
public class DocumentChunkResponse {

    /**
     * 分块 ID
     */
    @Schema(description = "分块ID")
    private String id;

    /** 所属文档 ID */
    @Schema(description = "文档ID")
    private String documentId;

    /** 分块索引（文档中的顺序） */
    @Schema(description = "分块索引")
    private Integer chunkIndex;

    /** 分块内容 */
    @Schema(description = "分块内容")
    private String content;

    /** 分块元数据（JSON 格式） */
    @Schema(description = "元数据")
    private String metadata;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}