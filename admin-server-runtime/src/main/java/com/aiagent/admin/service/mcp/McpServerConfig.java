package com.aiagent.admin.service.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 配置
 * <p>
 * 定义如何连接 MCP Server，支持 stdio 和 SSE transport。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerConfig {

    /**
     * Server 名称（标识）
     */
    private String name;

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
     * <p>
     * 例如：uvx、npx、python
     * </p>
     */
    private String command;

    /**
     * SSE URL（sse transport）
     * <p>
     * 例如：https://xxx/sse
     * </p>
     */
    private String url;

    /**
     * 命令参数（stdio transport）
     * <p>
     * 例如：["mcp-server-fetch"]
     * </p>
     */
    private List<String> args;

    /**
     * 环境变量（stdio transport）
     * <p>
     * 例如：{"API_KEY": "xxx"}
     * </p>
     */
    private Map<String, String> env;

    /**
     * 连接超时（毫秒）
     * <p>
     * 默认 30000ms
     * </p>
     */
    @Builder.Default
    private long connectTimeoutMs = 30000;

    /**
     * 工具调用超时（毫秒）
     * <p>
     * 默认 30000ms
     * </p>
     */
    @Builder.Default
    private long toolCallTimeoutMs = 30000;

    /**
     * 获取连接地址
     * <p>
     * SSE transport 返回 url，stdio transport 返回 command。
     * </p>
     */
    public String getConnectionAddress() {
        if ("sse".equals(transportType)) {
            return url;
        }
        return command;
    }
}