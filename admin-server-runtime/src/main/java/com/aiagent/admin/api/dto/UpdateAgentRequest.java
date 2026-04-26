package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新 Agent 请求 DTO
 * <p>
 * 支持更新 Agent 的基本信息、模型绑定、系统提示词、工具配置。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgentRequest {

    /**
     * Agent 名称
     */
    @NotBlank(message = "Agent name is required")
    private String name;

    /**
     * Agent 描述
     */
    private String description;

    /**
     * 绑定的模型 ID
     */
    @NotBlank(message = "Model ID is required")
    private String modelId;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * Agent 运行配置
     */
    private AgentConfigRequest config;
}