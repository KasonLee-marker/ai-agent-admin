package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 创建 MCP Server 请求 DTO
 * <p>
 * 用于创建新的 MCP Server 配置，支持 stdio 和 SSE transport。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMcpServerRequest {

    /**
     * MCP Server 名称
     */
    @NotBlank(message = "Name is required")
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * Transport 类型
     * <p>
     * 支持：stdio、sse
     * </p>
     */
    @Builder.Default
    private String transportType = "stdio";

    /**
     * 启动命令（stdio transport）
     */
    private String command;

    /**
     * SSE URL（sse transport）
     */
    private String url;

    /**
     * 命令参数（stdio transport）
     */
    private List<String> args;

    /**
     * 环境变量（stdio transport）
     */
    private Map<String, String> env;
}