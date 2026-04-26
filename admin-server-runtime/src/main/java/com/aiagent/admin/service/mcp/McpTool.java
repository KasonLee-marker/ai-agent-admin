package com.aiagent.admin.service.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP 工具定义
 * <p>
 * 表示 MCP Server 提供的一个工具，包含名称、描述和输入 Schema。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 输入参数 Schema
     * <p>
     * JSON Schema 格式，定义工具接受的参数。
     * </p>
     */
    private Map<String, Object> inputSchema;

    /**
     * MCP Server 名称
     */
    private String serverName;
}