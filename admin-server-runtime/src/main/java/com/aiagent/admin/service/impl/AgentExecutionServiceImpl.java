package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Agent;
import com.aiagent.admin.domain.entity.AgentExecutionLog;
import com.aiagent.admin.domain.repository.AgentExecutionLogRepository;
import com.aiagent.admin.domain.repository.AgentRepository;
import com.aiagent.admin.service.AgentExecutionService;
import com.aiagent.admin.service.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Agent 执行服务实现类
 * <p>
 * 使用 AgentExecutionEngine 执行核心逻辑，然后保存执行日志。
 * 仅负责日志保存，核心执行逻辑委托给 AgentExecutionEngine。
 * </p>
 *
 * @see AgentExecutionService
 * @see AgentExecutionEngine
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionServiceImpl implements AgentExecutionService {

    private final AgentRepository agentRepository;
    private final AgentExecutionLogRepository executionLogRepository;
    private final AgentExecutionEngine executionEngine;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AgentExecuteResponse execute(String agentId, AgentExecuteRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. 加载Agent配置
        Agent agent = executionEngine.getAgent(agentId);

        // 2. 构建消息列表
        List<ChatCompletionMessage> messages = new ArrayList<>();
        String systemPrompt = executionEngine.buildSystemPrompt(agent);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new ChatCompletionMessage(systemPrompt, Role.SYSTEM));
        }
        messages.add(new ChatCompletionMessage(request.getMessage(), Role.USER));

        // 3. 使用Engine执行（不发送事件）
        List<ToolCallRecord> toolCallRecords = new ArrayList<>();
        AgentExecutionEngine.ExecutionResult result = executionEngine.executeWithCallback(
                agentId, messages, request.getContext(),
                event -> {
                    // 同步模式不处理事件，只收集工具调用记录
                    if (event.getType() == StreamingEventType.TOOL_END && event.getToolCall() != null) {
                        toolCallRecords.add(event.getToolCall());
                    }
                });

        long duration = System.currentTimeMillis() - startTime;

        // 4. 保存执行日志
        AgentExecutionLog executionLog = saveExecutionLog(
                agentId, request.getSessionId(), request.getMessage(),
                result.finalResponse(), result.toolCallRecords(),
                duration, result.success(), result.errorMessage());

        return AgentExecuteResponse.builder()
                .response(result.finalResponse())
                .sessionId(request.getSessionId())
                .toolCalls(result.toolCallRecords())
                .durationMs(duration)
                .executionLogId(executionLog.getId())
                .build();
    }

    @Override
    public Flux<AgentStreamingEvent> executeStream(String agentId, AgentExecuteRequest request) {
        return Flux.<AgentStreamingEvent>create(emitter -> {
                    // 1. 加载Agent配置
                    Agent agent = executionEngine.getAgent(agentId);

                    // 2. 构建消息列表
                    List<ChatCompletionMessage> messages = new ArrayList<>();
                    String systemPrompt = executionEngine.buildSystemPrompt(agent);
                    if (systemPrompt != null && !systemPrompt.isEmpty()) {
                        messages.add(new ChatCompletionMessage(systemPrompt, Role.SYSTEM));
                    }
                    messages.add(new ChatCompletionMessage(request.getMessage(), Role.USER));

                    // 3. 使用Engine执行
                    AgentExecutionEngine.ExecutionResult result = executionEngine.executeWithCallback(
                            agentId, messages, request.getContext(),
                            emitter::next);

                    emitter.complete();

                    // 4. 保存执行日志
                    saveExecutionLogAsync(agentId, request.getSessionId(), request.getMessage(),
                            result.finalResponse(), result.toolCallRecords(),
                            result.durationMs(), result.success(), result.errorMessage());

                }, FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void executeStreamWithCallback(String agentId, AgentExecuteRequest request,
                                          java.util.function.Consumer<AgentStreamingEvent> onEvent) {
        // 1. 加载Agent配置
        Agent agent = executionEngine.getAgent(agentId);

        // 2. 构建消息列表
        List<ChatCompletionMessage> messages = new ArrayList<>();
        String systemPrompt = executionEngine.buildSystemPrompt(agent);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new ChatCompletionMessage(systemPrompt, Role.SYSTEM));
        }
        messages.add(new ChatCompletionMessage(request.getMessage(), Role.USER));

        // 3. 使用Engine执行
        AgentExecutionEngine.ExecutionResult result = executionEngine.executeWithCallback(
                agentId, messages, request.getContext(), onEvent);

        // 4. 保存执行日志
        saveExecutionLogAsync(agentId, request.getSessionId(), request.getMessage(),
                result.finalResponse(), result.toolCallRecords(),
                result.durationMs(), result.success(), result.errorMessage());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentExecutionLogResponse> getExecutionLogs(String agentId, int limit) {
        return executionLogRepository.findRecentByAgentId(agentId, limit).stream()
                .map(this::toLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AgentExecutionLogResponse> getExecutionLogDetail(String logId) {
        return executionLogRepository.findById(logId)
                .map(this::toLogResponseWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AgentExecutionLog> findEntityById(String logId) {
        return executionLogRepository.findById(logId);
    }

    /**
     * 保存执行日志
     */
    private AgentExecutionLog saveExecutionLog(String agentId, String sessionId, String input,
                                               String output, List<ToolCallRecord> toolCalls,
                                               long duration, boolean success, String errorMessage) {
        try {
            String toolCallsJson = objectMapper.writeValueAsString(toolCalls);

            AgentExecutionLog logEntity = AgentExecutionLog.builder()
                    .id(idGenerator.generateId())
                    .agentId(agentId)
                    .sessionId(sessionId)
                    .input(input)
                    .output(output)
                    .toolCalls(toolCallsJson)
                    .durationMs(duration)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();

            return executionLogRepository.save(logEntity);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize tool calls: {}", e.getMessage());
            return executionLogRepository.save(AgentExecutionLog.builder()
                    .id(idGenerator.generateId())
                    .agentId(agentId)
                    .sessionId(sessionId)
                    .input(input)
                    .output(output)
                    .toolCalls("[]")
                    .durationMs(duration)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build());
        }
    }

    /**
     * 异步保存执行日志
     */
    private void saveExecutionLogAsync(String agentId, String sessionId, String input,
                                       String output, List<ToolCallRecord> toolCalls,
                                       long duration, boolean success, String errorMessage) {
        try {
            String toolCallsJson = objectMapper.writeValueAsString(toolCalls);

            AgentExecutionLog logEntity = AgentExecutionLog.builder()
                    .id(idGenerator.generateId())
                    .agentId(agentId)
                    .sessionId(sessionId)
                    .input(input)
                    .output(output)
                    .toolCalls(toolCallsJson)
                    .durationMs(duration)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();

            executionLogRepository.save(logEntity);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize tool calls: {}", e.getMessage());
        }
    }

    /**
     * 转换为日志响应DTO（摘要）
     */
    private AgentExecutionLogResponse toLogResponse(AgentExecutionLog log) {
        String inputSummary = truncate(log.getInput(), 100);
        String outputSummary = truncate(log.getOutput(), 100);
        int toolCallCount = parseToolCalls(log.getToolCalls()).size();

        return AgentExecutionLogResponse.builder()
                .id(log.getId())
                .agentId(log.getAgentId())
                .sessionId(log.getSessionId())
                .inputSummary(inputSummary)
                .outputSummary(outputSummary)
                .toolCallCount(toolCallCount)
                .durationMs(log.getDurationMs())
                .success(log.getSuccess())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }

    /**
     * 转换为日志响应DTO（含详情）
     */
    private AgentExecutionLogResponse toLogResponseWithDetails(AgentExecutionLog log) {
        List<ToolCallRecord> toolCalls = parseToolCalls(log.getToolCalls());

        return AgentExecutionLogResponse.builder()
                .id(log.getId())
                .agentId(log.getAgentId())
                .sessionId(log.getSessionId())
                .inputSummary(log.getInput())
                .outputSummary(log.getOutput())
                .toolCallCount(toolCalls.size())
                .durationMs(log.getDurationMs())
                .success(log.getSuccess())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .toolCalls(toolCalls)
                .build();
    }

    /**
     * 解析工具调用记录
     */
    private List<ToolCallRecord> parseToolCalls(String toolCallsJson) {
        if (toolCallsJson == null || toolCallsJson.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(toolCallsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}