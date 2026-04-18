package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 文档上传请求 DTO
 * <p>
 * 用于上传文档到知识库，包含文档名称和可选的元数据。
 * </p>
 */
@Data
@Schema(description = "文档上传请求")
public class DocumentUploadRequest {

    /**
     * 文档名称（必填）
     */
    @Schema(description = "文档名称", example = "产品手册.pdf")
    @NotBlank(message = "文档名称不能为空")
    private String name;

    /** 文档元数据（可选，JSON 格式） */
    @Schema(description = "元数据")
    private Map<String, Object> metadata;
}