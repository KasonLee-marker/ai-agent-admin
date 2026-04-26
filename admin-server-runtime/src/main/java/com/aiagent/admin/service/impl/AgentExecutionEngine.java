package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.AgentStreamingEvent;
import com.aiagent.admin.api.dto.ToolCallRecord;
import com.aiagent.admin.domain.entity.Agent;
import com.aiagent.admin.domain.entity.AgentTool;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.repository.AgentRepository;
import com.aiagent.admin.domain.repository.AgentToolRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.domain.repository.ToolRepository;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.tool.ExecutionContext;
import com.aiagent.admin.service.tool.ToolExecutor;
import com.aiagent.admin.service.tool.ToolResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionFinishReason;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.Role;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.openai.api.OpenAiApi.FunctionTool.Function;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Agent执行引擎
 * <p>
 * 封装Agent执行的核心逻辑，提供纯执行功能，不保存任何日志。
 * 被AgentExecutionServiceImpl和ChatServiceImpl共同复用。
 * </p>
 *
 * <ul>
 *   <li>加载Agent配置和工具</li>
 *   <li>构建LLM请求（含Function Calling）</li>
 *   <li>执行LLM调用 + 工具循环</li>
 *   <li>流式发送事件</li>
 * </ul>
 *
 * @see AgentExecutionServiceImpl
 * @see ChatServiceImpl
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionEngine {

    private final AgentRepository agentRepository;
    private final AgentToolRepository agentToolRepository;
    private final ToolRepository toolRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final Map<String, ToolExecutor> toolExecutorMap;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    /**
     * 最大工具调用迭代次数
     */
    private static final int MAX_TOOL_CALL_ITERATIONS = 5;

    /**
     * 执行结果封装
     */
    public record ExecutionResult(
            String finalResponse,
            List<ToolCallRecord> toolCallRecords,
            long durationMs,
            boolean success,
            String errorMessage
    ) {
    }

    /**
     * 执行Agent（回调模式）
     * <p>
     * 使用回调函数实时推送事件，确保真正的流式输出。
     * 适用于Servlet SseEmitter场景。
     * </p>
     *
     * @param agentId     Agent ID
     * @param messages    LLM消息列表（已有系统消息、用户消息）
     * @param userContext 用户上下文（RAG来源等）
     * @param onEvent     事件回调（MODEL_OUTPUT, TOOL_START, TOOL_END）
     * @return 执行结果
     */
    public ExecutionResult executeWithCallback(
            String agentId,
            List<ChatCompletionMessage> messages,
            Map<String, Object> userContext,
            Consumer<AgentStreamingEvent> onEvent) {

        long startTime = System.currentTimeMillis();
        int sequence = 0;

        try {
            // 1. 加载Agent配置
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

            ModelConfig modelConfig = agent.getModelId() != null ?
                    findModelConfig(agent.getModelId()) : null;

            if (modelConfig == null) {
                onEvent.accept(AgentStreamingEvent.error("Agent has no model configured", sequence++));
                return new ExecutionResult(null, List.of(), 0, false, "Agent has no model configured");
            }

            // 2. 加载绑定的工具
            List<AgentTool> agentTools = agentToolRepository.findByAgentIdAndEnabledTrue(agentId);
            List<Tool> tools = loadAgentTools(agentId);

            List<FunctionTool> functionTools = buildFunctionTools(tools);

            // 3. 创建OpenAI API客户端
            OpenAiApi api = createOpenAiApi(modelConfig);

            // 4. 执行循环
            List<ToolCallRecord> toolCallRecords = new ArrayList<>();
            String finalResponse = null;
            int iteration = 0;

            while (iteration < MAX_TOOL_CALL_ITERATIONS) {
                iteration++;

                ChatCompletionRequest chatRequest = new ChatCompletionRequest(
                        messages,
                        modelConfig.getModelName(),
                        functionTools,
                        "auto"
                );

                log.debug("Calling LLM with {} tools, iteration {}", functionTools.size(), iteration);

                OpenAiApi.ChatCompletion completion = api.chatCompletionEntity(chatRequest).getBody();

                if (completion == null || completion.choices() == null || completion.choices().isEmpty()) {
                    onEvent.accept(AgentStreamingEvent.error("No response from model", sequence++));
                    break;
                }

                OpenAiApi.ChatCompletion.Choice choice = completion.choices().get(0);
                ChatCompletionMessage assistantMessage = choice.message();
                ChatCompletionFinishReason finishReason = choice.finishReason();

                messages.add(assistantMessage);

                // 发送模型响应文本（如果有）
                if (assistantMessage.content() != null && !assistantMessage.content().isEmpty()) {
                    onEvent.accept(AgentStreamingEvent.modelOutput(assistantMessage.content(), sequence++));
                }

                // 检查是否需要调用工具
                if (finishReason == ChatCompletionFinishReason.TOOL_CALLS) {
                    List<ChatCompletionMessage.ToolCall> toolCalls = assistantMessage.toolCalls();
                    if (toolCalls == null || toolCalls.isEmpty()) {
                        finalResponse = assistantMessage.content();
                        break;
                    }

                    // 执行所有工具调用
                    for (ChatCompletionMessage.ToolCall toolCall : toolCalls) {
                        // 发送工具开始事件
                        ToolCallRecord recordStart = ToolCallRecord.builder()
                                .toolName(toolCall.function().name())
                                .args(parseArgs(toolCall.function().arguments()))
                                .build();
                        onEvent.accept(AgentStreamingEvent.toolStart(recordStart, sequence++));

                        // 执行工具
                        ToolCallRecord record = executeToolCall(
                                toolCall, agentId, agentTools, tools, userContext);
                        toolCallRecords.add(record);

                        // 发送工具完成事件（含结果）
                        onEvent.accept(AgentStreamingEvent.toolEnd(record, sequence++));

                        // 添加工具结果消息
                        String toolResultContent = buildToolResultContent(record);
                        messages.add(new ChatCompletionMessage(
                                toolResultContent,
                                Role.TOOL,
                                toolCall.id(),
                                toolCall.function().name(),
                                null
                        ));
                    }
                } else {
                    finalResponse = assistantMessage.content();
                    break;
                }
            }

            if (finalResponse == null) {
                finalResponse = "Reached maximum tool call iterations.";
            }

            // 发送完成事件
            onEvent.accept(AgentStreamingEvent.done(sequence++));

            long duration = System.currentTimeMillis() - startTime;
            return new ExecutionResult(finalResponse, toolCallRecords, duration, true, null);

        } catch (Exception e) {
            log.error("Agent execution failed: {}", e.getMessage(), e);
            onEvent.accept(AgentStreamingEvent.error("Execution failed: " + e.getMessage(), sequence++));

            long duration = System.currentTimeMillis() - startTime;
            return new ExecutionResult(null, List.of(), duration, false, e.getMessage());
        }
    }

    /**
     * 加载Agent绑定的工具
     *
     * @param agentId Agent ID
     * @return 工具列表
     */
    public List<Tool> loadAgentTools(String agentId) {
        List<AgentTool> agentTools = agentToolRepository.findByAgentIdAndEnabledTrue(agentId);
        return agentTools.stream()
                .map(at -> toolRepository.findById(at.getToolId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建OpenAI FunctionTool列表
     *
     * @param tools 工具列表
     * @return FunctionTool列表
     */
    public List<FunctionTool> buildFunctionTools(List<Tool> tools) {
        return tools.stream()
                .map(tool -> {
                    Map<String, Object> parameters = parseSchema(tool.getSchema());
                    Function function = new Function(
                            tool.getDescription(),
                            tool.getName(),
                            parameters
                    );
                    return new FunctionTool(function);
                })
                .collect(Collectors.toList());
    }

    /**
     * 执行单个工具调用
     *
     * @param toolCall    LLM返回的工具调用请求
     * @param agentId     Agent ID
     * @param agentTools  Agent工具绑定配置
     * @param tools       工具列表
     * @param userContext 用户上下文
     * @return 工具调用记录
     */
    public ToolCallRecord executeToolCall(ChatCompletionMessage.ToolCall toolCall,
                                          String agentId,
                                          List<AgentTool> agentTools,
                                          List<Tool> tools,
                                          Map<String, Object> userContext) {
        long startTime = System.currentTimeMillis();

        String toolName = toolCall.function().name();
        String argumentsJson = toolCall.function().arguments();

        log.debug("Executing tool: name={}, args={}", toolName, argumentsJson);

        // 解析参数
        Map<String, Object> args = parseArgs(argumentsJson);

        // 查找工具定义
        Tool tool = tools.stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElse(null);

        if (tool == null) {
            return ToolCallRecord.builder()
                    .toolId(null)
                    .toolName(toolName)
                    .args(args)
                    .success(false)
                    .errorMessage("Tool not found: " + toolName)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // 查找执行器
        ToolExecutor executor = toolExecutorMap.get(tool.getExecutor());
        if (executor == null) {
            return ToolCallRecord.builder()
                    .toolId(tool.getId())
                    .toolName(tool.getName())
                    .args(args)
                    .success(false)
                    .errorMessage("Executor not found: " + tool.getExecutor())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // 构建执行上下文
        Map<String, Object> toolConfig = parseConfig(tool.getConfig());
        Map<String, Object> agentToolConfig = agentTools.stream()
                .filter(at -> at.getToolId().equals(tool.getId()))
                .findFirst()
                .map(at -> {
                    Map<String, Object> config = parseConfig(at.getConfig());
                    Map<String, Object> merged = new HashMap<>(toolConfig);
                    merged.putAll(config);
                    return merged;
                })
                .orElse(toolConfig);

        ExecutionContext context = ExecutionContext.builder()
                .agentId(agentId)
                .agentToolConfig(agentToolConfig)
                .userContext(userContext)
                .build();

        try {
            ToolResult result = executor.execute(args, context);

            return ToolCallRecord.builder()
                    .toolId(tool.getId())
                    .toolName(tool.getName())
                    .args(args)
                    .result(result.getData())
                    .success(result.isSuccess())
                    .errorMessage(result.getErrorMessage())
                    .durationMs(result.getDurationMs() > 0 ? result.getDurationMs() : System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Tool execution failed: {}", e.getMessage());

            return ToolCallRecord.builder()
                    .toolId(tool.getId())
                    .toolName(tool.getName())
                    .args(args)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 构建工具结果内容（用于TOOL role消息）
     *
     * @param record 工具调用记录
     * @return 结果内容字符串
     */
    public String buildToolResultContent(ToolCallRecord record) {
        if (record.getSuccess() && record.getResult() != null) {
            try {
                return objectMapper.writeValueAsString(record.getResult());
            } catch (JsonProcessingException e) {
                return String.valueOf(record.getResult());
            }
        } else {
            return "Error: " + record.getErrorMessage();
        }
    }

    /**
     * 构建系统提示词
     *
     * @param agent Agent配置
     * @return 系统提示词
     */
    public String buildSystemPrompt(Agent agent) {
        if (agent.getSystemPrompt() == null || agent.getSystemPrompt().isEmpty()) {
            return "You are a helpful assistant. Use the provided tools when appropriate.";
        }
        return agent.getSystemPrompt();
    }

    /**
     * 创建OpenAI API客户端
     *
     * @param config 模型配置
     * @return OpenAiApi实例
     */
    public OpenAiApi createOpenAiApi(ModelConfig config) {
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() :
                config.getProvider().getDefaultBaseUrl();

        log.debug("Creating OpenAI API client: baseUrl={}", baseUrl);
        return new OpenAiApi(baseUrl, apiKey);
    }

    /**
     * 查找模型配置
     *
     * @param modelId 模型ID
     * @return 模型配置
     */
    public ModelConfig findModelConfig(String modelId) {
        return modelConfigRepository.findById(modelId).orElse(null);
    }

    /**
     * 获取Agent实体
     *
     * @param agentId Agent ID
     * @return Agent实体
     */
    public Agent getAgent(String agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
    }

    /**
     * 解析工具schema JSON
     */
    private Map<String, Object> parseSchema(String schema) {
        if (schema == null || schema.isEmpty()) {
            return Map.of("type", "object", "properties", Map.of());
        }
        try {
            return objectMapper.readValue(schema, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse tool schema: {}", e.getMessage());
            return Map.of("type", "object", "properties", Map.of());
        }
    }

    /**
     * 解析工具参数JSON
     */
    private Map<String, Object> parseArgs(String argsJson) {
        if (argsJson == null || argsJson.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(argsJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /**
     * 解析配置JSON
     */
    private Map<String, Object> parseConfig(String config) {
        if (config == null || config.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(config, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}