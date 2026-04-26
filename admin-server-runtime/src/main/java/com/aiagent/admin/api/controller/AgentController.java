package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.enums.AgentStatus;
import com.aiagent.admin.service.AgentExecutionService;
import com.aiagent.admin.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent 管理 REST 控制器
 * <p>
 * 提供 Agent 的管理 API：
 * <ul>
 *   <li>Agent CRUD 操作</li>
 *   <li>Agent-Tool 绑定管理</li>
 *   <li>Agent 状态管理</li>
 *   <li>Agent 筛选查询</li>
 * </ul>
 * </p>
 *
 * @see AgentService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent Management", description = "APIs for managing AI agents")
public class AgentController {

    private final AgentService agentService;
    private final AgentExecutionService agentExecutionService;
    private final ObjectMapper objectMapper;

    /**
     * 查询 Agent 列表
     * <p>
     * 支持按状态、模型、关键词筛选。
     * </p>
     *
     * @param status  Agent 状态筛选（可选）
     * @param modelId 模型 ID 筛选（可选）
     * @param keyword 搜索关键词（可选）
     * @return Agent 响应 DTO 列表
     */
    @GetMapping
    @Operation(summary = "List all agents with optional filters")
    public ApiResponse<List<AgentResponse>> listAgents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String modelId,
            @RequestParam(required = false) String keyword) {
        AgentStatus statusEnum = status != null ? AgentStatus.valueOf(status.toUpperCase()) : null;
        return ApiResponse.success(agentService.findByFilters(statusEnum, modelId, keyword));
    }

    /**
     * 根据 ID 获取 Agent 详情
     *
     * @param id Agent ID
     * @return Agent 响应 DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get agent by ID")
    public ApiResponse<AgentResponse> getAgent(
            @Parameter(description = "Agent ID") @PathVariable String id) {
        return agentService.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Agent not found"));
    }

    /**
     * 创建 Agent
     *
     * @param request 创建请求
     * @return 创建成功的 Agent 响应 DTO
     */
    @PostMapping
    @Operation(summary = "Create a new agent")
    public ApiResponse<AgentResponse> createAgent(
            @Valid @RequestBody CreateAgentRequest request) {
        return ApiResponse.success(agentService.create(request));
    }

    /**
     * 更新 Agent
     *
     * @param id      Agent ID
     * @param request 更新请求
     * @return 更新后的 Agent 响应 DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update agent")
    public ApiResponse<AgentResponse> updateAgent(
            @Parameter(description = "Agent ID") @PathVariable String id,
            @Valid @RequestBody UpdateAgentRequest request) {
        return ApiResponse.success(agentService.update(id, request));
    }

    /**
     * 删除 Agent
     *
     * @param id Agent ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete agent")
    public ApiResponse<Void> deleteAgent(
            @Parameter(description = "Agent ID") @PathVariable String id) {
        agentService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 更新 Agent 状态
     *
     * @param id      Agent ID
     * @param request 状态更新请求
     * @return 更新后的 Agent 响应 DTO
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update agent status")
    public ApiResponse<AgentResponse> updateStatus(
            @Parameter(description = "Agent ID") @PathVariable String id,
            @Valid @RequestBody AgentStatusRequest request) {
        AgentStatus status = AgentStatus.valueOf(request.getStatus().toUpperCase());
        return ApiResponse.success(agentService.updateStatus(id, status));
    }

    /**
     * 绑定工具到 Agent
     *
     * @param id      Agent ID
     * @param request 工具绑定请求
     * @return 绑定关系响应 DTO
     */
    @PostMapping("/{id}/tools")
    @Operation(summary = "Bind tool to agent")
    public ApiResponse<ToolBindingResponse> bindTool(
            @Parameter(description = "Agent ID") @PathVariable String id,
            @Valid @RequestBody ToolBindingRequest request) {
        return ApiResponse.success(agentService.bindTool(id, request));
    }

    /**
     * 解绑 Agent 的工具
     *
     * @param id     Agent ID
     * @param toolId 工具 ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/{id}/tools/{toolId}")
    @Operation(summary = "Unbind tool from agent")
    public ApiResponse<Void> unbindTool(
            @Parameter(description = "Agent ID") @PathVariable String id,
            @Parameter(description = "Tool ID") @PathVariable String toolId) {
        agentService.unbindTool(id, toolId);
        return ApiResponse.success();
    }

    /**
     * 更新 Agent-Tool 绑定配置
     *
     * @param id      Agent ID
     * @param toolId  工具 ID
     * @param request 工具绑定请求
     * @return 更新后的绑定关系响应 DTO
     */
    @PutMapping("/{id}/tools/{toolId}")
    @Operation(summary = "Update tool binding config")
    public ApiResponse<ToolBindingResponse> updateToolBinding(
            @Parameter(description = "Agent ID") @PathVariable String id,
            @Parameter(description = "Tool ID") @PathVariable String toolId,
            @Valid @RequestBody ToolBindingRequest request) {
        return ApiResponse.success(agentService.updateToolBinding(id, toolId, request));
    }

    /**
     * 获取 Agent 绑定的所有工具
     *
     * @param id Agent ID
     * @return 工具绑定响应 DTO 列表
     */
    @GetMapping("/{id}/tools")
    @Operation(summary = "Get agent's bound tools")
    public ApiResponse<List<ToolBindingResponse>> getAgentTools(
            @Parameter(description = "Agent ID") @PathVariable String id) {
        return ApiResponse.success(agentService.getAgentTools(id));
    }

    /**
     * 执行 Agent（同步模式）
     * <p>
     * 输入消息，Agent 调用 LLM 和工具，返回响应。
     * </p>
     *
     * @param id      Agent ID
     * @param request 执行请求
     * @return 执行响应
     */
    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute agent with message")
    public ApiResponse<AgentExecuteResponse> executeAgent(
            @Parameter(description = "Agent ID") @PathVariable String id,
            @Valid @RequestBody AgentExecuteRequest request) {
        return ApiResponse.success(agentExecutionService.execute(id, request));
    }

    /**
     * 获取 Agent 执行日志列表
     *
     * @param id    Agent ID
     * @param limit 限制数量（默认 20）
     * @return 执行日志响应 DTO 列表
     */
    @GetMapping("/{id}/logs")
    @Operation(summary = "Get agent execution logs")
    public ApiResponse<List<AgentExecutionLogResponse>> getExecutionLogs(
            @Parameter(description = "Agent ID") @PathVariable String id,
            @Parameter(description = "Limit") @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(agentExecutionService.getExecutionLogs(id, limit));
    }

    /**
     * 获取执行日志详情
     *
     * @param id    Agent ID
     * @param logId 执行日志 ID
     * @return 执行日志详情响应 DTO
     */
    @GetMapping("/{id}/logs/{logId}")
    @Operation(summary = "Get execution log detail")
    public ApiResponse<AgentExecutionLogResponse> getExecutionLogDetail(
            @Parameter(description = "Agent ID") @PathVariable String id,
            @Parameter(description = "Log ID") @PathVariable String logId) {
        return agentExecutionService.getExecutionLogDetail(logId)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Execution log not found"));
    }

    /**
     * 执行 Agent（流式模式）
     * <p>
     * 返回 SSE 流，实时发送事件：
     * <ul>
     *   <li>MODEL_OUTPUT - 模型响应文本</li>
     *   <li>TOOL_START - 工具调用开始</li>
     *   <li>TOOL_END - 工具调用完成（含结果）</li>
     *   <li>DONE - 执行完成</li>
     * </ul>
     * </p>
     *
     * @param id      Agent ID
     * @param request 执行请求
     * @return SSE Emitter
     */
    @PostMapping(value = "/{id}/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Execute agent with streaming output")
    public SseEmitter executeAgentStream(
            @Parameter(description = "Agent ID") @PathVariable String id,
            @Valid @RequestBody AgentExecuteRequest request) {

        log.info("executeAgentStream called: agentId={}", id);

        // 创建 SSE Emitter，超时 60 秒
        SseEmitter emitter = new SseEmitter(60000L);

        // 设置响应头（确保不缓冲）
        emitter.onCompletion(() -> log.info("SSE stream completed: agentId={}", id));
        emitter.onTimeout(() -> {
            log.warn("SSE stream timeout: agentId={}", id);
            emitter.complete();
        });
        emitter.onError(e -> log.error("SSE emitter error: agentId={}, error={}", id, e.getMessage()));

        // 使用线程池执行，避免阻塞请求线程
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                agentExecutionService.executeStreamWithCallback(id, request, event -> {
                    try {
                        String json = objectMapper.writeValueAsString(event);
                        emitter.send(SseEmitter.event().data(json));
                        log.debug("SSE event sent: type={}, sequence={}", event.getType(), event.getSequence());
                    } catch (IOException e) {
                        log.error("Failed to send SSE event: {}", e.getMessage());
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("Stream execution error: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .data("{\"type\":\"ERROR\",\"content\":\"" + e.getMessage() + "\"}"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(e);
                }
            } finally {
                executor.shutdown();
            }
        });

        return emitter;
    }
}