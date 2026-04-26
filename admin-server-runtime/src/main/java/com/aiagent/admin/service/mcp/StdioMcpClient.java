package com.aiagent.admin.service.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP Client Stdio Transport 实现
 * <p>
 * 通过 stdio（标准输入/输出）与 MCP Server 进程通信。
 * 使用 JSON-RPC 2.0 协议格式。
 * </p>
 * <p>
 * 执行流程：
 * <ol>
 *   <li>启动 MCP Server 进程</li>
 *   <li>发送 initialize 请求</li>
 *   <li>发送 initialized 通知</li>
 *   <li>获取工具列表 (tools/list)</li>
 *   <li>调用工具 (tools/call)</li>
 *   <li>关闭时发送 shutdown 通知并终止进程</li>
 * </ol>
 * </p>
 *
 * @see McpClient
 * @see McpServerConfig
 */
@Slf4j
@Component
public class StdioMcpClient implements McpClient {

    private final ObjectMapper objectMapper;
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);

    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;
    private McpServerConfig config;
    private volatile boolean connected = false;
    private JsonNode serverCapabilities;

    public StdioMcpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void connect(McpServerConfig config) throws McpConnectionException {
        if (connected) {
            throw new McpConnectionException("Already connected to: " + getServerName());
        }

        this.config = config;
        log.info("Connecting to MCP Server: {}", config.getName());

        try {
            // 1. 启动 MCP Server 进程
            startProcess();

            // 2. 发送 initialize 请求
            JsonNode initResult = sendRequest("initialize", buildInitializeParams());
            serverCapabilities = initResult.path("capabilities");
            log.info("MCP Server capabilities: {}", serverCapabilities);

            // 3. 发送 initialized 通知
            sendNotification("initialized", Map.of());

            connected = true;
            log.info("Connected to MCP Server: {}", config.getName());

        } catch (Exception e) {
            closeProcess();
            throw new McpConnectionException("Failed to connect to MCP Server: " + config.getName(), e);
        }
    }

    @Override
    public boolean isConnected() {
        return connected && process != null && process.isAlive();
    }

    @Override
    public List<McpTool> listTools() throws McpConnectionException {
        if (!isConnected()) {
            throw new McpConnectionException("Not connected to MCP Server");
        }

        try {
            JsonNode result = sendRequest("tools/list", Map.of());
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

            JsonNode result = sendRequest("tools/call", params);

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
        if (!connected) {
            return;
        }

        log.info("Disconnecting from MCP Server: {}", config.getName());

        try {
            // 发送 shutdown 通知
            if (isConnected()) {
                sendNotification("shutdown", Map.of());
            }
        } catch (Exception e) {
            log.warn("Error sending shutdown notification: {}", e.getMessage());
        }

        closeProcess();
        connected = false;
        log.info("Disconnected from MCP Server: {}", config.getName());
    }

    @Override
    public String getServerName() {
        return config != null ? config.getName() : null;
    }

    /**
     * 启动 MCP Server 进程
     */
    private void startProcess() throws IOException {
        List<String> command = new ArrayList<>();
        command.add(config.getCommand());
        if (config.getArgs() != null) {
            command.addAll(config.getArgs());
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        // 设置环境变量
        if (config.getEnv() != null) {
            Map<String, String> env = pb.environment();
            env.putAll(config.getEnv());
        }

        process = pb.start();
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        log.info("Started MCP Server process: {}", command);

        // 启动错误流监控线程
        Thread errorThread = new Thread(() -> {
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    log.warn("MCP Server stderr: {}", line);
                }
            } catch (IOException e) {
                log.warn("Error reading MCP Server stderr: {}", e.getMessage());
            }
        });
        errorThread.setDaemon(true);
        errorThread.start();
    }

    /**
     * 关闭 MCP Server 进程
     */
    private void closeProcess() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            log.warn("Error closing streams: {}", e.getMessage());
        }

        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
            }
        }

        process = null;
        reader = null;
        writer = null;
    }

    /**
     * 发送 JSON-RPC 请求并等待响应
     */
    private JsonNode sendRequest(String method, Map<String, Object> params) throws IOException {
        int requestId = requestIdCounter.getAndIncrement();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", requestId);
        request.put("method", method);
        request.put("params", params);

        String requestJson = objectMapper.writeValueAsString(request);
        log.debug("Sending request: {}", requestJson);

        writer.write(requestJson);
        writer.newLine();
        writer.flush();

        // 等待响应
        String responseLine = reader.readLine();
        if (responseLine == null) {
            throw new McpConnectionException("No response from MCP Server");
        }

        log.debug("Received response: {}", responseLine);

        JsonNode response = objectMapper.readTree(responseLine);

        // 检查错误
        if (response.has("error")) {
            JsonNode error = response.get("error");
            String errorMsg = error.path("message").asText();
            throw new McpConnectionException("MCP Server error: " + errorMsg);
        }

        return response.get("result");
    }

    /**
     * 发送 JSON-RPC 通知（无需响应）
     */
    private void sendNotification(String method, Map<String, Object> params) throws IOException {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);
        notification.put("params", params);

        String notificationJson = objectMapper.writeValueAsString(notification);
        log.debug("Sending notification: {}", notificationJson);

        writer.write(notificationJson);
        writer.newLine();
        writer.flush();
    }

    /**
     * 构建 initialize 请求参数
     */
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

    /**
     * 解析工具定义
     */
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

    /**
     * 提取错误消息
     */
    private String extractErrorMessage(JsonNode contentNode) {
        if (contentNode.isArray()) {
            for (JsonNode item : contentNode) {
                if (item.path("type").asText().equals("text")) {
                    return item.path("text").asText();
                }
            }
        }
        return "Unknown error";
    }

    /**
     * 提取内容
     */
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