package com.aiagent.admin.service.mcp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MCP Client 工厂
 * <p>
 * 根据 McpServerConfig 的 runtimeMode 选择对应的 Client 实现：
 * <ul>
 *   <li>LOCAL: 使用 StdioMcpClient（本地子进程）</li>
 *   <li>DOCKER: 使用 DockerMcpClient（容器内进程）</li>
 * </ul>
 * </p>
 *
 * @see McpClient
 * @see StdioMcpClient
 * @see DockerMcpClient
 */
@Component
@RequiredArgsConstructor
public class McpClientFactory {

    private final StdioMcpClient stdioMcpClient;
    private final DockerMcpClient dockerMcpClient;
    private final SseMcpClient sseMcpClient;

    /**
     * 根据配置获取 MCP Client
     *
     * @param config MCP Server 配置
     * @return 对应的 MCP Client 实现
     */
    public McpClient getClient(McpServerConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("McpServerConfig cannot be null");
        }

        String transportType = config.getTransportType();

        // SSE transport 使用 SseMcpClient
        if ("sse".equalsIgnoreCase(transportType)) {
            return sseMcpClient;
        }

        // stdio transport 根据 runtimeMode 选择
        String runtimeMode = config.getRuntimeMode();
        if ("DOCKER".equalsIgnoreCase(runtimeMode)) {
            return dockerMcpClient;
        }

        // 默认使用 LOCAL 模式
        return stdioMcpClient;
    }

    /**
     * 根据 runtimeMode 字符串获取 Client（用于已知模式的情况）
     *
     * @param runtimeMode "LOCAL" 或 "DOCKER"
     * @return 对应的 MCP Client
     */
    public McpClient getClient(String runtimeMode) {
        if ("DOCKER".equalsIgnoreCase(runtimeMode)) {
            return dockerMcpClient;
        }
        return stdioMcpClient;
    }
}
