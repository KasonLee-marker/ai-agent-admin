package com.aiagent.admin.service.tool.executor;

import com.aiagent.admin.service.mcp.*;
import com.aiagent.admin.service.tool.ExecutionContext;
import com.aiagent.admin.service.tool.ToolExecutor;
import com.aiagent.admin.service.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具执行器
 * <p>
 * 执行 MCP 类型工具，通过 MCP Client 调用外部 MCP Server。
 * 支持 stdio 和 SSE 两种 transport。
 * </p>
 * <p>
 * 执行流程：
 * <ol>
 *   <li>从工具配置中获取 MCP Server 信息</li>
 *   <li>根据 transportType 选择 Client</li>
 *   <li>建立或复用 MCP Client 连接</li>
 *   <li>调用 MCP 工具</li>
 *   <li>返回结果</li>
 * </ol>
 * </p>
 *
 * @see ToolExecutor
 * @see McpClient
 * @see StdioMcpClient
 * @see SseMcpClient
 */
@Slf4j
@Component("mcpToolExecutor")
@RequiredArgsConstructor
public class McpToolExecutor implements ToolExecutor {

    private final StdioMcpClient stdioMcpClient;
    private final SseMcpClient sseMcpClient;
    private final ObjectMapper objectMapper;

    /**
     * MCP Client 连接池
     * <p>
     * 按 Server ID 缓存连接，避免重复启动进程或建立连接。
     * </p>
     */
    private final Map<String, McpClient> clientPool = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "mcpToolExecutor";
    }

    @Override
    public String getSchema() {
        // MCP 工具的 Schema 由 MCP Server 提供，此处返回通用 Schema
        return "{\"type\":\"object\",\"properties\":{},\"description\":\"MCP tool - schema provided by MCP server\"}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ExecutionContext context) {
        long startTime = System.currentTimeMillis();

        // 获取 MCP Server 配置
        Map<String, Object> agentToolConfig = context.getAgentToolConfig();
        String mcpServerId = (String) agentToolConfig.get("mcpServerId");
        String mcpToolName = (String) agentToolConfig.get("mcpToolName");
        String transportType = (String) agentToolConfig.get("transportType");

        if (mcpServerId == null || mcpToolName == null) {
            return ToolResult.failure("MCP Server or Tool name not configured");
        }

        log.info("Executing MCP tool: {} from server: {} (transport: {})", mcpToolName, mcpServerId, transportType);

        try {
            // 获取或创建 MCP Client
            McpClient client = getOrCreateClient(mcpServerId, agentToolConfig);

            // 调用 MCP 工具
            McpToolResult mcpResult = client.callTool(mcpToolName, args);

            long duration = System.currentTimeMillis() - startTime;

            if (mcpResult.isSuccess()) {
                return ToolResult.success(mcpResult.getContent(), duration);
            } else {
                return ToolResult.failure(mcpResult.getErrorMessage(), duration);
            }

        } catch (McpConnectionException e) {
            log.error("MCP connection error: {}", e.getMessage());
            // 清除失效的连接
            clientPool.remove(mcpServerId);
            return ToolResult.failure("MCP connection failed: " + e.getMessage(),
                    System.currentTimeMillis() - startTime);

        } catch (McpToolExecutionException e) {
            log.error("MCP tool execution error: {}", e.getMessage());
            return ToolResult.failure("MCP tool execution failed: " + e.getMessage(),
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Unexpected MCP execution error: {}", e.getMessage(), e);
            return ToolResult.failure("Unexpected error: " + e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 获取或创建 MCP Client
     * <p>
     * 根据 transportType 选择对应的 Client 实现。
     * </p>
     */
    private McpClient getOrCreateClient(String serverId, Map<String, Object> config) throws McpConnectionException {
        // 检查连接池中是否有可用连接
        McpClient cachedClient = clientPool.get(serverId);
        if (cachedClient != null && cachedClient.isConnected()) {
            log.debug("Using cached MCP Client for server: {}", serverId);
            return cachedClient;
        }

        // 创建新连接
        log.info("Creating new MCP Client for server: {}", serverId);

        String transportType = (String) config.get("transportType");
        McpClient client = "sse".equals(transportType) ? sseMcpClient : stdioMcpClient;

        McpServerConfig mcpConfig = McpServerConfig.builder()
                .name((String) config.get("mcpServerName"))
                .transportType(transportType)
                .url((String) config.get("url"))
                .command((String) config.get("command"))
                .args(parseList(config.get("args")))
                .env(parseMap(config.get("env")))
                .build();

        client.connect(mcpConfig);
        clientPool.put(serverId, client);

        return client;
    }

    /**
     * 解析参数列表
     */
    private java.util.List<String> parseList(Object obj) {
        if (obj == null) {
            return java.util.Collections.emptyList();
        }
        if (obj instanceof java.util.List) {
            return (java.util.List<String>) obj;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class));
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 解析环境变量 Map
     */
    private Map<String, String> parseMap(Object obj) {
        if (obj == null) {
            return java.util.Collections.emptyMap();
        }
        if (obj instanceof Map) {
            return (Map<String, String>) obj;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        } catch (Exception e) {
            return java.util.Collections.emptyMap();
        }
    }

    /**
     * 清理连接池
     * <p>
     * 关闭所有缓存的 MCP Client 连接。
     * </p>
     */
    public void cleanup() {
        for (Map.Entry<String, McpClient> entry : clientPool.entrySet()) {
            try {
                entry.getValue().disconnect();
                log.info("Disconnected MCP Client for server: {}", entry.getKey());
            } catch (Exception e) {
                log.warn("Error disconnecting MCP Client for server {}: {}", entry.getKey(), e.getMessage());
            }
        }
        clientPool.clear();
    }
}