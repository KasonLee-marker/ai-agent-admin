package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建 Agent 请求 DTO
 * <p>
 * 包含 Agent 的基本信息、模型绑定、系统提示词、工具绑定列表。
 * </p>
 *
 * @see ToolBindingRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentRequest {

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
     * 工具绑定列表
     * <p>
     * 创建时可同时绑定多个工具。
     * </p>
     */
    private List<ToolBindingRequest> tools;

    /**
     * Agent 运行配置
     */
    private AgentConfigRequest config;
}