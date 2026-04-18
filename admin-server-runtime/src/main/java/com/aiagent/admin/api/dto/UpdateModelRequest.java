package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.ModelProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 更新模型配置请求 DTO
 * <p>
 * 用于更新现有的模型配置，修改供应商、模型名称、参数等。
 * </p>
 */
@Data
public class UpdateModelRequest {

    /**
     * 配置名称（必填）
     */
    @NotBlank(message = "Name is required")
    private String name;

    /** 模型供应商（必填） */
    @NotNull(message = "Provider is required")
    private ModelProvider provider;

    /** 模型名称（必填） */
    @NotBlank(message = "Model name is required")
    private String modelName;

    /** 新 API Key（可选） */
    private String apiKey;

    /** 新 API 基础 URL（可选） */
    private String baseUrl;

    /** 温度参数（默认 0.7） */
    private Double temperature = 0.7;

    /** 最大 Token 数（默认 2048） */
    private Integer maxTokens = 2048;

    /** Top-P 参数（默认 1.0） */
    private Double topP = 1.0;

    /** 额外参数 */
    private Map<String, Object> extraParams;

    /** 是否激活（默认 true） */
    private Boolean isActive = true;
}
