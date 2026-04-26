package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.CreateMcpServerRequest;
import com.aiagent.admin.api.dto.McpServerJsonRequest;
import com.aiagent.admin.api.dto.McpServerResponse;
import com.aiagent.admin.domain.entity.McpServer;
import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import com.aiagent.admin.domain.repository.McpServerRepository;
import com.aiagent.admin.domain.repository.ToolRepository;
import com.aiagent.admin.service.IdGenerator;
import com.aiagent.admin.service.McpServerService;
import com.aiagent.admin.service.mcp.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Server 服务实现类
 * <p>
 * 管理 MCP Server 配置，支持 stdio 和 SSE transport。
 * 支持标准 MCP 配置 JSON 格式，自动识别 transport 类型。
 * </p>
 *
 * @see McpServerService
 * @see McpClient
 */
@Slf4j
@Service
public class McpServerServiceImpl implements McpServerService {

    private final McpServerRepository mcpServerRepository;
    private final ToolRepository toolRepository;
    private final StdioMcpClient stdioMcpClient;
    private final SseMcpClient sseMcpClient;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public McpServerServiceImpl(McpServerRepository mcpServerRepository,
                                ToolRepository toolRepository,
                                StdioMcpClient stdioMcpClient,
                                SseMcpClient sseMcpClient,
                                IdGenerator idGenerator,
                                ObjectMapper objectMapper) {
        this.mcpServerRepository = mcpServerRepository;
        this.toolRepository = toolRepository;
        this.stdioMcpClient = stdioMcpClient;
        this.sseMcpClient = sseMcpClient;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public List<McpServerResponse> createFromJson(McpServerJsonRequest request) {
        String configJson = request.getConfigJson();

        // 解析 JSON
        Map<String, Object> rootConfig;
        try {
            rootConfig = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 格式不正确: " + e.getMessage());
        }

        // 获取 mcpServers 对象
        Object mcpServersObj = rootConfig.get("mcpServers");
        if (mcpServersObj == null || !(mcpServersObj instanceof Map)) {
            throw new IllegalArgumentException("配置中缺少 'mcpServers' 字段或格式不正确");
        }

        Map<String, Object> mcpServers = (Map<String, Object>) mcpServersObj;
        List<McpServerResponse> created = new ArrayList<>();

        // 遍历每个 Server 配置
        for (Map.Entry<String, Object> entry : mcpServers.entrySet()) {
            String serverName = entry.getKey();
            Object serverConfigObj = entry.getValue();

            if (!(serverConfigObj instanceof Map)) {
                log.warn("Server '{}' config is not a valid object, skipping", serverName);
                continue;
            }

            Map<String, Object> serverConfig = (Map<String, Object>) serverConfigObj;

            // 自动识别 transport 类型
            String transportType = determineTransportType(serverConfig);

            // 创建 MCP Server
            McpServer server = createMcpServerFromConfig(serverName, serverConfig, transportType);
            created.add(toResponseWithToolCount(server));
            log.info("Created MCP Server '{}' (transport: {})", serverName, transportType);
        }

        return created;
    }

    @Override
    @Transactional
    public McpServerResponse updateFromJson(String id, McpServerJsonRequest request) {
        McpServer existingServer = mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + id));

        String configJson = request.getConfigJson();

        // 解析 JSON
        Map<String, Object> rootConfig;
        try {
            rootConfig = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 格式不正确: " + e.getMessage());
        }

        Object mcpServersObj = rootConfig.get("mcpServers");
        if (mcpServersObj == null || !(mcpServersObj instanceof Map)) {
            throw new IllegalArgumentException("配置中缺少 'mcpServers' 字段或格式不正确");
        }

        Map<String, Object> mcpServers = (Map<String, Object>) mcpServersObj;

        // 取第一个 Server 配置用于更新
        if (mcpServers.isEmpty()) {
            throw new IllegalArgumentException("配置中没有 MCP Server");
        }

        Map.Entry<String, Object> firstEntry = mcpServers.entrySet().iterator().next();
        String newName = firstEntry.getKey();
        Map<String, Object> serverConfig = (Map<String, Object>) firstEntry.getValue();

        // 检查新名称是否与其他 Server 冲突
        if (!existingServer.getName().equals(newName) && mcpServerRepository.existsByName(newName)) {
            throw new IllegalArgumentException("MCP Server name already exists: " + newName);
        }

        // 自动识别 transport 类型
        String transportType = determineTransportType(serverConfig);

        // 更新 Server
        updateMcpServerFromConfig(existingServer, newName, serverConfig, transportType);

        return toResponseWithToolCount(existingServer);
    }

    /**
     * 根据 Server 配置自动判断 transport 类型
     * <p>
     * 有 url → SSE
     * 有 command → Stdio
     * </p>
     */
    private String determineTransportType(Map<String, Object> serverConfig) {
        if (serverConfig.containsKey("url")) {
            return "sse";
        }
        if (serverConfig.containsKey("command")) {
            return "stdio";
        }
        throw new IllegalArgumentException("Server 配置必须包含 'url' (SSE) 或 'command' (Stdio)");
    }

    /**
     * 从配置创建 MCP Server 实体
     */
    private McpServer createMcpServerFromConfig(String name, Map<String, Object> config, String transportType) {
        // 检查名称唯一性
        if (mcpServerRepository.existsByName(name)) {
            throw new IllegalArgumentException("MCP Server name already exists: " + name);
        }

        String url = transportType.equals("sse") ? (String) config.get("url") : null;
        String command = transportType.equals("stdio") ? (String) config.get("command") : null;
        List<String> args = parseArgsList(config.get("args"));
        Map<String, String> env = parseEnvMap(config.get("env"));

        McpServer server = McpServer.builder()
                .id(idGenerator.generateId())
                .name(name)
                .description((String) config.get("description"))
                .transportType(transportType)
                .url(url)
                .command(command)
                .args(toJson(args))
                .env(toJson(env))
                .status("ACTIVE")
                .build();

        return mcpServerRepository.save(server);
    }

    /**
     * 从配置更新 MCP Server 实体
     */
    private void updateMcpServerFromConfig(McpServer server, String newName,
                                           Map<String, Object> config, String transportType) {
        server.setName(newName);
        server.setDescription((String) config.get("description"));
        server.setTransportType(transportType);

        if (transportType.equals("sse")) {
            server.setUrl((String) config.get("url"));
            server.setCommand(null);
            server.setArgs("[]");
        } else {
            server.setUrl(null);
            server.setCommand((String) config.get("command"));
            server.setArgs(toJson(parseArgsList(config.get("args"))));
        }

        server.setEnv(toJson(parseEnvMap(config.get("env"))));
        mcpServerRepository.save(server);
    }

    /**
     * 解析 args 为 List（支持字符串数组）
     */
    private List<String> parseArgsList(Object argsObj) {
        if (argsObj == null) return Collections.emptyList();
        if (argsObj instanceof List) {
            return ((List<?>) argsObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 解析 env 为 Map
     */
    private Map<String, String> parseEnvMap(Object envObj) {
        if (envObj == null) return Collections.emptyMap();
        if (envObj instanceof Map) {
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) envObj).entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }
            return result;
        }
        return Collections.emptyMap();
    }

    @Override
    @Transactional
    public McpServerResponse create(CreateMcpServerRequest request) {
        // 检查名称唯一性
        if (mcpServerRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("MCP Server name already exists: " + request.getName());
        }

        // 验证 transport 配置
        String transportType = request.getTransportType() != null ? request.getTransportType() : "stdio";
        if ("stdio".equals(transportType) && request.getCommand() == null) {
            throw new IllegalArgumentException("Command is required for stdio transport");
        }
        if ("sse".equals(transportType) && request.getUrl() == null) {
            throw new IllegalArgumentException("URL is required for SSE transport");
        }

        McpServer server = McpServer.builder()
                .id(idGenerator.generateId())
                .name(request.getName())
                .description(request.getDescription())
                .transportType(transportType)
                .command(request.getCommand())
                .url(request.getUrl())
                .args(toJson(request.getArgs()))
                .env(toJson(request.getEnv()))
                .status("ACTIVE")
                .build();

        server = mcpServerRepository.save(server);
        log.info("Created MCP Server: {} (transport: {})", server.getId(), transportType);

        return toResponse(server);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpServerResponse> listAll() {
        return mcpServerRepository.findAll().stream()
                .map(this::toResponseWithToolCount)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpServerResponse> getById(String id) {
        return mcpServerRepository.findById(id)
                .map(this::toResponseWithToolCount);
    }

    @Override
    @Transactional
    public void delete(String id) {
        McpServer server = mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + id));

        // 删除关联的 MCP 工具
        List<Tool> mcpTools = toolRepository.findByType(ToolType.MCP);
        for (Tool tool : mcpTools) {
            Map<String, Object> config = parseConfig(tool.getConfig());
            if (id.equals(config.get("mcpServerId"))) {
                toolRepository.delete(tool);
                log.info("Deleted MCP tool: {}", tool.getId());
            }
        }

        mcpServerRepository.delete(server);
        log.info("Deleted MCP Server: {}", id);
    }

    @Override
    @Transactional
    public McpServerResponse update(String id, CreateMcpServerRequest request) {
        McpServer server = mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + id));

        // 检查名称唯一性（如果修改了名称）
        if (!server.getName().equals(request.getName()) &&
                mcpServerRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("MCP Server name already exists: " + request.getName());
        }

        // 验证 transport 配置
        String transportType = request.getTransportType() != null ? request.getTransportType() : server.getTransportType();
        if ("stdio".equals(transportType) && request.getCommand() == null && server.getCommand() == null) {
            throw new IllegalArgumentException("Command is required for stdio transport");
        }
        if ("sse".equals(transportType) && request.getUrl() == null && server.getUrl() == null) {
            throw new IllegalArgumentException("URL is required for SSE transport");
        }

        // 更新字段
        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setTransportType(transportType);
        server.setCommand(request.getCommand());
        server.setUrl(request.getUrl());
        server.setArgs(toJson(request.getArgs()));
        server.setEnv(toJson(request.getEnv()));

        server = mcpServerRepository.save(server);
        log.info("Updated MCP Server: {}", id);

        return toResponseWithToolCount(server);
    }

    @Override
    @Transactional
    public List<McpTool> refreshTools(String id) {
        McpServer server = mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + id));

        McpServerConfig config = toMcpServerConfig(server);
        McpClient client = getClient(server.getTransportType());

        try {
            // 连接 MCP Server
            client.connect(config);

            // 获取工具列表
            List<McpTool> tools = client.listTools();

            // 注册工具到 Tool 表
            for (McpTool mcpTool : tools) {
                registerMcpTool(mcpTool, server);
            }

            // 断开连接
            client.disconnect();

            log.info("Refreshed {} tools from MCP Server: {}", tools.size(), id);
            return tools;

        } catch (McpConnectionException e) {
            log.error("Failed to connect to MCP Server {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to connect to MCP Server: " + e.getMessage(), e);
        } finally {
            if (client.isConnected()) {
                client.disconnect();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpServer> findEntityById(String id) {
        return mcpServerRepository.findById(id);
    }

    /**
     * 根据 transport 类型获取对应的 Client
     */
    private McpClient getClient(String transportType) {
        if ("sse".equals(transportType)) {
            return sseMcpClient;
        }
        return stdioMcpClient;
    }

    /**
     * 注册 MCP 工具到 Tool 表
     */
    private void registerMcpTool(McpTool mcpTool, McpServer server) {
        Optional<Tool> existingTool = toolRepository.findByName(mcpTool.getName());
        if (existingTool.isPresent()) {
            Tool tool = existingTool.get();
            tool.setDescription(mcpTool.getDescription());
            tool.setSchema(toJson(mcpTool.getInputSchema()));
            tool.setConfig(buildMcpToolConfig(server, mcpTool));
            toolRepository.save(tool);
            log.info("Updated MCP tool: {}", mcpTool.getName());
        } else {
            Tool tool = Tool.builder()
                    .id(idGenerator.generateId())
                    .name(mcpTool.getName())
                    .description(mcpTool.getDescription())
                    .type(ToolType.MCP)
                    .category(determineCategory(mcpTool.getName()))
                    .schema(toJson(mcpTool.getInputSchema()))
                    .executor("mcpToolExecutor")
                    .config(buildMcpToolConfig(server, mcpTool))
                    .build();
            toolRepository.save(tool);
            log.info("Created MCP tool: {}", mcpTool.getName());
        }
    }

    /**
     * 构建 MCP 工具配置
     */
    private String buildMcpToolConfig(McpServer server, McpTool mcpTool) {
        Map<String, Object> config = new HashMap<>();
        config.put("mcpServerId", server.getId());
        config.put("mcpServerName", server.getName());
        config.put("mcpToolName", mcpTool.getName());
        config.put("transportType", server.getTransportType());
        if ("sse".equals(server.getTransportType())) {
            config.put("url", server.getUrl());
        } else {
            config.put("command", server.getCommand());
            config.put("args", parseArgs(server.getArgs()));
            config.put("env", parseEnv(server.getEnv()));
        }
        return toJson(config);
    }

    /**
     * 根据工具名称推断类别
     */
    private ToolCategory determineCategory(String toolName) {
        String lowerName = toolName.toLowerCase();
        if (lowerName.contains("search") || lowerName.contains("query") || lowerName.contains("find")) {
            return ToolCategory.SEARCH;
        }
        if (lowerName.contains("fetch") || lowerName.contains("http") || lowerName.contains("api")) {
            return ToolCategory.HTTP;
        }
        return ToolCategory.GENERAL;
    }

    /**
     * 转换为 MCP Server Config
     */
    private McpServerConfig toMcpServerConfig(McpServer server) {
        McpServerConfig.McpServerConfigBuilder builder = McpServerConfig.builder()
                .name(server.getName())
                .transportType(server.getTransportType());

        if ("sse".equals(server.getTransportType())) {
            builder.url(server.getUrl());
        } else {
            builder.command(server.getCommand())
                    .args(parseArgs(server.getArgs()))
                    .env(parseEnv(server.getEnv()));
        }

        return builder.build();
    }

    /**
     * 转换为响应 DTO
     */
    private McpServerResponse toResponse(McpServer server) {
        return McpServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .transportType(server.getTransportType())
                .command(server.getCommand())
                .url(server.getUrl())
                .args(parseArgs(server.getArgs()))
                .env(parseEnv(server.getEnv()))
                .status(server.getStatus())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }

    /**
     * 转换为响应 DTO（含工具数量）
     */
    private McpServerResponse toResponseWithToolCount(McpServer server) {
        McpServerResponse response = toResponse(server);
        List<Tool> mcpTools = toolRepository.findByType(ToolType.MCP);
        int count = 0;
        for (Tool tool : mcpTools) {
            Map<String, Object> config = parseConfig(tool.getConfig());
            if (server.getId().equals(config.get("mcpServerId"))) {
                count++;
            }
        }
        response.setToolCount(count);
        return response;
    }

    private String toJson(Object obj) {
        if (obj == null) return "{}";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private List<String> parseArgs(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private Map<String, String> parseEnv(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }
}