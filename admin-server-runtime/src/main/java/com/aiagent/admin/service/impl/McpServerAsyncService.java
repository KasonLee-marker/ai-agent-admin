package com.aiagent.admin.service.impl;

import com.aiagent.admin.domain.entity.McpServer;
import com.aiagent.admin.domain.repository.McpServerRepository;
import com.aiagent.admin.service.event.McpServerRefreshEvent;
import com.aiagent.admin.service.mcp.McpRuntimeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MCP Server 异步服务
 * <p>
 * 专门处理 MCP Server 的异步任务，避免 {@code @Async} 自调用问题。
 * 使用事件机制避免循环依赖。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerAsyncService {

    private final McpServerRepository mcpServerRepository;
    private final McpRuntimeManager mcpRuntimeManager;

    /**
     * 定时检查并重启停止的 MCP Server 进程
     * 每 30 秒检查一次
     */
    @Scheduled(fixedRate = 30000)
    public void checkAndRestartStoppedServers() {
        try {
            List<McpServer> stoppedServers = mcpServerRepository.findAll().stream()
                    .filter(server -> "stdio".equals(server.getTransportType()))
                    .filter(server -> "DOCKER".equals(server.getRuntimeMode()))
                    .filter(server -> {
                        // 检查进程状态
                        McpRuntimeManager.ProcessStatus status = mcpRuntimeManager.getProcessStatus(server.getName());
                        return status == null || 
                               "unknown".equals(status.getStatus()) || 
                               "not_found".equals(status.getStatus()) ||
                               "stopped".equals(status.getStatus());
                    })
                    .toList();
            
            for (McpServer server : stoppedServers) {
                log.info("Detected stopped MCP Server: {}, attempting to restart", server.getName());
                try {
                    // 重新启动进程
                    restartMcpServer(server);
                } catch (Exception e) {
                    log.error("Failed to restart MCP Server {}: {}", server.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during MCP Server health check: {}", e.getMessage());
        }
    }
    
    /**
     * 重启 MCP Server 进程
     */
    private void restartMcpServer(McpServer server) {
        log.info("Restarting MCP Server: {}", server.getName());
        
        try {
            // 停止旧进程（如果存在）
            mcpRuntimeManager.stopServerProcess(server.getName());
            
            // 重新启动
            boolean started = mcpRuntimeManager.startServerProcess(server);
            
            if (started) {
                // 更新状态为 INSTALLING
                server.setProcessStatus("INSTALLING");
                server.setLastLog("进程已重启，正在等待安装完成...");
                mcpServerRepository.save(server);
                log.info("MCP Server {} restarted successfully", server.getName());
                
                // 启动状态轮询
                pollAndUpdateStatus(server.getId(), server.getName());
            } else {
                server.setProcessStatus("ERROR");
                server.setLastLog("进程重启失败");
                mcpServerRepository.save(server);
                log.error("Failed to restart MCP Server {}", server.getName());
            }
        } catch (Exception e) {
            log.error("Failed to restart MCP Server {}: {}", server.getName(), e.getMessage());
            server.setProcessStatus("ERROR");
            server.setLastLog("进程重启失败: " + e.getMessage());
            mcpServerRepository.save(server);
        }
    }
    
    /**
     * 异步启动 MCP Server 进程
     * <p>
     * 对于需要长时间安装的 MCP Server（如 npx），使用异步执行避免超时。
     * </p>
     *
     * @param event MCP Server 刷新事件
     */
    @Async("taskExecutor")
    @EventListener
    public void onMcpServerRefreshEvent(McpServerRefreshEvent event) {
        String serverId = event.getServerId();
        log.info("Starting async process for MCP Server: {}", serverId);
        
        // 等待事务提交（最多等待 5 秒）
        McpServer server = null;
        for (int i = 0; i < 10; i++) {
            server = mcpServerRepository.findById(serverId).orElse(null);
            if (server != null) {
                break;
            }
            log.info("MCP Server {} not found yet, waiting... (attempt {})", serverId, i + 1);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for MCP Server {}", serverId);
                return;
            }
        }
        
        if (server == null) {
            log.error("MCP Server not found after waiting: {}", serverId);
            return;
        }
        
        try {
            // 更新状态为安装中
            server.setProcessStatus("INSTALLING");
            server.setLastLog("正在启动进程，请稍候...");
            mcpServerRepository.save(server);
            log.info("Set MCP Server {} status to INSTALLING", serverId);
            
            // 启动 MCP Server 进程
            log.info("Starting MCP Server process for: {}", server.getName());
            boolean started = mcpRuntimeManager.startServerProcess(server);
            
            if (started) {
                // 获取进程状态
                McpRuntimeManager.ProcessStatus processStatus = mcpRuntimeManager.getProcessStatus(server.getName());
                if (processStatus != null && processStatus.getPid() != null) {
                    server.setProcessId(processStatus.getPid());
                    server.setProcessStatus("INSTALLING");
                    server.setLastLog("进程已启动，PID: " + processStatus.getPid() + "，正在等待安装完成...");
                    mcpServerRepository.save(server);
                    log.info("MCP Server process started for {} with PID {}", serverId, processStatus.getPid());
                    
                    // 轮询检查安装状态
                    pollAndUpdateStatus(serverId, server.getName());
                } else {
                    server.setProcessStatus("ERROR");
                    server.setLastLog("启动进程失败：无法获取进程状态");
                    mcpServerRepository.save(server);
                    log.error("Failed to get process status for MCP Server {}", serverId);
                }
            } else {
                server.setProcessStatus("ERROR");
                server.setLastLog("启动进程失败");
                mcpServerRepository.save(server);
                log.error("Failed to start MCP Server process for {}", serverId);
            }
            
        } catch (Exception e) {
            log.error("Async process failed for MCP Server {}: {}", serverId, e.getMessage(), e);
            
            // 更新错误状态
            try {
                McpServer serverError = mcpServerRepository.findById(serverId).orElse(null);
                if (serverError != null) {
                    serverError.setProcessStatus("ERROR");
                    serverError.setLastLog("启动失败: " + e.getMessage());
                    mcpServerRepository.save(serverError);
                }
            } catch (Exception ex) {
                log.error("Failed to update error status for MCP Server {}: {}", serverId, ex.getMessage());
            }
        }
    }
    
    /**
     * 轮询检查 MCP Server 安装状态
     */
    private void pollAndUpdateStatus(String serverId, String serverName) {
        log.info("Starting status polling for MCP Server: {}", serverId);
        
        for (int i = 0; i < 60; i++) {  // 最多轮询 60 次（约 2 分钟）
            try {
                Thread.sleep(2000);  // 每 2 秒检查一次
                
                McpRuntimeManager.ProcessStatus status = mcpRuntimeManager.getProcessStatus(serverName);
                if (status == null) {
                    log.warn("Process status not found for MCP Server: {}", serverName);
                    continue;
                }
                
                String installStatus = status.getInstallStatus();
                log.info("MCP Server {} install status: {}", serverName, installStatus);
                
                McpServer server = mcpServerRepository.findById(serverId).orElse(null);
                if (server == null) {
                    log.error("MCP Server not found during polling: {}", serverId);
                    return;
                }
                
                if ("ready".equals(installStatus)) {
                    // 安装完成，更新状态为 RUNNING
                    server.setProcessStatus("RUNNING");
                    server.setLastLog("MCP Server 安装完成，正在运行");
                    mcpServerRepository.save(server);
                    log.info("MCP Server {} is ready and running", serverName);
                    
                    // 刷新工具列表
                    refreshTools(server);
                    return;
                } else if ("error".equals(installStatus)) {
                    // 安装失败
                    server.setProcessStatus("ERROR");
                    server.setLastLog("MCP Server 安装失败");
                    mcpServerRepository.save(server);
                    log.error("MCP Server {} installation failed", serverName);
                    return;
                }
                // 继续轮询...
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Status polling interrupted for MCP Server: {}", serverId);
                return;
            } catch (Exception e) {
                log.error("Error during status polling for MCP Server {}: {}", serverId, e.getMessage());
            }
        }
        
        log.warn("Status polling timeout for MCP Server: {}", serverId);
    }
    
    /**
     * 刷新 MCP Server 工具列表
     */
    private void refreshTools(McpServer server) {
        try {
            log.info("Refreshing tools for MCP Server: {}", server.getName());
            // TODO: 实现工具列表刷新
            // 这里可以调用 McpServerServiceImpl 的 refreshTools 方法
            // 或者使用 ApplicationEventPublisher 发布事件
        } catch (Exception e) {
            log.error("Failed to refresh tools for MCP Server {}: {}", server.getName(), e.getMessage());
        }
    }
}
