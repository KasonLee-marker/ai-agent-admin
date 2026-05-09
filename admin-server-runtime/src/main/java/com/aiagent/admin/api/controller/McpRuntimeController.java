package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.ApiResponse;
import com.aiagent.admin.service.mcp.McpRuntimeManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * MCP Runtime 管理 REST 控制器
 * <p>
 * 提供 Docker 容器和进程状态管理 API：
 * <ul>
 *   <li>容器生命周期管理（启动/停止/状态查询）</li>
 *   <li>容器日志查看</li>
 *   <li>进程状态查询</li>
 *   <li>进程日志查看</li>
 *   <li>进程重启</li>
 * </ul>
 * </p>
 *
 * @see McpRuntimeManager
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "MCP Runtime Management", description = "APIs for managing MCP Docker runtime and processes")
public class McpRuntimeController {

    private final McpRuntimeManager mcpRuntimeManager;

    /**
     * 获取 MCP Runtime 容器状态
     */
    @GetMapping("/mcp-runtime/status")
    @Operation(summary = "Get MCP runtime container status",
            description = "Check if the MCP runtime Docker container is running")
    public ApiResponse<ContainerStatusDTO> getRuntimeStatus() {
        McpRuntimeManager.ContainerStatus status = mcpRuntimeManager.getContainerStatus();

        ContainerStatusDTO dto = ContainerStatusDTO.builder()
                .containerId(status.getContainerId())
                .running(status.isRunning())
                .state(status.getState())
                .health(status.getHealth())
                .statusText(status.getStatusText())
                .build();

        return ApiResponse.success(dto);
    }

    /**
     * 启动 MCP Runtime 容器
     */
    @PostMapping("/mcp-runtime/start")
    @Operation(summary = "Start MCP runtime container",
            description = "Create and start the MCP runtime Docker container if not exists")
    public ApiResponse<ContainerStatusDTO> startRuntime() {
        String containerId = mcpRuntimeManager.ensureContainerRunning();
        McpRuntimeManager.ContainerStatus status = mcpRuntimeManager.getContainerStatus();

        ContainerStatusDTO dto = ContainerStatusDTO.builder()
                .containerId(containerId)
                .running(status.isRunning())
                .state(status.getState())
                .health(status.getHealth())
                .build();

        return ApiResponse.success(dto);
    }

    /**
     * 停止 MCP Runtime 容器
     */
    @PostMapping("/mcp-runtime/stop")
    @Operation(summary = "Stop MCP runtime container",
            description = "Stop the MCP runtime Docker container")
    public ApiResponse<Void> stopRuntime() {
        mcpRuntimeManager.stopContainer();
        return ApiResponse.success();
    }

    /**
     * 获取 MCP Runtime 容器日志
     */
    @GetMapping("/mcp-runtime/logs")
    @Operation(summary = "Get MCP runtime container logs",
            description = "Retrieve logs from the MCP runtime Docker container")
    public ApiResponse<LogsDTO> getRuntimeLogs(
            @RequestParam(defaultValue = "100") int lines) {
        String logs = mcpRuntimeManager.getContainerLogs(lines);
        return ApiResponse.success(new LogsDTO(logs, lines));
    }

    // ========== DTO 类 ==========

    @lombok.Data
    @lombok.Builder
    public static class ContainerStatusDTO {
        private String containerId;
        private boolean running;
        private String state;
        private String health;
        private String statusText;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class LogsDTO {
        private String logs;
        private int lines;
    }
}
