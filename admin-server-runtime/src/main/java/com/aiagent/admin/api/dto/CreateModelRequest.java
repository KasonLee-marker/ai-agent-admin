package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.ModelProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 创建模型配置请求 DTO
 * <p>
 * 用于创建新的 AI 模型配置，包括供应商、模型名称、API Key、参数等。
 * </p>
 */
@Data
public class CreateModelRequest {

    /**
     * 配置名称（必填）
     */
    @NotBlank(message = "Name is required")
    private String name;

    /** 模型供应商（必填，如 OPENAI、DASHSCOPE） */
    @NotNull(message = "Provider is required")
    private ModelProvider provider;

    /** 模型名称（必填，如 gpt-4、qwen-max） */
    @NotBlank(message = "Model name is required")
    private String modelName;

    /** API Key（可选，可在创建后配置） */
    private String apiKey;

    /** API 基础 URL（可选，默认使用供应商默认地址） */
    private String baseUrl;

    /** 温度参数（默认 0.7） */
    private Double temperature = 0.7;

    /** 最大 Token 数（默认 2048） */
    private Integer maxTokens = 2048;

    /** Top-P 参数（默认 1.0） */
    private Double topP = 1.0;

    /** 额外参数（JSON 格式） */
    private Map<String, Object> extraParams;

    /** 是否为默认模型（默认 false） */
    private Boolean isDefault = false;

    /** 是否激活（默认 true） */
    private Boolean isActive = true;
}
