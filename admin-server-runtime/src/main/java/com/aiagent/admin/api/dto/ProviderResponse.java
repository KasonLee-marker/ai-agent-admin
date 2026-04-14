package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.ModelProvider;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Provider 信息响应 DTO
 * <p>
 * 返回模型供应商的配置信息，包括类型（Chat/Embedding）。
 * </p>
 */
@Data
@AllArgsConstructor
public class ProviderResponse {

    /**
     * Provider 名称（枚举值，如 OPENAI, OPENAI_EMBEDDING）
     */
    private String name;

    /**
     * 显示名称（如 OpenAI, OpenAI Embedding）
     */
    private String displayName;

    /**
     * 默认 API 基础 URL
     */
    private String defaultBaseUrl;

    /**
     * 模型类型（CHAT 或 EMBEDDING）
     */
    private String modelType;

    /**
     * 内置模型列表
     */
    private List<ModelProvider.BuiltinModel> builtinModels;
}
