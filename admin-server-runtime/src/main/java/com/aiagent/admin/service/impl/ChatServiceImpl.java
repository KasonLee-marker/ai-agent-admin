package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.*;
import com.aiagent.admin.domain.enums.MessageRole;
import com.aiagent.admin.domain.repository.*;
import com.aiagent.admin.service.ChatService;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.IdGenerator;
import com.aiagent.admin.service.RagService;
import com.aiagent.admin.service.tool.ToolExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天服务实现类
 * <p>
 * 提供聊天会话管理和消息处理的核心功能：
 * <ul>
 *   <li>会话创建、查询、删除</li>
 *   <li>消息发送（同步和流式）</li>
 *   <li>AI 模型调用与响应处理</li>
 *   <li>历史消息管理</li>
 * </ul>
 * </p>
 * <p>
 * 使用 Spring AI OpenAiChatClient 进行模型调用，支持 OpenAI 兼容的 API（如 DashScope）。
 * 流式响应通过 Reactor Flux 实现，支持 SSE 格式输出。
 * </p>
 *
 * @see ChatService
 * @see OpenAiChatClient
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AgentRepository agentRepository;
    private final AgentToolRepository agentToolRepository;
    private final ToolRepository toolRepository;
    private final AgentExecutionEngine executionEngine;
    private final Map<String, ToolExecutor> toolExecutorMap;
    private final EncryptionService encryptionService;
    private final IdGenerator idGenerator;
    private final RagService ragService;
    private final ObjectMapper objectMapper;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;


    @Override
    @Transactional
    public ChatSessionDTO createSession(ChatRequest.CreateSessionRequest request, String createdBy) {
        // 如果指定了 agentId，从 Agent 加载配置
        String modelId = request.getModelId();
        String systemMessage = request.getSystemMessage();
        String agentId = request.getAgentId();
        String agentName = null;

        if (agentId != null && !agentId.isEmpty()) {
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new EntityNotFoundException("Agent not found with id: " + agentId));

            // Agent 配置覆盖：如果未手动指定，则使用 Agent 的配置
            if (modelId == null || modelId.isEmpty()) {
                modelId = agent.getModelId();
            }
            if (systemMessage == null || systemMessage.isEmpty()) {
                systemMessage = agent.getSystemPrompt();
            }
            agentName = agent.getName();

            log.info("Session created with Agent association: agentId={}, agentName={}, modelId={}",
                    agentId, agentName, modelId);
        }

        // 如果指定了 promptId，从模板加载内容作为 systemMessage（Agent 配置优先级更高）
        if ((systemMessage == null || systemMessage.isEmpty()) &&
                request.getPromptId() != null && !request.getPromptId().isEmpty()) {
            PromptTemplate template = promptTemplateRepository.findById(request.getPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("Prompt template not found with id: " + request.getPromptId()));
            systemMessage = template.getContent();
        }

        // RAG 配置：Embedding 模型必须继承知识库的默认模型
        String ragEmbeddingModelId = null;
        if (Boolean.TRUE.equals(request.getEnableRag()) && request.getKnowledgeBaseId() != null) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(request.getKnowledgeBaseId())
                    .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found: " + request.getKnowledgeBaseId()));
            ragEmbeddingModelId = kb.getDefaultEmbeddingModelId();
            if (ragEmbeddingModelId == null) {
                throw new IllegalStateException("知识库未配置默认 Embedding 模型，请先在知识库设置中绑定模型");
            }
        }

        ChatSession session = ChatSession.builder()
                .id(idGenerator.generateId())
                .title(request.getTitle())
                .modelId(modelId)
                .promptId(request.getPromptId())
                .systemMessage(systemMessage)
                .messageCount(0)
                .isActive(true)
                .createdBy(createdBy)
                // Agent 关联
                .agentId(agentId)
                // RAG 配置
                .enableRag(request.getEnableRag())
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .ragTopK(request.getRagTopK())
                .ragThreshold(request.getRagThreshold())
                .ragStrategy(request.getRagStrategy())
                .ragEmbeddingModelId(ragEmbeddingModelId)  // 使用知识库的模型
                .build();

        ChatSession saved = chatSessionRepository.save(session);
        return toSessionDTOWithAgentName(saved, agentName);
    }


    @Override
    @Transactional
    public ChatSessionDTO updateSession(String sessionId, ChatRequest.UpdateSessionRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Chat session not found with id: " + sessionId));

        String agentName = null;

        // 更新 Agent 关联
        if (request.getAgentId() != null) {
            String newAgentId = request.getAgentId();
            if (!newAgentId.isEmpty()) {
                Agent agent = agentRepository.findById(newAgentId)
                        .orElseThrow(() -> new EntityNotFoundException("Agent not found with id: " + newAgentId));

                session.setAgentId(newAgentId);
                agentName = agent.getName();

                // Agent 配置覆盖：如果未手动指定，则使用 Agent 的配置
                if (request.getModelId() == null) {
                    session.setModelId(agent.getModelId());
                }
                if (request.getSystemMessage() == null || request.getSystemMessage().isEmpty()) {
                    session.setSystemMessage(agent.getSystemPrompt());
                }

                log.info("Session updated with Agent association: agentId={}, agentName={}",
                        newAgentId, agentName);
            } else {
                // 清空 Agent 关联
                session.setAgentId(null);
                log.info("Session Agent association cleared: sessionId={}", sessionId);
            }
        }

        // 更新标题
        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }

        // 更新模型ID
        if (request.getModelId() != null) {
            session.setModelId(request.getModelId());
        }

        // 更新提示词ID和系统消息
        if (request.getPromptId() != null) {
            session.setPromptId(request.getPromptId());
            // 如果指定了 promptId，从模板加载内容
            if (request.getPromptId().isEmpty()) {
                // 清空 promptId
                session.setPromptId(null);
            } else {
                PromptTemplate template = promptTemplateRepository.findById(request.getPromptId())
                        .orElseThrow(() -> new EntityNotFoundException("Prompt template not found with id: " + request.getPromptId()));
                // 如果用户没有手动指定 systemMessage，则使用模板内容
                if (request.getSystemMessage() == null || request.getSystemMessage().isEmpty()) {
                    session.setSystemMessage(template.getContent());
                } else {
                    session.setSystemMessage(request.getSystemMessage());
                }
            }
        } else if (request.getSystemMessage() != null) {
            // 单独更新系统消息
            session.setSystemMessage(request.getSystemMessage());
        }

        // 更新 RAG 配置
        if (request.getEnableRag() != null) {
            session.setEnableRag(request.getEnableRag());
        }

        // 知识库变更时，自动更新 Embedding 模型为知识库的默认模型
        if (request.getKnowledgeBaseId() != null) {
            session.setKnowledgeBaseId(request.getKnowledgeBaseId());
            // 如果启用 RAG，自动继承知识库的 Embedding 模型
            if (Boolean.TRUE.equals(session.getEnableRag()) && !request.getKnowledgeBaseId().isEmpty()) {
                KnowledgeBase kb = knowledgeBaseRepository.findById(request.getKnowledgeBaseId())
                        .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found: " + request.getKnowledgeBaseId()));
                session.setRagEmbeddingModelId(kb.getDefaultEmbeddingModelId());
            }
        }

        if (request.getRagTopK() != null) {
            session.setRagTopK(request.getRagTopK());
        }
        if (request.getRagThreshold() != null) {
            session.setRagThreshold(request.getRagThreshold());
        }
        if (request.getRagStrategy() != null) {
            session.setRagStrategy(request.getRagStrategy());
        }
        // 忽略用户传入的 ragEmbeddingModelId，保持使用知识库的模型

        session.setUpdatedAt(LocalDateTime.now());
        ChatSession saved = chatSessionRepository.save(session);

        // 如果有 agentName，加载；否则从已保存的 agentId 加载
        if (agentName == null && saved.getAgentId() != null) {
            agentName = agentRepository.findById(saved.getAgentId())
                    .map(Agent::getName)
                    .orElse(null);
        }

        return toSessionDTOWithAgentName(saved, agentName);
    }


    @Override
    @Transactional(readOnly = true)
    public ChatSessionDTO getSession(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Chat session not found with id: " + sessionId));
        return toSessionDTO(session);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionDTO> listSessions(String createdBy, Pageable pageable) {
        Page<ChatSession> page = chatSessionRepository.findByCreatedByOrderByUpdatedAtDesc(createdBy, pageable);
        return page.getContent().stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionDTO> listSessionsByKeyword(String createdBy, String keyword, Pageable pageable) {
        Page<ChatSession> page = chatSessionRepository.findByCreatedByAndKeyword(createdBy, keyword, pageable);
        return page.getContent().stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new EntityNotFoundException("Chat session not found with id: " + sessionId);
        }
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }


    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        // 使用公共方法获取 session 和 modelConfig
        SessionModelContext context = getSessionAndModelContext(request);
        ChatSession session = context.session();
        ModelConfig modelConfig = context.modelConfig();

        // 保存用户消息
        saveUserMessageInternal(session, request.getContent());

        // RAG 检索（如果启用）
        List<VectorSearchResult> sources = null;
        if (Boolean.TRUE.equals(session.getEnableRag())) {
            sources = retrieveDocuments(session, request.getContent());
        }

        ChatMessage assistantMessage;
        try {
            // 判断是否为 Agent 会话
            String aiResponse;
            List<ToolCallRecord> toolCalls = null;
            if (session.getAgentId() != null && !session.getAgentId().isEmpty()) {
                // Agent 会话：使用 AgentExecutionEngine 执行工具调用
                AgentExecutionEngine.ExecutionResult result = executeAgent(session, request.getContent(), sources);
                aiResponse = result.finalResponse();
                toolCalls = result.toolCallRecords();
                if (!result.success()) {
                    throw new RuntimeException(result.errorMessage());
                }
            } else {
                // 普通会话：直接调用 AI 模型
                aiResponse = callAiModel(session, request.getContent(), modelConfig, sources);
            }
            long latency = System.currentTimeMillis() - startTime;

            assistantMessage = ChatMessage.builder()
                    .id(idGenerator.generateId())
                    .sessionId(session.getId())
                    .role(MessageRole.ASSISTANT)
                    .content(aiResponse)
                    .modelName(modelConfig.getModelName())
                    .latencyMs(latency)
                    .isError(false)
                    .sources(sources != null ? serializeSources(sources) : null)
                    .toolCalls(toolCalls != null ? serializeToolCalls(toolCalls) : null)
                    .build();
        } catch (Exception e) {
            log.error("Error calling AI model: {}", e.getMessage(), e);

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Unknown error";
            }

            if (errorMsg.contains("404")) {
                errorMsg = "模型可能不存在或 API endpoint 不正确: " + modelConfig.getModelName();
            }

            assistantMessage = ChatMessage.builder()
                    .id(idGenerator.generateId())
                    .sessionId(session.getId())
                    .role(MessageRole.ASSISTANT)
                    .content("")
                    .modelName(modelConfig.getModelName())
                    .isError(true)
                    .errorMessage(errorMsg)
                    .build();
        }

        chatMessageRepository.save(assistantMessage);

        session.setMessageCount((int) chatMessageRepository.countBySessionId(session.getId()));
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        return toMessageDTO(assistantMessage);
    }

    /**
     * 执行 Agent 并处理工具调用
     * <p>
     * 使用 AgentExecutionEngine 执行 Agent，支持工具调用循环。
     * 如果有 RAG 检索结果，会注入到上下文中。
     * </p>
     *
     * @param session     聊天会话（包含 Agent 关联）
     * @param userContent 用户输入内容
     * @param sources     RAG 检索来源（可选）
     * @return Agent 执行结果（包含响应内容和工具调用记录）
     */
    private AgentExecutionEngine.ExecutionResult executeAgent(ChatSession session, String userContent, List<VectorSearchResult> sources) {
        // 1. 加载Agent配置
        Agent agent = executionEngine.getAgent(session.getAgentId());

        // 2. 构建消息列表
        List<ChatCompletionMessage> messages = new ArrayList<>();
        String systemPrompt = executionEngine.buildSystemPrompt(agent);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            String enhancedPrompt = buildSystemMessageWithRag(systemPrompt, sources);
            messages.add(new ChatCompletionMessage(enhancedPrompt, Role.SYSTEM));
        }
        String userMessage = buildUserMessageWithRag(userContent, sources);
        messages.add(new ChatCompletionMessage(userMessage, Role.USER));

        // 3. 构建用户上下文
        Map<String, Object> userContext = buildAgentContext(sources);

        // 4. 使用Engine执行（同步模式，回调仅用于日志）
        AgentExecutionEngine.ExecutionResult result = executionEngine.executeWithCallback(
                session.getAgentId(), messages, userContext,
                event -> log.debug("Agent event: type={}, content={}", event.getType(), event.getContent()));

        log.info("Agent executed: agentId={}, toolCalls={}, durationMs={}, success={}",
                session.getAgentId(), result.toolCallRecords().size(), result.durationMs(), result.success());

        return result;
    }

    /**
     * 构建带 RAG 上下文的用户消息
     *
     * @param userContent 用户原始输入
     * @param sources     RAG 检索来源（可选）
     * @return 增强后的用户消息
     */
    private String buildUserMessageWithRag(String userContent, List<VectorSearchResult> sources) {
        if (sources == null || sources.isEmpty()) {
            return userContent;
        }

        String context = sources.stream()
                .map(VectorSearchResult::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        return String.format("""
                参考信息：
                %s
                
                用户问题：%s
                
                请基于参考信息回答用户问题。
                """, context, userContent);
    }

    /**
     * 构建 Agent 执行上下文
     *
     * @param sources RAG 检索来源（可选）
     * @return 执行上下文 Map
     */
    private Map<String, Object> buildAgentContext(List<VectorSearchResult> sources) {
        Map<String, Object> context = new HashMap<>();
        if (sources != null && !sources.isEmpty()) {
            context.put("ragSources", sources);
        }
        return context;
    }

    /**
     * 检索相关文档（RAG）
     *
     * @param session     聊天会话，包含 RAG 配置
     * @param userContent 用户问题
     * @return 检索结果列表
     */
    private List<VectorSearchResult> retrieveDocuments(ChatSession session, String userContent) {
        RagChatRequest ragRequest = new RagChatRequest();
        ragRequest.setQuestion(userContent);
        ragRequest.setKnowledgeBaseId(session.getKnowledgeBaseId());
        ragRequest.setTopK(session.getRagTopK() != null ? session.getRagTopK() : 5);
        ragRequest.setThreshold(session.getRagThreshold() != null ? session.getRagThreshold() : 0.5);
        ragRequest.setStrategy(session.getRagStrategy());
        ragRequest.setEmbeddingModelId(session.getRagEmbeddingModelId());

        return ragService.retrieve(ragRequest);
    }

    /**
     * 序列化检索来源为 JSON
     *
     * @param sources 检索结果列表
     * @return JSON 字符串
     */
    private String serializeSources(List<VectorSearchResult> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize sources: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 序列化工具调用记录为 JSON
     *
     * @param toolCalls 工具调用记录列表
     * @return JSON 字符串
     */
    private String serializeToolCalls(List<ToolCallRecord> toolCalls) {
        try {
            return objectMapper.writeValueAsString(toolCalls);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize toolCalls: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建消息列表(包含系统消息、历史消息和当前用户消息)
     * <p>
     * 如果提供了 RAG 检索来源，会将上下文注入到系统消息中。
     * </p>
     *
     * @param session     聊天会话，包含系统消息模板
     * @param userContent 当前用户输入内容
     * @param sources     RAG 检索来源（可选）
     * @return 构建好的消息列表，用于发送给 AI 模型
     */
    private List<org.springframework.ai.chat.messages.Message> buildMessageList(ChatSession session, String userContent, List<VectorSearchResult> sources) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        // 系统消息（可能包含 RAG 上下文）
        String systemMessage = buildSystemMessage(session, sources);
        if (systemMessage != null && !systemMessage.isEmpty()) {
            messages.add(new SystemMessage(systemMessage));
        }

        List<ChatMessage> history = chatMessageRepository.findConversationHistory(session.getId());
        for (ChatMessage msg : history) {
            if (msg.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        messages.add(new UserMessage(userContent));
        return messages;
    }

    /**
     * 构建系统消息（包含 RAG 上下文）
     *
     * @param session 聊天会话
     * @param sources RAG 检索来源（可选）
     * @return 系统消息
     */
    private String buildSystemMessage(ChatSession session, List<VectorSearchResult> sources) {
        String baseMessage = session.getSystemMessage();

        // 如果有 RAG 来源，注入上下文
        if (sources != null && !sources.isEmpty()) {
            String context = sources.stream()
                    .map(VectorSearchResult::getContent)
                    .collect(Collectors.joining("\n\n---\n\n"));

            String ragPrompt = """
                    你是一个智能助手，请根据以下参考信息回答用户的问题。
                    如果参考信息中没有相关内容，请诚实地说"根据现有信息无法回答该问题"。
                    
                    参考信息：
                    %s
                    
                    请基于以上参考信息回答用户问题。
                    """.formatted(context);

            // 如果有自定义系统消息，追加 RAG 上下文提示
            if (baseMessage != null && !baseMessage.isEmpty()) {
                return baseMessage + "\n\n" + ragPrompt;
            }
            return ragPrompt;
        }

        return baseMessage;
    }

    /**
     * 提取公共逻辑:获取 Session 和 ModelConfig
     */
    private SessionModelContext getSessionAndModelContext(ChatRequest request) {
        ChatSession session = chatSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new EntityNotFoundException("Chat session not found with id: " + request.getSessionId()));

        final String modelId = request.getModelId() != null ? request.getModelId() : session.getModelId();
        final ModelConfig modelConfig;
        if (modelId == null) {
            modelConfig = modelConfigRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new IllegalStateException("No default model configured"));
        } else {
            modelConfig = modelConfigRepository.findById(modelId)
                    .orElseThrow(() -> new EntityNotFoundException("Model not found with id: " + modelId));
        }

        return new SessionModelContext(session, modelConfig);
    }

    /**
     * 保存用户消息（公开方法，供 Controller 调用）
     */
    @Override
    public void saveUserMessage(String sessionId, String content) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
        ChatMessage userMessage = ChatMessage.builder()
                .id(idGenerator.generateId())
                .sessionId(sessionId)
                .role(MessageRole.USER)
                .content(content)
                .build();
        chatMessageRepository.save(userMessage);
        log.info("Saved user message for session: {}", sessionId);
    }

    /**
     * 保存用户消息（内部方法）
     */
    private ChatMessage saveUserMessageInternal(ChatSession session, String content) {
        ChatMessage userMessage = ChatMessage.builder()
                .id(idGenerator.generateId())
                .sessionId(session.getId())
                .role(MessageRole.USER)
                .content(content)
                .build();
        return chatMessageRepository.save(userMessage);
    }

    /**
     * 内部类:封装 Session 和 ModelConfig 上下文
     */
    private record SessionModelContext(ChatSession session, ModelConfig modelConfig) {
    }

    /**
     * 调用 AI 模型获取响应
     * <p>
     * 使用 Spring AI OpenAiChatClient 进行模型调用。
     * 支持通过配置的 baseUrl 和 apiKey 连接到 OpenAI 兼容的 API。
     * 如果提供了 sources，会注入 RAG 上下文。
     * </p>
     *
     * @param session     聊天会话，用于获取历史上下文
     * @param userContent 用户输入内容
     * @param modelConfig 模型配置，包含 API endpoint、模型名称、温度等参数
     * @param sources     RAG 检索来源（可选）
     * @return AI 模型的响应文本
     * @throws RuntimeException 模型调用失败时抛出
     */
    private String callAiModel(ChatSession session, String userContent, ModelConfig modelConfig, List<VectorSearchResult> sources) {
        List<Message> messages = buildMessageList(session, userContent, sources);
        Prompt prompt = new Prompt(messages);

        try {
            log.info("Calling AI model: name={}, modelName={}, baseUrl={}, ragEnabled={}",
                    modelConfig.getName(), modelConfig.getModelName(), modelConfig.getBaseUrl(), sources != null);

            OpenAiChatClient chatClient = buildChatClient(modelConfig);
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("AI model call failed for model {} ({}): {}",
                    modelConfig.getName(), modelConfig.getModelName(), e.getMessage(), e);

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Unknown error";
            }

            throw new RuntimeException("模型调用失败: " + errorMsg +
                    " [模型: " + modelConfig.getModelName() +
                    ", URL: " + modelConfig.getBaseUrl() + "]");
        }
    }

    /**
     * 构建 OpenAI ChatClient
     * <p>
     * 根据模型配置创建 OpenAiApi 和 OpenAiChatClient 实例。
     * 配置参数包括：baseUrl、apiKey、modelName、temperature、maxTokens、topP。
     * </p>
     *
     * @param config 模型配置实体
     * @return 配置好的 OpenAiChatClient 实例
     */
    private OpenAiChatClient buildChatClient(ModelConfig config) {
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() :
            config.getProvider().getDefaultBaseUrl();

        log.debug("Building chat client: model={}, baseUrl={}", config.getModelName(), baseUrl);

        OpenAiApi api = new OpenAiApi(baseUrl, apiKey);

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .withModel(config.getModelName());

        if (config.getTemperature() != null) {
            optionsBuilder.withTemperature(config.getTemperature().floatValue());
        }
        if (config.getMaxTokens() != null) {
            optionsBuilder.withMaxTokens(config.getMaxTokens());
        }
        if (config.getTopP() != null) {
            optionsBuilder.withTopP(config.getTopP().floatValue());
        }

        return new OpenAiChatClient(api, optionsBuilder.build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getSessionMessages(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getConversationHistory(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findConversationHistory(sessionId);
        return messages.stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将会话实体转换为 DTO
     *
     * @param session 会话实体
     * @return 会话 DTO
     */
    private ChatSessionDTO toSessionDTO(ChatSession session) {
        String agentName = null;
        if (session.getAgentId() != null) {
            agentName = agentRepository.findById(session.getAgentId())
                    .map(Agent::getName)
                    .orElse(null);
        }
        return toSessionDTOWithAgentName(session, agentName);
    }

    /**
     * 将会话实体转换为 DTO（包含 Agent 名称）
     *
     * @param session   会话实体
     * @param agentName Agent 名称（可选）
     * @return 会话 DTO
     */
    private ChatSessionDTO toSessionDTOWithAgentName(ChatSession session, String agentName) {
        return ChatSessionDTO.builder()
                .id(session.getId())
                .title(session.getTitle())
                .modelId(session.getModelId())
                .promptId(session.getPromptId())
                .systemMessage(session.getSystemMessage())
                .messageCount(session.getMessageCount())
                .isActive(session.getIsActive())
                // Agent 关联
                .agentId(session.getAgentId())
                .agentName(agentName)
                // RAG 配置
                .enableRag(session.getEnableRag())
                .knowledgeBaseId(session.getKnowledgeBaseId())
                .ragTopK(session.getRagTopK())
                .ragThreshold(session.getRagThreshold())
                .ragStrategy(session.getRagStrategy())
                .ragEmbeddingModelId(session.getRagEmbeddingModelId())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .createdBy(session.getCreatedBy())
                .build();
    }

    /**
     * 将消息实体转换为 DTO
     *
     * @param message 消息实体
     * @return 消息 DTO
     */
    private ChatResponse toMessageDTO(ChatMessage message) {
        // 解析 sources JSON
        List<VectorSearchResult> sources = null;
        if (message.getSources() != null && !message.getSources().isEmpty()) {
            try {
                sources = objectMapper.readValue(message.getSources(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, VectorSearchResult.class));
            } catch (JsonProcessingException e) {
                log.error("Failed to parse sources JSON: {}", e.getMessage());
            }
        }

        // 解析 toolCalls JSON
        List<ToolCallRecord> toolCalls = null;
        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
            try {
                toolCalls = objectMapper.readValue(message.getToolCalls(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ToolCallRecord.class));
            } catch (JsonProcessingException e) {
                log.error("Failed to parse toolCalls JSON: {}", e.getMessage());
            }
        }

        return ChatResponse.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .modelName(message.getModelName())
                .tokenCount(message.getTokenCount())
                .latencyMs(message.getLatencyMs())
                .isError(message.getIsError())
                .errorMessage(message.getErrorMessage())
                .sources(sources)
                .toolCalls(toolCalls)
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * 流式发送消息（回调模式）
     * <p>
     * 支持普通对话和Agent对话的流式输出。
     * </p>
     */
    @Override
    public void sendMessageStreamWithCallback(ChatRequest request, java.util.function.Consumer<String> onChunk) {
        SessionModelContext context = getSessionAndModelContext(request);
        ChatSession session = context.session();
        ModelConfig modelConfig = context.modelConfig();

        // 保存用户消息
        saveUserMessageInternal(session, request.getContent());

        // RAG 检索（如果启用）
        List<VectorSearchResult> sources = null;
        if (Boolean.TRUE.equals(session.getEnableRag())) {
            sources = retrieveDocuments(session, request.getContent());
        }

        // 判断是否为 Agent 会话
        if (session.getAgentId() != null && !session.getAgentId().isEmpty()) {
            // Agent 会话流式执行
            executeAgentStream(session, request.getContent(), sources, modelConfig, onChunk);
        } else {
            // 普通会话流式执行（使用 OkHttp 直接调用）
            executeNormalStream(session, request.getContent(), sources, modelConfig, onChunk);
        }
    }

    /**
     * 执行 Agent 会话的流式输出
     * <p>
     * 使用 AgentExecutionEngine 执行，完成后保存 ChatMessage（含 toolCalls）。
     * 不保存 AgentExecutionLog。
     * </p>
     */
    private void executeAgentStream(ChatSession session, String userContent, List<VectorSearchResult> sources,
                                    ModelConfig modelConfig, java.util.function.Consumer<String> onChunk) {
        // 1. 加载Agent配置
        Agent agent = executionEngine.getAgent(session.getAgentId());

        // 2. 构建消息列表
        List<ChatCompletionMessage> messages = new ArrayList<>();
        String systemPrompt = executionEngine.buildSystemPrompt(agent);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            // 如果有RAG来源，注入到系统消息
            String enhancedPrompt = buildSystemMessageWithRag(systemPrompt, sources);
            messages.add(new ChatCompletionMessage(enhancedPrompt, Role.SYSTEM));
        }
        // 用户消息（含RAG上下文）
        String userMessage = buildUserMessageWithRag(userContent, sources);
        messages.add(new ChatCompletionMessage(userMessage, Role.USER));

        // 3. 构建用户上下文
        Map<String, Object> userContext = buildAgentContext(sources);

        // 4. 使用Engine执行
        StringBuilder fullResponse = new StringBuilder();
        List<ToolCallRecord> toolCalls = new ArrayList<>();
        boolean success = true;
        String errorMessage = null;

        try {
            AgentExecutionEngine.ExecutionResult result = executionEngine.executeWithCallback(
                    session.getAgentId(), messages, userContext,
                    event -> {
                        if (event.getType() == StreamingEventType.MODEL_OUTPUT) {
                            if (event.getContent() != null) {
                                fullResponse.append(event.getContent());
                                onChunk.accept(event.getContent());
                            }
                        } else if (event.getType() == StreamingEventType.TOOL_START) {
                            if (event.getToolCall() != null) {
                                toolCalls.add(event.getToolCall());
                                String toolStartText = "\n🔧 **调用工具: " + event.getToolCall().getToolName() + "**\n";
                                fullResponse.append(toolStartText);
                                onChunk.accept(toolStartText);
                            }
                        } else if (event.getType() == StreamingEventType.TOOL_END) {
                            if (event.getToolCall() != null) {
                                // 更新工具调用记录
                                updateToolCallRecord(toolCalls, event.getToolCall());
                                String toolEndText;
                                if (event.getToolCall().getSuccess()) {
                                    toolEndText = "✅ 工具完成 (" + event.getToolCall().getDurationMs() + "ms)\n\n";
                                } else {
                                    toolEndText = "❌ 工具失败: " + event.getToolCall().getErrorMessage() + "\n\n";
                                }
                                fullResponse.append(toolEndText);
                                onChunk.accept(toolEndText);
                            }
                        } else if (event.getType() == StreamingEventType.ERROR) {
                            String errorText = "\n❌ 错误: " + event.getContent() + "\n";
                            fullResponse.append(errorText);
                            onChunk.accept(errorText);
                        }
                    });

            success = result.success();
            if (!success) {
                errorMessage = result.errorMessage();
            }
        } catch (Exception e) {
            log.error("Agent stream execution error: {}", e.getMessage(), e);
            success = false;
            errorMessage = e.getMessage();
            onChunk.accept("\n❌ 执行失败: " + errorMessage + "\n");
        }

        // 5. 保存助手消息（含toolCalls）- 无论成功还是失败都保存
        saveAssistantMessageTransactional(session.getId(), modelConfig.getModelName(), fullResponse.toString(), sources, toolCalls, success, errorMessage);
    }

    /**
     * 更新工具调用记录（根据TOOL_END事件更新）
     */
    private void updateToolCallRecord(List<ToolCallRecord> records, ToolCallRecord updated) {
        for (int i = 0; i < records.size(); i++) {
            ToolCallRecord record = records.get(i);
            if (record.getToolName().equals(updated.getToolName()) && record.getSuccess() == null) {
                records.set(i, updated);
                break;
            }
        }
    }

    /**
     * 构建带RAG上下文的系统消息
     */
    private String buildSystemMessageWithRag(String basePrompt, List<VectorSearchResult> sources) {
        if (sources == null || sources.isEmpty()) {
            return basePrompt;
        }

        String context = sources.stream()
                .map(VectorSearchResult::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        String ragPrompt = """
                参考信息：
                %s
                
                请基于以上参考信息回答用户问题。
                """.formatted(context);

        if (basePrompt != null && !basePrompt.isEmpty()) {
            return basePrompt + "\n\n" + ragPrompt;
        }
        return ragPrompt;
    }

    /**
     * 执行普通会话的流式输出（使用 OkHttp 直接调用 API）
     */
    private void executeNormalStream(ChatSession session, String userContent, List<VectorSearchResult> sources,
                                     ModelConfig modelConfig, java.util.function.Consumer<String> onChunk) {
        try {
            log.info("executeNormalStream: sessionId={}, modelName={}", session.getId(), modelConfig.getModelName());

            // 构建请求 JSON
            List<Map<String, Object>> messages = new ArrayList<>();

            // 系统消息
            String systemPrompt = buildSystemMessage(session, sources);
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }

            // 历史消息
            List<ChatMessage> history = chatMessageRepository.findConversationHistory(session.getId());
            for (ChatMessage msg : history) {
                if (msg.getRole() == MessageRole.USER) {
                    messages.add(Map.of("role", "user", "content", msg.getContent()));
                } else if (msg.getRole() == MessageRole.ASSISTANT) {
                    messages.add(Map.of("role", "assistant", "content", msg.getContent()));
                }
            }

            // 当前用户消息
            messages.add(Map.of("role", "user", "content", userContent));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelConfig.getModelName());
            requestBody.put("messages", messages);
            requestBody.put("stream", true);

            if (modelConfig.getTemperature() != null) {
                requestBody.put("temperature", modelConfig.getTemperature());
            }
            if (modelConfig.getMaxTokens() != null) {
                requestBody.put("max_tokens", modelConfig.getMaxTokens());
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String apiKey = encryptionService.decrypt(modelConfig.getApiKey());
            String baseUrl = modelConfig.getBaseUrl() != null ? modelConfig.getBaseUrl() :
                    modelConfig.getProvider().getDefaultBaseUrl();
            String url = baseUrl + "/v1/chat/completions";

            log.info("Stream API URL: {}, model: {}", url, modelConfig.getModelName());

            // 创建 OkHttp 客户端
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                    .build();

            StringBuilder fullResponse = new StringBuilder();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No body";
                    log.error("API call failed: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("API call failed: " + response.code() + " - " + errorBody);
                }

                log.info("API call successful, reading stream...");
                BufferedSource source = response.body().source();
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null || line.isEmpty()) continue;

                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if (data.equals("[DONE]")) break;

                        try {
                            Map<String, Object> chunk = objectMapper.readValue(data,
                                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                    });
                            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                if (delta != null && delta.get("content") != null) {
                                    String content = (String) delta.get("content");
                                    fullResponse.append(content);
                                    onChunk.accept(content);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse SSE chunk: {}", data);
                        }
                    }
                }

                // 保存助手消息
                saveAssistantMessageTransactional(session.getId(), modelConfig.getModelName(), fullResponse.toString(), sources, null, true, null);
            }
        } catch (Exception e) {
            log.error("Stream execution failed: {}", e.getMessage(), e);
            onChunk.accept("\n❌ 错误: " + e.getMessage());
            saveAssistantMessageTransactional(session.getId(), modelConfig.getModelName(), "", sources, null, false, e.getMessage());
        }
    }

    /**
     * 准备流式上下文(阻塞操作,需在单独线程池执行) - 已废弃，保留供参考
     * <p>
     * 如果会话启用了 RAG，会先检索文档并注入上下文。
     * </p>
     */
    private StreamContext prepareStreamContext(ChatRequest request) {
        // 使用公共方法获取 session 和 modelConfig
        SessionModelContext context = getSessionAndModelContext(request);
        ChatSession session = context.session();
        ModelConfig modelConfig = context.modelConfig();

        // 保存用户消息
        saveUserMessageInternal(session, request.getContent());

        // RAG 检索（如果启用）
        List<VectorSearchResult> sources = null;
        if (Boolean.TRUE.equals(session.getEnableRag())) {
            sources = retrieveDocuments(session, request.getContent());
        }

        // 构建消息列表（包含 RAG 上下文）
        List<org.springframework.ai.chat.messages.Message> messages = buildMessageList(session, request.getContent(), sources);
        Prompt prompt = new Prompt(messages);
        OpenAiChatClient chatClient = buildChatClient(modelConfig);

        return new StreamContext(session, modelConfig, prompt, chatClient, sources);
    }

    /**
     * 流式上下文，包含 session、modelConfig、prompt、chatClient 和 RAG sources
     */
    private record StreamContext(
            ChatSession session,
            ModelConfig modelConfig,
            Prompt prompt,
            OpenAiChatClient chatClient,
            List<VectorSearchResult> sources
    ) {
    }

    /**
     * 保存流式响应完成后生成的助手消息
     * <p>
     * 更新会话的消息计数和最后更新时间。
     * 如果执行失败，标记为错误消息并记录错误原因。
     * 如果有 RAG 检索来源，会存储到消息中。
     * </p>
     * <p>
     * 使用编程式事务管理（TransactionTemplate），确保在异步线程中也能正确保存。
     * </p>
     *
     * @param sessionId   会话ID
     * @param modelName   模型名称（可选，如为空则从 session 的 agent 或关联模型获取）
     * @param content     助手响应内容
     * @param sources     RAG 检索来源（可选）
     * @param toolCalls   工具调用记录（可选）
     * @param success     执行是否成功
     * @param errorMessage 错误信息（可选，失败时记录）
     */
    @Override
    public void saveAssistantMessageTransactional(String sessionId, String modelName, String content,
                                                  List<VectorSearchResult> sources, List<ToolCallRecord> toolCalls,
                                                  boolean success, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            // 重新查询 session 确保在当前事务中
            ChatSession session = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));

            // 如果没有传入 modelName，尝试从 session 关联获取
            String finalModelName = modelName;
            if (finalModelName == null && session.getAgentId() != null) {
                Agent agent = agentRepository.findById(session.getAgentId()).orElse(null);
                if (agent != null && agent.getModelId() != null) {
                    ModelConfig modelConfig = modelConfigRepository.findById(agent.getModelId()).orElse(null);
                    if (modelConfig != null) {
                        finalModelName = modelConfig.getModelName();
                    }
                }
            }
            if (finalModelName == null && session.getModelId() != null) {
                ModelConfig modelConfig = modelConfigRepository.findById(session.getModelId()).orElse(null);
                if (modelConfig != null) {
                    finalModelName = modelConfig.getModelName();
                }
            }

            String toolCallsJson = null;
            if (toolCalls != null && !toolCalls.isEmpty()) {
                try {
                    toolCallsJson = objectMapper.writeValueAsString(toolCalls);
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize tool calls: {}", e.getMessage());
                }
            }

            ChatMessage assistantMessage = ChatMessage.builder()
                    .id(idGenerator.generateId())
                    .sessionId(sessionId)
                    .role(MessageRole.ASSISTANT)
                    .content(content)
                    .modelName(finalModelName)
                    .isError(!success)
                    .errorMessage(errorMessage)
                    .sources(sources != null ? serializeSources(sources) : null)
                    .toolCalls(toolCallsJson)
                    .build();
            chatMessageRepository.save(assistantMessage);

            session.setMessageCount((int) chatMessageRepository.countBySessionId(sessionId));
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);

            log.info("Saved assistant message for session: {}, isError: {}, contentLength: {}, toolCalls: {}",
                    sessionId, !success, content.length(), toolCalls != null ? toolCalls.size() : 0);
        });
    }
}