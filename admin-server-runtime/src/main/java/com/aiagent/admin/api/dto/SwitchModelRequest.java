package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 切换模型请求 DTO
 * <p>
 * 用于切换会话使用的模型配置。
 * </p>
 */
@Data
public class SwitchModelRequest {

    /**
     * 目标模型配置 ID（必填）
     */
    @NotBlank(message = "Model ID is required")
    private String modelId;
}
