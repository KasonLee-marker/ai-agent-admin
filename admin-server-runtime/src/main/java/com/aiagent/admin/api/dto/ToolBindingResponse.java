package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具绑定响应 DTO
 * <p>
 * 返回 Agent-Tool 绑定关系的详细信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolBindingResponse {

    /**
     * 绑定关系 ID
     */
    private String id;

    /**
     * 工具 ID
     */
    private String toolId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具描述
     */
    private String toolDescription;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 工具配置
     */
    private Map<String, Object> config;
}