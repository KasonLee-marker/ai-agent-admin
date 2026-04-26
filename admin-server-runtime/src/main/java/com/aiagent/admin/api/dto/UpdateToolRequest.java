package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 更新自定义工具请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateToolRequest {

    /**
     * 工具名称
     */
    @NotBlank(message = "Tool name is required")
    private String name;

    /**
     * 工具描述
     */
    @NotBlank(message = "Tool description is required")
    private String description;

    /**
     * 工具类别
     */
    private String category;

    /**
     * JSON Schema（输入参数定义）
     */
    private Map<String, Object> schema;

    /**
     * HTTP API endpoint
     */
    private String endpoint;

    /**
     * 认证类型
     */
    private String authType;

    /**
     * 认证配置
     */
    private Map<String, Object> authConfig;

    /**
     * 请求模板
     */
    private String requestTemplate;

    /**
     * 响应字段提取规则
     */
    private String responseMapping;
}