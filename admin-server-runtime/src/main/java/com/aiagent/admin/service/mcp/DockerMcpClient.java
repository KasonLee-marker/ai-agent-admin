package com.aiagent.admin.service.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP Client Docker 模式实现
 * <p>
 * 通过 Docker 容器内的 MCP Host Service 管理 stdio MCP Server 进程。
 * 所有 stdio MCP Server 共享一个容器，每个 Server 是容器内的一个独立进程。
 * </p>
 * <p>
 * 通信流程：
 * <ol>
 *   <li>通过 McpRuntimeManager 确保容器运行</li>
 *   <li>通过 HTTP API 在容器内启动 MCP Server 进程</li>
 *   <li>通过 HTTP JSON-RPC 代理与 MCP Server 通信</li>
 *   <li>断开时停止容器内对应进程</li>
 * </ol>
 * </p>
 *
 * @see McpRuntimeManager
 * @see StdioMcpClient
 */
@Slf4j
@Component
public class DockerMcpClient implements McpClient {

    private final ObjectMapper objectMapper;
    private final McpRuntimeManager runtimeManager;
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);

    private McpServerConfig config;
    private volatile boolean connected = false;
    private JsonNode serverCapabilities;
    private Integer processPid;

    public DockerMcpClient(ObjectMapper objectMapper, McpRuntimeManager runtimeManager) {
        this.objectMapper = objectMapper;
        this.runtimeManager = runtimeManager;
    }

    @Override
    public void connect(McpServerConfig config) throws McpConnectionException {
        if (connected) {
            throw new McpConnectionException("Already connected to: " + getServerName());
        }

        this.config = config;
        log.info("Connecting to MCP Server via Docker runtime: {}", config.getName());

        try {
            // 1. 确保容器正在运行
            runtimeManager.ensureContainerRunning();

            // 2. 构建启动请求参数
            String serverId = config.getName();
            List<String> command = buildCommand(config);
            Map<String, String> env = config.getEnv() != null ? config.getEnv() : new HashMap<>();

            // 3. 通过 HTTP API 启动容器内进程
            String startUrl = String.format("%s/servers/%s/start",
                    runtimeManager.getMcpHostBaseUrl(), serverId);

            Map<String, Object> startRequest = new HashMap<>();
            startRequest.put("command", command);
            startRequest.put("env", env);

            String startJson = objectMapper.writeValueAsString(startRequest);

            // 使用 HTTP 客户端发送请求
            String response = sendHttpPost(startUrl, startJson);
            JsonNode startResponse = objectMapper.readTree(response);

            if (!"started".equals(startResponse.path("status").asText()) &&
                    !"already_running".equals(startResponse.path("status").asText())) {
                throw new McpConnectionException("Failed to start MCP server process: " + response);
            }

            this.processPid = startResponse.path("pid").asInt();
            log.info("MCP Server process started in container: {}, PID: {}", serverId, processPid);

            // 4. 发送 initialize 请求
            JsonNode initResult = sendJsonRpcRequest("initialize", buildInitializeParams());
            serverCapabilities = initResult.path("capabilities");
            log.info("MCP Server capabilities: {}", serverCapabilities);

            // 5. 发送 initialized 通知
            sendJsonRpcNotification("notifications/initialized", Map.of());

            connected = true;
            log.info("Connected to MCP Server via Docker runtime: {}", config.getName());

        } catch (Exception e) {
            disconnect();
            throw new McpConnectionException(
                    "Failed to connect to MCP Server '" + config.getName() + "': " + extractRootCauseMessage(e), e);
        }
    }

    @Override
    public boolean isConnected() {
        if (!connected) {
            return false;
        }

        // 检查进程状态
        McpRuntimeManager.ProcessStatus status = runtimeManager.getProcessStatus(config.getName());
        return "running".equals(status.getStatus());
    }

    @Override
    public List<McpTool> listTools() throws McpConnectionException {
        if (!isConnected()) {
            throw new McpConnectionException("Not connected to MCP Server");
        }

        try {
            JsonNode result = sendJsonRpcRequest("tools/list", Map.of());
            JsonNode toolsNode = result.path("tools");

            List<McpTool> tools = new ArrayList<>();
            if (toolsNode.isArray()) {
                for (JsonNode toolNode : toolsNode) {
                    McpTool tool = parseTool(toolNode);
                    tools.add(tool);
                }
            }

            log.info("Found {} tools from MCP Server: {}", tools.size(), config.getName());
            return tools;

        } catch (Exception e) {
            throw new McpConnectionException("Failed to list tools from MCP Server: " + config.getName(), e);
        }
    }

    @Override
    public McpToolResult callTool(String toolName, Map<String, Object> args)
            throws McpConnectionException, McpToolExecutionException {
        if (!isConnected()) {
            throw new McpConnectionException("Not connected to MCP Server");
        }

        log.info("Calling MCP tool: {} with args: {}", toolName, args);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("name", toolName);
            params.put("arguments", args != null ? args : Map.of());

            JsonNode result = sendJsonRpcRequest("tools/call", params);

            boolean isError = result.path("isError").asBoolean(false);
            JsonNode contentNode = result.path("content");

            if (isError) {
                String errorMsg = extractErrorMessage(contentNode);
                return McpToolResult.error(errorMsg);
            }

            Object content = extractContent(contentNode);
            return McpToolResult.success(content);

        } catch (McpConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new McpToolExecutionException("Failed to call MCP tool: " + toolName, e);
        }
    }

    @Override
    public void disconnect() {
        if (!connected && processPid == null) {
            return;
        }

        log.info("Disconnecting from MCP Server: {}", config.getName());

        try {
            // 发送 shutdown 通知
            if (isConnected()) {
                sendJsonRpcNotification("notifications/shutdown", Map.of());
            }
        } catch (Exception e) {
            log.warn("Error sending shutdown notification: {}", e.getMessage());
        }

        // 停止容器内进程
        try {
            runtimeManager.stopServerProcess(config.getName());
            log.info("Stopped MCP server process: {}", config.getName());
        } catch (Exception e) {
            log.warn("Error stopping server process: {}", e.getMessage());
        }

        connected = false;
        processPid = null;
        log.info("Disconnected from MCP Server: {}", config.getName());
    }

    @Override
    public String getServerName() {
        return config != null ? config.getName() : null;
    }

    /**
     * 获取进程状态
     */
    public McpRuntimeManager.ProcessStatus getProcessStatus() {
        if (config == null) {
            return null;
        }
        return runtimeManager.getProcessStatus(config.getName());
    }

    /**
     * 获取进程日志
     */
    public String getProcessLogs(int lines) {
        if (config == null) {
            return "";
        }
        return runtimeManager.getProcessLogs(config.getName(), lines);
    }

    /**
     * 重启进程
     */
    public boolean restartProcess() {
        if (config == null) {
            return false;
        }

        try {
            // 先停止
            runtimeManager.stopServerProcess(config.getName());
            Thread.sleep(1000);

            // 重新连接
            connect(config);
            return true;
        } catch (Exception e) {
            log.error("Failed to restart process: {}", e.getMessage());
            return false;
        }
    }

    // ========== 私有方法 ==========

    private List<String> buildCommand(McpServerConfig config) {
        List<String> command = new ArrayList<>();
        command.add(config.getCommand());
        if (config.getArgs() != null) {
            command.addAll(config.getArgs());
        }
        return command;
    }

    private JsonNode sendJsonRpcRequest(String method, Map<String, Object> params) throws IOException {
        int requestId = requestIdCounter.getAndIncrement();

        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", requestId);
        request.put("method", method);
        request.put("params", params);

        String requestJson = objectMapper.writeValueAsString(request);
        log.debug("Sending JSON-RPC request: {}", requestJson);

        String url = String.format("%s/rpc/%s", runtimeManager.getMcpHostBaseUrl(), config.getName());
        String responseJson = sendHttpPost(url, requestJson);

        log.debug("Received JSON-RPC response: {}", responseJson);

        JsonNode response = objectMapper.readTree(responseJson);

        // 检查错误
        if (response.has("error")) {
            JsonNode error = response.get("error");
            String errorMsg = error.path("message").asText("Unknown error");
            throw new McpConnectionException("MCP Server error: " + errorMsg);
        }

        return response.get("result");
    }

    private void sendJsonRpcNotification(String method, Map<String, Object> params) throws IOException {
        Map<String, Object> notification = new java.util.LinkedHashMap<>();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);
        notification.put("params", params);

        String notificationJson = objectMapper.writeValueAsString(notification);
        log.debug("Sending JSON-RPC notification: {}", notificationJson);

        String url = String.format("%s/rpc/%s", runtimeManager.getMcpHostBaseUrl(), config.getName());
        sendHttpPost(url, notificationJson);
    }

    private String sendHttpPost(String url, String body) throws IOException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            java.net.http.HttpResponse<String> response = client.send(
                    request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IOException("HTTP error " + response.statusCode() + ": " + response.body());
            }

            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    private String extractRootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isEmpty()) {
            message = cause.getClass().getSimpleName();
        }
        return message;
    }

    private Map<String, Object> buildInitializeParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", Map.of(
                "tools", Map.of()
        ));
        params.put("clientInfo", Map.of(
                "name", "ai-agent-admin",
                "version", "2.0.0"
        ));
        return params;
    }

    private McpTool parseTool(JsonNode toolNode) {
        String name = toolNode.path("name").asText();
        String description = toolNode.path("description").asText("");
        JsonNode inputSchemaNode = toolNode.path("inputSchema");

        Map<String, Object> inputSchema = new HashMap<>();
        if (inputSchemaNode.isObject()) {
            inputSchema = objectMapper.convertValue(inputSchemaNode, Map.class);
        }

        return McpTool.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .serverName(config.getName())
                .build();
    }

    private String extractErrorMessage(JsonNode contentNode) {
        if (contentNode.isArray()) {
            for (JsonNode item : contentNode) {
                if ("text".equals(item.path("type").asText())) {
                    return item.path("text").asText();
                }
            }
        }
        return "Unknown error";
    }

    private Object extractContent(JsonNode contentNode) {
        if (contentNode.isArray()) {
            List<Object> contents = new ArrayList<>();
            for (JsonNode item : contentNode) {
                String type = item.path("type").asText();
                if ("text".equals(type)) {
                    contents.add(item.path("text").asText());
                } else if ("image".equals(type)) {
                    contents.add(item.path("data").asText());
                } else if ("resource".equals(type)) {
                    contents.add(item.path("resource").asText());
                }
            }
            return contents.size() == 1 ? contents.get(0) : contents;
        }
        return contentNode.asText();
    }
}
