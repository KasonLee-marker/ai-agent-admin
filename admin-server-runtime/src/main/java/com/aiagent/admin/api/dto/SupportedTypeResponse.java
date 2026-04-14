package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支持的文件类型响应 DTO
 * <p>
 * 包含 MIME 类型、文件扩展名和显示名称，
 * 用于前端展示支持的文件类型列表。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "支持的文件类型")
public class SupportedTypeResponse {

    @Schema(description = "内容类型（MIME）")
    private String contentType;

    @Schema(description = "文件扩展名")
    private String extension;

    @Schema(description = "显示名称")
    private String displayName;
}