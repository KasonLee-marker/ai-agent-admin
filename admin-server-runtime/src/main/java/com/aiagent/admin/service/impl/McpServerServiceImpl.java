package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.AgentInfoDTO;
import com.aiagent.admin.api.dto.CreateMcpServerRequest;
import com.aiagent.admin.api.dto.McpServerJsonRequest;
import com.aiagent.admin.api.dto.McpServerResponse;
import com.aiagent.admin.domain.entity.AgentTool;
import com.aiagent.admin.domain.entity.McpServer;
import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import com.aiagent.admin.domain.repository.AgentRepository;
import com.aiagent.admin.domain.repository.AgentToolRepository;
import com.aiagent.admin.domain.repository.McpServerRepository;
import com.aiagent.admin.domain.repository.ToolRepository;
import com.aiagent.admin.service.IdGenerator;
import com.aiagent.admin.service.McpServerService;
import com.aiagent.admin.service.event.McpServerRefreshEvent;
import com.aiagent.admin.service.mcp.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

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
    private final AgentToolRepository agentToolRepository;
    private final AgentRepository agentRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final McpClientFactory mcpClientFactory;
    private final McpRuntimeManager mcpRuntimeManager;
    private final ApplicationEventPublisher eventPublisher;

    public McpServerServiceImpl(McpServerRepository mcpServerRepository,
                                ToolRepository toolRepository,
                                AgentToolRepository agentToolRepository,
                                AgentRepository agentRepository,
                                IdGenerator idGenerator,
                                ObjectMapper objectMapper,
                                McpClientFactory mcpClientFactory,
                                McpRuntimeManager mcpRuntimeManager,
                                ApplicationEventPublisher eventPublisher) {
        this.mcpServerRepository = mcpServerRepository;
        this.toolRepository = toolRepository;
        this.agentToolRepository = agentToolRepository;
        this.agentRepository = agentRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.mcpClientFactory = mcpClientFactory;
        this.mcpRuntimeManager = mcpRuntimeManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public List<McpServerResponse> createFromJson(McpServerJsonRequest request) {
        String configJson = request.getConfigJson();

        // 解析 JSON
        Map<String, Object> rootConfig;
        try {
            rootConfig = objectMapper.readValue(configJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 格式不正确: " + e.getMessage());
        }

        // 获取 mcpServers 对象
        Object mcpServersObj = rootConfig.get("mcpServers");
        if (!(mcpServersObj instanceof Map)) {
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

            // 创建 MCP Server，传入 request 中的 description（优先级高于 JSON 中的 description）
            McpServer server = createMcpServerFromConfig(serverName, serverConfig, transportType, request.getDescription());
            log.info("Created MCP Server '{}' (transport: {}, description: {})", serverName, transportType,
                    request.getDescription() != null ? "from request" : "from JSON/config");
            
            // 对于 stdio + DOCKER 模式，异步刷新工具列表（避免 npx 安装超时）
            if ("stdio".equals(transportType) && "DOCKER".equals(server.getRuntimeMode())) {
                final String serverId = server.getId();
                log.info("Scheduling async tool refresh event for MCP Server: {}", serverId);
                log.info("Transaction active: {}", TransactionSynchronizationManager.isActualTransactionActive());
                
                // 在事务提交后发布事件
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            log.info("Publishing async tool refresh event for MCP Server after commit: {}", serverId);
                            eventPublisher.publishEvent(new McpServerRefreshEvent(serverId));
                        }
                    });
                } else {
                    // 如果没有事务，直接发布事件
                    log.warn("No active transaction, publishing event immediately for MCP Server: {}", serverId);
                    eventPublisher.publishEvent(new McpServerRefreshEvent(serverId));
                }
            } else {
                // 对于其他模式，同步刷新工具列表
                refreshTools(server.getId());
            }
            
            // 重新加载 server 以获取更新后的状态
            final String serverId = server.getId();
            McpServer updatedServer = mcpServerRepository.findById(serverId)
                    .orElseThrow(() -> new IllegalArgumentException("MCP Server not found after refresh: " + serverId));
            
            created.add(toResponseWithToolCount(updatedServer));
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
            rootConfig = objectMapper.readValue(configJson, new TypeReference<>() {
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

        // 更新 Server，传入 request 中的 description（优先级高于 JSON 中的 description）
        updateMcpServerFromConfig(existingServer, newName, serverConfig, transportType, request.getDescription());

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
    private McpServer createMcpServerFromConfig(String name, Map<String, Object> config, String transportType, String description) {
        // 检查名称唯一性
        if (mcpServerRepository.existsByName(name)) {
            throw new IllegalArgumentException("MCP Server name already exists: " + name);
        }

        String url = transportType.equals("sse") ? (String) config.get("url") : null;
        String command = transportType.equals("stdio") ? (String) config.get("command") : null;
        List<String> args = parseArgsList(config.get("args"));
        Map<String, String> env = parseEnvMap(config.get("env"));

        // 获取 runtimeMode，默认 DOCKER
        String runtimeMode = (String) config.get("runtimeMode");
        if (runtimeMode == null || runtimeMode.isEmpty()) {
            runtimeMode = "DOCKER";  // 默认使用 Docker 模式
        }

        // 优先级：传入的 description > JSON 中的 description
        String finalDescription = description != null ? description : (String) config.get("description");

        McpServer server = McpServer.builder()
                .id(idGenerator.generateId())
                .name(name)
                .description(finalDescription)
                .transportType(transportType)
                .runtimeMode(runtimeMode)
                .url(url)
                .command(command)
                .args(toJson(args))
                .env(toJson(env))
                .status("ACTIVE")
                .processStatus(transportType.equals("stdio") ? "STOPPED" : null)
                .build();

        return mcpServerRepository.save(server);
    }

    /**
     * 从配置更新 MCP Server 实体
     */
    private void updateMcpServerFromConfig(McpServer server, String newName,
                                           Map<String, Object> config, String transportType, String description) {
        server.setName(newName);
        // 优先级：传入的 description > JSON 中的 description
        String finalDescription = description != null ? description : (String) config.get("description");
        server.setDescription(finalDescription);
        server.setTransportType(transportType);

        // 更新 runtimeMode（如果配置中有）
        String runtimeMode = (String) config.get("runtimeMode");
        if (runtimeMode != null && !runtimeMode.isEmpty()) {
            server.setRuntimeMode(runtimeMode);
        }

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
        Optional<McpServer> serverOpt = mcpServerRepository.findById(id);
        if (serverOpt.isEmpty()) {
            return Optional.empty();
        }
        
        McpServer server = serverOpt.get();
        
        // 对于 stdio + DOCKER 模式，同步进程状态
        if ("stdio".equals(server.getTransportType()) && "DOCKER".equals(server.getRuntimeMode())) {
            try {
                syncProcessStatus(server);
            } catch (Exception e) {
                log.warn("Failed to sync process status for MCP Server {}: {}", id, e.getMessage());
            }
        }
        
        return Optional.of(toResponseWithToolCount(server));
    }
    
    /**
     * 同步 MCP Server 进程状态
     */
    private void syncProcessStatus(McpServer server) {
        McpRuntimeManager.ProcessStatus status = mcpRuntimeManager.getProcessStatus(server.getName());
        if (status == null) {
            return;
        }
        
        String installStatus = status.getInstallStatus();
        String processStatus = status.getStatus();
        
        // 根据 install_status 判断进程状态
        if ("installing".equals(installStatus)) {
            processStatus = "INSTALLING";
        } else if ("ready".equals(installStatus)) {
            processStatus = "RUNNING";
        } else if ("error".equals(installStatus)) {
            processStatus = "ERROR";
        } else if ("unknown".equals(status.getStatus()) || "not_found".equals(status.getStatus())) {
            // 进程不存在或未知状态
            processStatus = "STOPPED";
        } else if ("running".equals(status.getStatus()) && installStatus == null) {
            // 兼容旧版本
            processStatus = "RUNNING";
        }
        
        // 更新数据库状态（不保存，因为方法是 readOnly）
        server.setProcessStatus(processStatus);
        server.setProcessId(status.getPid());
    }

    @Override
    @Transactional
    public void delete(String id) {
        McpServer server = mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + id));

        // 删除关联的 MCP 工具及其 Agent 绑定
        List<Tool> mcpTools = toolRepository.findByType(ToolType.MCP);
        for (Tool tool : mcpTools) {
            Map<String, Object> config = parseConfig(tool.getConfig());
            if (id.equals(config.get("mcpServerId"))) {
                // 先删除该工具的 Agent 绑定
                List<AgentTool> agentTools = agentToolRepository.findByToolId(tool.getId());
                for (AgentTool agentTool : agentTools) {
                    agentToolRepository.delete(agentTool);
                    log.info("Deleted AgentTool binding: agentId={}, toolId={}", agentTool.getAgentId(), tool.getId());
                }
                // 再删除工具
                toolRepository.delete(tool);
                log.info("Deleted MCP tool: {}", tool.getId());
            }
        }

        mcpServerRepository.delete(server);
        log.info("Deleted MCP Server: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentInfoDTO> getReferencingAgents(String id) {
        // 获取该 MCP Server 下的所有工具 ID
        List<Tool> mcpTools = toolRepository.findByType(ToolType.MCP);
        List<String> toolIds = mcpTools.stream()
                .filter(tool -> {
                    Map<String, Object> config = parseConfig(tool.getConfig());
                    return id.equals(config.get("mcpServerId"));
                })
                .map(Tool::getId)
                .collect(Collectors.toList());

        if (toolIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取引用这些工具的 Agent
        Set<String> agentIds = new HashSet<>();
        for (String toolId : toolIds) {
            List<AgentTool> agentTools = agentToolRepository.findByToolId(toolId);
            agentTools.forEach(at -> agentIds.add(at.getAgentId()));
        }

        // 查询 Agent 信息
        List<AgentInfoDTO> result = new ArrayList<>();
        for (String agentId : agentIds) {
            agentRepository.findById(agentId).ifPresent(agent -> {
                result.add(AgentInfoDTO.builder()
                        .id(agent.getId())
                        .name(agent.getName())
                        .status(agent.getStatus() != null ? agent.getStatus().name() : null)
                        .build());
            });
        }

        return result;
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
        String transportType = buildTransport(request, server);

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

    /**
     * 获取mcpServer transport类型
     *
     * @param request 创建mcpServer请求
     * @param server  McpServer对象
     * @return 返回协议类型
     */
    private String buildTransport(CreateMcpServerRequest request, McpServer server) {
        String transportType = request.getTransportType() != null ? request.getTransportType() : server.getTransportType();
        if ("stdio".equals(transportType) && request.getCommand() == null && server.getCommand() == null) {
            throw new IllegalArgumentException("Command is required for stdio transport");
        }
        if ("sse".equals(transportType) && request.getUrl() == null && server.getUrl() == null) {
            throw new IllegalArgumentException("URL is required for SSE transport");
        }
        return transportType;
    }

    @Override
    @Transactional
    public List<McpTool> refreshTools(String id) {
        McpServer server = mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + id));

        McpServerConfig config = toMcpServerConfig(server);
        McpClient client = getClient(server);

        try {
            // 连接 MCP Server
            client.connect(config);

            // 获取工具列表
            List<McpTool> tools = client.listTools();

            // 注册工具到 Tool 表
            for (McpTool mcpTool : tools) {
                registerMcpTool(mcpTool, server);
            }

            // 对于 Docker 模式，只关闭连接而不停止进程
            // 进程由 MCP Runtime 管理，保持运行状态
            if ("stdio".equals(server.getTransportType()) && "DOCKER".equals(server.getRuntimeMode())) {
                // 只发送 shutdown 通知，不停止进程
                try {
                    if (client instanceof DockerMcpClient) {
                        ((DockerMcpClient) client).closeConnection();
                    } else {
                        client.disconnect();
                    }
                } catch (Exception e) {
                    log.warn("Error closing connection: {}", e.getMessage());
                }
            } else {
                // 断开连接（会停止进程）
                client.disconnect();
            }

            // 更新进程状态（Docker 模式下）
            if ("stdio".equals(server.getTransportType()) && "DOCKER".equals(server.getRuntimeMode())) {
                updateProcessStatus(server);
            }

            log.info("Refreshed {} tools from MCP Server: {}", tools.size(), id);
            return tools;

        } catch (McpConnectionException e) {
            log.error("Failed to connect to MCP Server {}: {}", id, e.getMessage(), e);
            // 更新错误状态和日志
            if ("stdio".equals(server.getTransportType()) && "DOCKER".equals(server.getRuntimeMode())) {
                server.setProcessStatus("ERROR");
                server.setLastLog(e.getMessage());
                mcpServerRepository.save(server);
            }
            // 直接抛出原始异常，保留完整的错误信息
            throw e;
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
     * 根据 transport 类型和 runtimeMode 获取对应的 Client
     * <p>
     * 使用 McpClientFactory 根据配置创建合适的 Client 实例。
     * </p>
     */
    private McpClient getClient(McpServer server) {
        return mcpClientFactory.getClient(toMcpServerConfig(server));
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
                .transportType(server.getTransportType())
                .runtimeMode(server.getRuntimeMode());

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
        McpServerResponse response = McpServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .transportType(server.getTransportType())
                .runtimeMode(server.getRuntimeMode())
                .command(server.getCommand())
                .url(server.getUrl())
                .args(parseArgs(server.getArgs()))
                .env(parseEnv(server.getEnv()))
                .status(server.getStatus())
                .processStatus(server.getProcessStatus())
                .containerId(server.getContainerId())
                .processId(server.getProcessId())
                .lastLog(server.getLastLog())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
        return response;
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

    /**
     * 更新进程状态（DOCKER 模式）
     */
    private void updateProcessStatus(McpServer server) {
        if (!"DOCKER".equals(server.getRuntimeMode())) {
            log.debug("Server {} is not DOCKER mode, skipping process status update", server.getName());
            return;
        }

        try {
            // 直接从 MCP Runtime HTTP API 获取进程状态
            String statusUrl = String.format("%s/servers/%s/status",
                    mcpRuntimeManager.getMcpHostBaseUrl(), server.getName());
            
            log.info("Updating process status for {} from URL: {}", server.getName(), statusUrl);
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(statusUrl, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> status = response.getBody();
                String rawStatus = (String) status.get("status");
                String installStatus = (String) status.get("install_status");  // 注意：JSON 字段名是 install_status
                Object pidObj = status.get("pid");
                Integer processId = null;
                if (pidObj instanceof Integer) {
                    processId = (Integer) pidObj;
                } else if (pidObj instanceof Long) {
                    processId = ((Long) pidObj).intValue();
                } else if (pidObj instanceof Number) {
                    processId = ((Number) pidObj).intValue();
                }
                
                // 调试日志
                log.info("Raw status from MCP Runtime: status={}, install_status={}, pid={}", 
                        rawStatus, installStatus, pidObj);
                
                // 根据状态映射到标准状态
                String processStatus;
                if (("running".equals(rawStatus) || "already_running".equals(rawStatus)) && "ready".equals(installStatus)) {
                    processStatus = "RUNNING";
                } else if (("running".equals(rawStatus) || "already_running".equals(rawStatus)) && "installing".equals(installStatus)) {
                    processStatus = "INSTALLING";
                } else if ("not_found".equals(rawStatus) || "stopped".equals(rawStatus) || "unknown".equals(rawStatus)) {
                    processStatus = "STOPPED";
                } else if ("error".equals(installStatus)) {
                    processStatus = "ERROR";
                } else if ("running".equals(rawStatus) || "already_running".equals(rawStatus)) {
                    // 如果正在运行但 install_status 不是 ready，也认为是 RUNNING
                    processStatus = "RUNNING";
                } else {
                    processStatus = rawStatus != null ? rawStatus.toUpperCase() : "UNKNOWN";
                }
                
                log.info("Mapped process status: {} -> {}", rawStatus, processStatus);
                
                server.setProcessStatus(processStatus);
                server.setProcessId(processId);
                mcpServerRepository.save(server);
                
                log.info("Updated process status for {}: status={}, pid={}", 
                        server.getName(), processStatus, processId);
            } else {
                log.warn("Failed to get process status for {}: HTTP {}", 
                        server.getName(), response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Failed to update process status for {}: {}", server.getName(), e.getMessage());
            // 不抛出异常，避免影响主流程
        }
    }

    @Override
    public String getProcessLogs(String id, int lines) {
        McpServer server = mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + id));

        if (!"stdio".equals(server.getTransportType()) || !"DOCKER".equals(server.getRuntimeMode())) {
            return "Not available for this server type";
        }

        return mcpRuntimeManager.getProcessLogs(server.getName(), lines);
    }

    @Override
    public boolean restartProcess(String id) {
        McpServer server = mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + id));

        if (!"stdio".equals(server.getTransportType()) || !"DOCKER".equals(server.getRuntimeMode())) {
            return false;
        }

        // 停止旧进程
        mcpRuntimeManager.stopServerProcess(server.getName());

        // 重新启动（通过 refreshTools 或直接启动）
        try {
            McpServerConfig config = toMcpServerConfig(server);
            boolean started = mcpRuntimeManager.startServerProcess(server);

            if (started) {
                server.setProcessStatus("RUNNING");
                mcpServerRepository.save(server);
            }

            return started;
        } catch (Exception e) {
            log.error("Failed to restart process for server {}: {}", id, e.getMessage());
            server.setProcessStatus("ERROR");
            server.setLastLog(e.getMessage());
            mcpServerRepository.save(server);
            return false;
        }
    }

    @Override
    public ProcessStatusDTO getProcessStatus(String id) {
        McpServer server = mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + id));

        if (!"stdio".equals(server.getTransportType()) || !"DOCKER".equals(server.getRuntimeMode())) {
            return ProcessStatusDTO.builder()
                    .serverId(id)
                    .status("NOT_SUPPORTED")
                    .build();
        }

        McpRuntimeManager.ProcessStatus status = mcpRuntimeManager.getProcessStatus(server.getName());

        // 同步数据库状态
        String installStatus = status.getInstallStatus();
        String processStatus = status.getStatus();
        
        // 根据 install_status 判断进程状态
        if ("installing".equals(installStatus)) {
            processStatus = "INSTALLING";
        } else if ("ready".equals(installStatus)) {
            processStatus = "RUNNING";
        } else if ("error".equals(installStatus)) {
            processStatus = "ERROR";
        } else if ("unknown".equals(status.getStatus()) || "not_found".equals(status.getStatus())) {
            // 进程不存在或未知状态
            processStatus = "STOPPED";
        } else if ("running".equals(status.getStatus()) && installStatus == null) {
            // 兼容旧版本
            processStatus = "RUNNING";
        }
        
        if (!processStatus.equals(server.getProcessStatus())) {
            server.setProcessStatus(processStatus);
            server.setProcessId(status.getPid());
            mcpServerRepository.save(server);
        }

        return ProcessStatusDTO.builder()
                .serverId(id)
                .status(processStatus)
                .pid(status.getPid())
                .uptimeSeconds(status.getUptimeSeconds())
                .build();
    }
}