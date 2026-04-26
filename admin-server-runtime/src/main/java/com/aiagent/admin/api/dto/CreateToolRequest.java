package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 创建自定义工具请求 DTO
 * <p>
 * 用于创建 CUSTOM 类型的 HTTP API 工具。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateToolRequest {

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
     * <p>
     * CUSTOM 类型工具必须提供。
     * </p>
     */
    private String endpoint;

    /**
     * 认证类型
     * <p>
     * NONE | API_KEY | BASIC | BEARER
     * </p>
     */
    private String authType;

    /**
     * 认证配置
     * <p>
     * 根据 authType 提供相应配置：
     * <ul>
     *   <li>API_KEY: {headerName, headerValue}</li>
     *   <li>BASIC: {username, password}</li>
     *   <li>BEARER: {token}</li>
     * </ul>
     * </p>
     */
    private Map<String, Object> authConfig;

    /**
     * 请求模板
     * <p>
     * JSON 模板，支持参数占位符 ${paramName}。
     * </p>
     */
    private String requestTemplate;

    /**
     * 响应字段提取规则
     * <p>
     * 如 "data.results" 表示从响应中提取 data.results 字段。
     * </p>
     */
    private String responseMapping;
}