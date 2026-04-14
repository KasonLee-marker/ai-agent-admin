package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.entity.Document;
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
@Schema(description = "文档响应")
public class DocumentResponse {

    @Schema(description = "文档ID")
    private String id;

    @Schema(description = "文档名称")
    private String name;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "总分块数")
    private Integer totalChunks;

    @Schema(description = "分块策略")
    private String chunkStrategy;

    @Schema(description = "分块大小")
    private Integer chunkSize;

    @Schema(description = "分块重叠")
    private Integer chunkOverlap;

    @Schema(description = "已创建分块数")
    private Integer chunksCreated;

    @Schema(description = "已Embedding分块数")
    private Integer chunksEmbedded;

    @Schema(description = "Embedding模型ID")
    private String embeddingModelId;

    @Schema(description = "Embedding模型名称")
    private String embeddingModelName;

    @Schema(description = "向量维度")
    private Integer embeddingDimension;

    @Schema(description = "状态")
    private Document.DocumentStatus status;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "创建人")
    private String createdBy;
}