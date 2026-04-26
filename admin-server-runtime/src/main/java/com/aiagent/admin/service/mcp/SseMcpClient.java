package com.aiagent.admin.service.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP Client SSE Transport 实现
 * <p>
 * 通过 SSE (Server-Sent Events) 与 MCP Server 通信。
 * </p>
 * <p>
 * SSE Transport 协议：
 * <ol>
 *   <li>GET /sse 建立 SSE 连接，接收 endpoint 事件</li>
 *   <li>POST 到 message endpoint 发送请求</li>
 *   <li>通过 SSE 连接接收响应</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
public class SseMcpClient implements McpClient {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);

    private McpServerConfig config;
    private volatile boolean connected = false;
    private JsonNode serverCapabilities;
    private String messageEndpoint;

    private final Map<Integer, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private Disposable sseSubscription;

    public SseMcpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Override
    public void connect(McpServerConfig config) throws McpConnectionException {
        if (connected) {
            throw new McpConnectionException("Already connected");
        }

        this.config = config;
        String sseUrl = config.getUrl(); // SSE URL

        log.info("Connecting to MCP Server via SSE: {}", sseUrl);

        try {
            // 建立 SSE 连接
            sseSubscription = webClient.get()
                    .uri(sseUrl)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .subscribe(
                            this::handleSseEvent,
                            error -> log.error("SSE error: {}", error.getMessage()),
                            () -> log.info("SSE stream completed")
                    );

            // 等待 endpoint 事件（最多 10 秒）
            Thread.sleep(2000);

            // 发送 initialize 请求
            JsonNode initResult = sendRequestAndWait("initialize", buildInitializeParams());
            serverCapabilities = initResult.path("capabilities");
            log.info("MCP Server capabilities: {}", serverCapabilities);

            connected = true;
            log.info("Connected to MCP Server: {}", config.getName());

        } catch (Exception e) {
            disconnect();
            throw new McpConnectionException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public List<McpTool> listTools() throws McpConnectionException {
        if (!isConnected()) {
            throw new McpConnectionException("Not connected");
        }

        try {
            JsonNode result = sendRequestAndWait("tools/list", Map.of());
            JsonNode toolsNode = result.path("tools");

            List<McpTool> tools = new ArrayList<>();
            if (toolsNode.isArray()) {
                for (JsonNode toolNode : toolsNode) {
                    tools.add(parseTool(toolNode));
                }
            }
            return tools;
        } catch (Exception e) {
            throw new McpConnectionException("Failed to list tools: " + e.getMessage(), e);
        }
    }

    @Override
    public McpToolResult callTool(String toolName, Map<String, Object> args)
            throws McpConnectionException, McpToolExecutionException {
        if (!isConnected()) {
            throw new McpConnectionException("Not connected");
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("name", toolName);
            params.put("arguments", args != null ? args : Map.of());

            JsonNode result = sendRequestAndWait("tools/call", params);

            boolean isError = result.path("isError").asBoolean(false);
            JsonNode contentNode = result.path("content");

            if (isError) {
                return McpToolResult.error(extractErrorMessage(contentNode));
            }
            return McpToolResult.success(extractContent(contentNode));

        } catch (McpConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new McpToolExecutionException("Tool execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() {
        if (sseSubscription != null) {
            sseSubscription.dispose();
        }
        pendingRequests.clear();
        connected = false;
        log.info("Disconnected from MCP Server");
    }

    @Override
    public String getServerName() {
        return config != null ? config.getName() : null;
    }

    private void handleSseEvent(String event) {
        log.debug("SSE event: {}", event);

        try {
            String jsonContent = event;
            if (event.startsWith("data:")) {
                jsonContent = event.substring(5).trim();
            }

            if (jsonContent.isEmpty()) {
                return;
            }

            // endpoint 事件包含 message URL
            if (jsonContent.startsWith("http") || jsonContent.contains("/message")) {
                messageEndpoint = jsonContent;
                log.info("Received SSE endpoint: {}", messageEndpoint);
                return;
            }

            JsonNode response = objectMapper.readTree(jsonContent);

            if (response.has("id")) {
                int id = response.get("id").asInt();
                CompletableFuture<JsonNode> future = pendingRequests.get(id);
                if (future != null) {
                    if (response.has("error")) {
                        future.completeExceptionally(
                                new McpConnectionException(response.get("error").path("message").asText()));
                    } else {
                        future.complete(response);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse SSE event: {}", e.getMessage());
        }
    }

    private JsonNode sendRequestAndWait(String method, Map<String, Object> params) throws Exception {
        int requestId = requestIdCounter.getAndIncrement();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", requestId);
        request.put("method", method);
        request.put("params", params);

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        String requestJson = objectMapper.writeValueAsString(request);

        // 构建完整的 POST URL
        String postUrl;
        if (messageEndpoint != null) {
            // messageEndpoint 可能是相对路径，需要构建完整 URL
            if (messageEndpoint.startsWith("http://") || messageEndpoint.startsWith("https://")) {
                postUrl = messageEndpoint;
            } else {
                // 从 SSE URL 中提取 base URL
                String sseUrl = config.getUrl();
                int pathStart = sseUrl.indexOf("/", sseUrl.indexOf("//") + 2);
                String baseUrl = pathStart > 0 ? sseUrl.substring(0, pathStart) : sseUrl;
                postUrl = baseUrl + messageEndpoint;
            }
        } else {
            // 如果还没有 messageEndpoint，使用 SSE URL + /message
            postUrl = config.getUrl().replace("/sse", "/message");
        }

        log.debug("POST to {}: {}", postUrl, requestJson);

        webClient.post()
                .uri(postUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        response -> log.debug("POST response: {}", response),
                        future::completeExceptionally
                );

        // 等待响应（最多 30 秒）
        JsonNode response = future.get(30, TimeUnit.SECONDS);
        pendingRequests.remove(requestId);

        return response.get("result");
    }

    private Map<String, Object> buildInitializeParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", Map.of("tools", Map.of()));
        params.put("clientInfo", Map.of("name", "ai-agent-admin", "version", "2.0.0"));
        return params;
    }

    private McpTool parseTool(JsonNode toolNode) {
        return McpTool.builder()
                .name(toolNode.path("name").asText())
                .description(toolNode.path("description").asText(""))
                .inputSchema(objectMapper.convertValue(toolNode.path("inputSchema"), Map.class))
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
                if ("text".equals(item.path("type").asText())) {
                    contents.add(item.path("text").asText());
                }
            }
            return contents.size() == 1 ? contents.get(0) : contents;
        }
        return contentNode.asText();
    }
}