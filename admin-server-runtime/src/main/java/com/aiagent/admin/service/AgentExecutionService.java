package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.AgentExecuteRequest;
import com.aiagent.admin.api.dto.AgentExecuteResponse;
import com.aiagent.admin.api.dto.AgentExecutionLogResponse;
import com.aiagent.admin.api.dto.AgentStreamingEvent;
import com.aiagent.admin.domain.entity.AgentExecutionLog;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

/**
 * Agent 执行服务接口
 * <p>
 * 提供 Agent 的执行功能：
 * <ul>
 *   <li>Agent 单次执行（同步）</li>
 *   <li>Agent 对话（流式）</li>
 *   <li>执行日志查询</li>
 * </ul>
 * </p>
 * <p>
 * 执行流程：
 * <ol>
 *   <li>构建 Prompt（系统提示词 + 工具定义）</li>
 *   <li>调用 LLM</li>
 *   <li>检测 tool_calls</li>
 *   <li>执行工具，获取结果</li>
 *   <li>将工具结果加入对话，再次调用 LLM</li>
 *   <li>重复直到无 tool_calls</li>
 *   <li>返回最终响应</li>
 * </ol>
 * </p>
 *
 * @see com.aiagent.admin.domain.entity.Agent
 */
public interface AgentExecutionService {

    /**
     * 执行 Agent（同步模式）
     * <p>
     * 输入消息 → LLM 调用 → 工具执行 → 返回响应。
     * 支持多轮工具调用（最大 5 次）。
     * </p>
     *
     * @param agentId Agent ID
     * @param request 执行请求
     * @return 执行响应
     */
    AgentExecuteResponse execute(String agentId, AgentExecuteRequest request);

    /**
     * 查询 Agent 的执行日志列表
     *
     * @param agentId Agent ID
     * @param limit   限制数量（默认 20）
     * @return 执行日志列表
     */
    List<AgentExecutionLogResponse> getExecutionLogs(String agentId, int limit);

    /**
     * 查询执行日志详情
     *
     * @param logId 执行日志 ID
     * @return 执行日志详情
     */
    Optional<AgentExecutionLogResponse> getExecutionLogDetail(String logId);

    /**
     * 查询执行日志实体（用于内部调用）
     *
     * @param logId 执行日志 ID
     * @return 执行日志实体
     */
    Optional<AgentExecutionLog> findEntityById(String logId);

    /**
     * 执行 Agent（流式模式）
     * <p>
     * 流式输出执行过程，实时显示：
     * <ul>
     *   <li>模型响应文本</li>
     *   <li>工具调用开始/完成状态</li>
     *   <li>工具执行结果</li>
     * </ul>
     * </p>
     * <p>
     * 返回 Flux 流，每个事件包装为 AgentStreamingEvent，
     * 前端解析 SSE 格式实时渲染。
     * </p>
     *
     * @param agentId Agent ID
     * @param request 执行请求
     * @return 流式事件 Flux
     */
    Flux<AgentStreamingEvent> executeStream(String agentId, AgentExecuteRequest request);

    /**
     * 执行 Agent（流式回调模式）
     * <p>
     * 使用回调函数实时推送事件，确保真正的流式输出。
     * </p>
     *
     * @param agentId Agent ID
     * @param request 执行请求
     * @param onEvent 事件回调，每次事件立即调用
     */
    void executeStreamWithCallback(String agentId, AgentExecuteRequest request, java.util.function.Consumer<AgentStreamingEvent> onEvent);
}