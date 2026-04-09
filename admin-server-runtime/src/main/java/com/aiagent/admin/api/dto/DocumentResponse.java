package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.entity.Document;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

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

    @Schema(description = "Embedding模型")
    private String embeddingModel;

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