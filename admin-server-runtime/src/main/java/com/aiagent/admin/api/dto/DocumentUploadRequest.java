package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "文档上传请求")
public class DocumentUploadRequest {

    @Schema(description = "文档名称", example = "产品手册.pdf")
    @NotBlank(message = "文档名称不能为空")
    private String name;

    @Schema(description = "元数据")
    private Map<String, Object> metadata;
}