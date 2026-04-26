package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具绑定请求 DTO
 * <p>
 * 用于 Agent 创建/更新时绑定工具，包含工具 ID 和配置。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolBindingRequest {

    /**
     * 工具 ID
     */
    @NotBlank(message = "Tool ID is required")
    private String toolId;

    /**
     * 是否启用
     * <p>
     * 默认启用。
     * </p>
     */
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 工具配置
     * <p>
     * Agent 级别的配置覆盖，例如：
     * <ul>
     *   <li>knowledge_retrieval: {"kbId": "xxx", "topK": 5}</li>
     * </ul>
     * </p>
     */
    private Map<String, Object> config;
}