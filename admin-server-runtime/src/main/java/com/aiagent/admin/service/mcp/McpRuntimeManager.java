package com.aiagent.admin.service.mcp;

import com.aiagent.admin.domain.entity.McpServer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * MCP Runtime 管理器
 * <p>
 * 负责管理 MCP Runtime Docker 容器的生命周期，以及通过 HTTP API
 * 与容器内的 MCP Host Service 通信。
 * </p>
 * <p>
 * 架构：
 * <ul>
 *   <li>单个 Docker 容器运行多个 MCP Server 进程</li>
 *   <li>容器内 MCP Host Service 提供 HTTP API</li>
 *   <li>本类负责容器管理和 HTTP 调用转发</li>
 * </ul>
 * </p>
 *
 * @see DockerMcpClient
 * @see McpServer
 */
@Slf4j
@Service
public class McpRuntimeManager {

    /**
     * Docker 镜像名称
     */
    private static final String MCP_RUNTIME_IMAGE = "ai-agent-admin/mcp-runtime:latest";

    /**
     * 容器名称
     */
    private static final String CONTAINER_NAME = "mcp-runtime";

    /**
     * MCP Host 服务端口
     */
    private static final int MCP_HOST_PORT = 8080;

    /**
     * Docker 主机地址
     * <p>Windows 默认使用 npipe:////./pipe/docker_engine</p>
     * <p>Linux/Mac 默认使用 unix:///var/run/docker.sock</p>
     */
    @Value("${mcp.docker.host:#{null}}")
    private String dockerHost;

    /**
     * MCP Host 暴露的端口（映射到宿主机）
     */
    @Value("${mcp.docker.host.port:18080}")
    private int hostPort;

    private DockerClient dockerClient;
    private RestClient restClient;
    private volatile String containerId;
    private volatile boolean initialized = false;

    /**
     * 初始化 Docker 客户端
     */
    @PostConstruct
    public void init() {
        log.info("Initializing MCP Runtime Manager, docker host: {}", dockerHost != null ? dockerHost : "(auto-detect)");

        try {
            DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
            if (dockerHost != null && !dockerHost.isBlank()) {
                configBuilder.withDockerHost(dockerHost);
            }
            DockerClientConfig config = configBuilder.build();

            // 使用 Apache HttpClient 5
            ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();

            this.dockerClient = DockerClientImpl.getInstance(config, httpClient);

            // 测试 Docker 连接
            dockerClient.pingCmd().exec();
            log.info("Docker connection established successfully");

            // 初始化 RestClient
            this.restClient = RestClient.builder()
                    .baseUrl(getMcpHostBaseUrl())
                    .build();

            // 检查/启动容器
            ensureContainerRunning();

            this.initialized = true;
        } catch (Exception e) {
            log.error("Failed to initialize Docker client: {}. MCP Runtime features will be disabled.", e.getMessage());
            log.error("On Windows, please ensure Docker Desktop is running and TCP port 2375 is exposed in settings, " +
                    "or configure mcp.docker.host property.");
            this.dockerClient = null;
            this.initialized = false;
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("MCP Runtime Manager shutting down");
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (IOException e) {
                log.warn("Error closing docker client: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取 MCP Host 服务基础 URL
     */
    public String getMcpHostBaseUrl() {
        return String.format("http://localhost:%d", hostPort);
    }

    /**
     * 确保容器正在运行
     * <p>
     * 检查容器状态，如果不存在则创建，如果停止则启动。
     * </p>
     *
     * @return 容器 ID
     */
    public synchronized String ensureContainerRunning() {
        if (dockerClient == null) {
            log.warn("Docker client is not available. Cannot ensure container running.");
            return null;
        }
        // 检查容器是否存在
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(List.of(CONTAINER_NAME))
                .exec();

        if (containers.isEmpty()) {
            // 创建新容器
            return createAndStartContainer();
        }

        Container container = containers.get(0);
        this.containerId = container.getId();

        // 检查容器状态
        if (!"running".equals(container.getState())) {
            log.info("Starting stopped MCP runtime container: {}", containerId);
            dockerClient.startContainerCmd(containerId).exec();

            // 等待服务就绪
            waitForMcpHostReady();
        }

        return containerId;
    }

    /**
     * 获取当前容器 ID
     */
    public Optional<String> getContainerId() {
        if (dockerClient == null) {
            return Optional.empty();
        }
        if (containerId != null) {
            return Optional.of(containerId);
        }

        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(List.of(CONTAINER_NAME))
                .exec();

        if (!containers.isEmpty()) {
            this.containerId = containers.get(0).getId();
            return Optional.of(containerId);
        }

        return Optional.empty();
    }

    /**
     * 获取容器状态
     */
    public ContainerStatus getContainerStatus() {
        if (dockerClient == null) {
            return ContainerStatus.notAvailable();
        }

        Optional<String> optId = getContainerId();
        if (optId.isEmpty()) {
            return ContainerStatus.notFound();
        }

        String id = optId.get();
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withIdFilter(List.of(id))
                .exec();

        if (containers.isEmpty()) {
            return ContainerStatus.notFound();
        }

        Container container = containers.get(0);
        boolean running = "running".equals(container.getState());

        // 获取健康状态
        String health = "unknown";
        if (container.getStatus() != null && container.getStatus().contains("healthy")) {
            health = "healthy";
        } else if (container.getStatus() != null && container.getStatus().contains("unhealthy")) {
            health = "unhealthy";
        }

        return ContainerStatus.builder()
                .containerId(id)
                .running(running)
                .state(container.getState())
                .health(health)
                .statusText(container.getStatus())
                .build();
    }

    /**
     * 停止容器
     */
    public void stopContainer() {
        if (dockerClient == null) {
            log.warn("Docker client is not available. Cannot stop container.");
            return;
        }
        getContainerId().ifPresent(id -> {
            log.info("Stopping MCP runtime container: {}", id);
            dockerClient.stopContainerCmd(id).exec();
        });
    }

    /**
     * 删除容器
     */
    public void removeContainer() {
        if (dockerClient == null) {
            log.warn("Docker client is not available. Cannot remove container.");
            return;
        }
        getContainerId().ifPresent(id -> {
            log.info("Removing MCP runtime container: {}", id);
            dockerClient.removeContainerCmd(id)
                    .withForce(true)
                    .exec();
            this.containerId = null;
        });
    }

    /**
     * 获取容器日志
     */
    public String getContainerLogs(int tailLines) {
        if (dockerClient == null) {
            return "Docker client is not available";
        }

        Optional<String> optId = getContainerId();
        if (optId.isEmpty()) {
            return "Container not found";
        }

        try {
            return dockerClient.logContainerCmd(optId.get())
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(tailLines)
                    .exec(new LogContainerResultCallback())
                    .awaitCompletion()
                    .toString();
        } catch (Exception e) {
            log.error("Failed to get container logs: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // ========== MCP Host HTTP API 代理方法 ==========

    /**
     * 在容器内启动 MCP Server 进程
     */
    public boolean startServerProcess(McpServer server) {
        if (restClient == null) {
            log.warn("MCP Runtime is not available. Cannot start server process.");
            return false;
        }
        String url = String.format("/servers/%s/start", server.getName());

        StartProcessRequest request = StartProcessRequest.builder()
                .command(buildCommand(server))
                .env(server.getEnv() != null ? parseEnv(server.getEnv()) : null)
                .build();

        try {
            ResponseEntity<StartProcessResponse> response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .toEntity(StartProcessResponse.class);

            if (response.getBody() != null && "started".equals(response.getBody().getStatus())) {
                log.info("Started MCP server process in container: {}, PID: {}",
                        server.getName(), response.getBody().getPid());
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to start server process in container: {}", e.getMessage());
        }

        return false;
    }

    /**
     * 停止容器内的 MCP Server 进程
     */
    public boolean stopServerProcess(String serverName) {
        if (restClient == null) {
            log.warn("MCP Runtime is not available. Cannot stop server process.");
            return false;
        }
        String url = String.format("/servers/%s/stop", serverName);

        try {
            ResponseEntity<StopProcessResponse> response = restClient.post()
                    .uri(url)
                    .retrieve()
                    .toEntity(StopProcessResponse.class);

            return response.getBody() != null &&
                    ("stopped".equals(response.getBody().getStatus()) ||
                            "already_stopped".equals(response.getBody().getStatus()));
        } catch (Exception e) {
            log.error("Failed to stop server process: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 发送 JSON-RPC 请求
     */
    public String sendRpc(String serverName, String jsonRpcRequest) {
        if (restClient == null) {
            throw new McpConnectionException("MCP Runtime is not available");
        }
        String url = String.format("/rpc/%s", serverName);

        try {
            return restClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .body(jsonRpcRequest)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("RPC call failed: {}", e.getMessage());
            throw new McpConnectionException("RPC call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取服务器进程状态
     */
    public ProcessStatus getProcessStatus(String serverName) {
        if (restClient == null) {
            return ProcessStatus.unknown(serverName);
        }
        String url = String.format("/servers/%s/status", serverName);

        try {
            ResponseEntity<ProcessStatusResponse> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(ProcessStatusResponse.class);

            if (response.getBody() != null) {
                ProcessStatusResponse body = response.getBody();
                return ProcessStatus.builder()
                        .serverId(body.getServerId())
                        .status(body.getStatus())
                        .pid(body.getPid())
                        .uptimeSeconds(body.getUptimeSeconds())
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to get process status: {}", e.getMessage());
        }

        return ProcessStatus.unknown(serverName);
    }

    /**
     * 获取服务器进程日志
     */
    public String getProcessLogs(String serverName, int lines) {
        if (restClient == null) {
            return "MCP Runtime is not available";
        }
        String url = String.format("/servers/%s/logs?lines=%d", serverName, lines);

        try {
            ResponseEntity<ProcessLogsResponse> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(ProcessLogsResponse.class);

            if (response.getBody() != null) {
                return response.getBody().getLogs();
            }
        } catch (Exception e) {
            log.error("Failed to get process logs: {}", e.getMessage());
        }

        return "";
    }

    // ========== 私有方法 ==========

    private String createAndStartContainer() {
        log.info("Creating new MCP runtime container...");

        // 拉取/检查镜像
        ensureImage();

        // 创建容器
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withPortBindings(new com.github.dockerjava.api.model.PortBinding(
                        new com.github.dockerjava.api.model.Ports.Binding("0.0.0.0", String.valueOf(hostPort)),
                        new com.github.dockerjava.api.model.ExposedPort(MCP_HOST_PORT)
                ))
                .withRestartPolicy(com.github.dockerjava.api.model.RestartPolicy.unlessStoppedRestart())
                .withAutoRemove(false);

        CreateContainerResponse container = dockerClient.createContainerCmd(MCP_RUNTIME_IMAGE)
                .withName(CONTAINER_NAME)
                .withHostConfig(hostConfig)
                .withExposedPorts(new com.github.dockerjava.api.model.ExposedPort(MCP_HOST_PORT))
                .exec();

        this.containerId = container.getId();

        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        log.info("MCP runtime container started: {}", containerId);

        // 等待服务就绪
        waitForMcpHostReady();

        return containerId;
    }

    private void ensureImage() {
        try {
            dockerClient.inspectImageCmd(MCP_RUNTIME_IMAGE).exec();
            log.debug("Docker image exists: {}", MCP_RUNTIME_IMAGE);
        } catch (Exception e) {
            log.warn("Docker image '{}' not found locally. Attempting to pull...", MCP_RUNTIME_IMAGE);
            try {
                dockerClient.pullImageCmd(MCP_RUNTIME_IMAGE)
                        .start()
                        .awaitCompletion();
                log.info("Successfully pulled image: {}", MCP_RUNTIME_IMAGE);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Image pull interrupted", ie);
            } catch (Exception pullEx) {
                // 拉取失败（镜像不存在于仓库或需要登录），继续尝试使用本地镜像
                log.warn("Failed to pull image '{}': {}. Will try to use local image if available.",
                        MCP_RUNTIME_IMAGE, pullEx.getMessage());
                log.warn("Please build the image locally with: cd docker/mcp-runtime && ./build.sh");
            }
        }
    }

    private void waitForMcpHostReady() {
        log.info("Waiting for MCP Host Service to be ready...");

        int maxAttempts = 30;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(1000);
                ResponseEntity<String> response = restClient.get()
                        .uri("/health")
                        .retrieve()
                        .toEntity(String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("MCP Host Service is ready");
                    return;
                }
            } catch (Exception e) {
                // 继续等待
            }
        }

        log.warn("MCP Host Service did not become ready within {} seconds", maxAttempts);
    }

    private List<String> buildCommand(McpServer server) {
        if (server.getArgs() != null && !server.getArgs().isEmpty()) {
            String argsStr = server.getArgs();
            // 解析 JSON 数组
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                List<String> args = mapper.readValue(argsStr,
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                        });
                List<String> command = new java.util.ArrayList<>();
                command.add(server.getCommand());
                command.addAll(args);
                return command;
            } catch (Exception e) {
                log.warn("Failed to parse args JSON: {}", argsStr);
            }
        }

        return List.of(server.getCommand());
    }

    private java.util.Map<String, String> parseEnv(String envJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(envJson,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {
                    });
        } catch (Exception e) {
            return java.util.Map.of();
        }
    }

    // ========== 内部 DTO 类 ==========

    @Data
    @lombok.Builder
    public static class ContainerStatus {
        private String containerId;
        private boolean running;
        private String state;
        private String health;
        private String statusText;

        public static ContainerStatus notFound() {
            return builder()
                    .running(false)
                    .state("not_found")
                    .build();
        }

        public static ContainerStatus notAvailable() {
            return builder()
                    .running(false)
                    .state("not_available")
                    .statusText("Docker client is not available")
                    .build();
        }
    }

    @Data
    @lombok.Builder
    public static class ProcessStatus {
        private String serverId;
        private String status;
        private Integer pid;
        private double uptimeSeconds;

        public static ProcessStatus unknown(String serverId) {
            return builder()
                    .serverId(serverId)
                    .status("unknown")
                    .build();
        }
    }

    @Data
    @lombok.Builder
    private static class StartProcessRequest {
        private List<String> command;
        private java.util.Map<String, String> env;
    }

    @Data
    private static class StartProcessResponse {
        private String status;
        private int pid;
        private List<String> command;
    }

    @Data
    private static class StopProcessResponse {
        private String status;
        private Integer pid;
    }

    @Data
    private static class ProcessStatusResponse {
        private String serverId;
        private String status;
        private Integer pid;
        private double uptimeSeconds;
        private List<String> command;
    }

    @Data
    private static class ProcessLogsResponse {
        private String logs;
        private int lines;
        private String file;
    }

    /**
     * 日志回调处理器
     */
    private static class LogContainerResultCallback
            extends com.github.dockerjava.api.async.ResultCallback.Adapter<
            com.github.dockerjava.api.model.Frame> {

        private final StringBuilder logs = new StringBuilder();

        @Override
        public void onNext(com.github.dockerjava.api.model.Frame frame) {
            logs.append(new String(frame.getPayload())).append("\n");
        }

        @Override
        public String toString() {
            return logs.toString();
        }
    }
}
