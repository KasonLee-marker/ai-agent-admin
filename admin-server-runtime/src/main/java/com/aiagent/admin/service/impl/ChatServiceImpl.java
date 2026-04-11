package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.ChatRequest;
import com.aiagent.admin.api.dto.ChatResponse;
import com.aiagent.admin.api.dto.ChatSessionDTO;
import com.aiagent.admin.domain.entity.ChatMessage;
import com.aiagent.admin.domain.entity.ChatSession;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.entity.PromptTemplate;
import com.aiagent.admin.domain.enums.MessageRole;
import com.aiagent.admin.domain.repository.ChatMessageRepository;
import com.aiagent.admin.domain.repository.ChatSessionRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.domain.repository.PromptTemplateRepository;
import com.aiagent.admin.service.ChatService;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.IdGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final PromptTemplateRepository promptTemplateRepository;
    private final EncryptionService encryptionService;
    private final IdGenerator idGenerator;
    private String userContent;

    /**
     * 创建新的聊天会话
     * <p>
     * 初始化会话时设置标题、关联模型和提示词模板，以及系统消息。
     * 会话创建后消息计数为 0，状态为活跃。
     * </p>
     *
     * @param request   创建会话请求，包含标题、模型ID、提示词ID等
     * @param createdBy 会话创建者标识
     * @return 创建成功的会话 DTO
     */
    @Override
    @Transactional
    public ChatSessionDTO createSession(ChatRequest.CreateSessionRequest request, String createdBy) {
        // 如果指定了 promptId，从模板加载内容作为 systemMessage
        String systemMessage = request.getSystemMessage();
        if (request.getPromptId() != null && !request.getPromptId().isEmpty()) {
            PromptTemplate template = promptTemplateRepository.findById(request.getPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("Prompt template not found with id: " + request.getPromptId()));
            // 如果用户没有手动指定 systemMessage，则使用模板内容
            if (systemMessage == null || systemMessage.isEmpty()) {
                systemMessage = template.getContent();
            }
        }

        ChatSession session = ChatSession.builder()
                .id(idGenerator.generateId())
                .title(request.getTitle())
                .modelId(request.getModelId())
                .promptId(request.getPromptId())
                .systemMessage(systemMessage)
                .messageCount(0)
                .isActive(true)
                .createdBy(createdBy)
                .build();

        ChatSession saved = chatSessionRepository.save(session);
        return toSessionDTO(saved);
    }

    /**
     * 更新会话信息
     * <p>
     * 支持更新标题、模型ID、提示词ID和系统消息。
     * 如果指定了新的 promptId 且未手动指定 systemMessage，则从模板加载内容。
     * </p>
     *
     * @param sessionId 会话唯一标识
     * @param request   更新请求，包含可更新的字段
     * @return 更新后的会话 DTO
     * @throws EntityNotFoundException 会话不存在时抛出
     */
    @Override
    @Transactional
    public ChatSessionDTO updateSession(String sessionId, ChatRequest.UpdateSessionRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Chat session not found with id: " + sessionId));

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

        session.setUpdatedAt(LocalDateTime.now());
        ChatSession saved = chatSessionRepository.save(session);
        return toSessionDTO(saved);
    }

    /**
     * 根据会话 ID 获取会话详情
     *
     * @param sessionId 会话唯一标识
     * @return 会话 DTO
     * @throws EntityNotFoundException 会话不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public ChatSessionDTO getSession(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Chat session not found with id: " + sessionId));
        return toSessionDTO(session);
    }

    /**
     * 分页查询用户的聊天会话列表
     * <p>
     * 按更新时间倒序排列，返回最近的会话优先。
     * </p>
     *
     * @param createdBy 会话创建者标识
     * @param pageable  分页参数
     * @return 会话 DTO 列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionDTO> listSessions(String createdBy, Pageable pageable) {
        Page<ChatSession> page = chatSessionRepository.findByCreatedByOrderByUpdatedAtDesc(createdBy, pageable);
        return page.getContent().stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据关键词搜索用户的聊天会话
     * <p>
     * 支持在会话标题和消息内容中搜索匹配关键词。
     * </p>
     *
     * @param createdBy 会话创建者标识
     * @param keyword   搜索关键词
     * @param pageable  分页参数
     * @return 匹配的会话 DTO 列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionDTO> listSessionsByKeyword(String createdBy, String keyword, Pageable pageable) {
        Page<ChatSession> page = chatSessionRepository.findByCreatedByAndKeyword(createdBy, keyword, pageable);
        return page.getContent().stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 删除聊天会话及其所有消息
     * <p>
     * 同时删除会话记录和关联的所有消息记录。
     * </p>
     *
     * @param sessionId 会话唯一标识
     * @throws EntityNotFoundException 会话不存在时抛出
     */
    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new EntityNotFoundException("Chat session not found with id: " + sessionId);
        }
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }

    /**
     * 发送消息并获取 AI 响应（同步模式）
     * <p>
     * 执行流程：
     * <ol>
     *   <li>获取会话和模型配置</li>
     *   <li>保存用户消息</li>
     *   <li>构建包含系统消息、历史消息和当前消息的 Prompt</li>
     *   <li>调用 AI 模型获取响应</li>
     *   <li>保存助手消息并更新会话统计</li>
     * </ol>
     * </p>
     *
     * @param request 消息请求，包含会话ID、消息内容、可选模型ID
     * @return 助手响应消息 DTO
     */
    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        // 使用公共方法获取 session 和 modelConfig
        SessionModelContext context = getSessionAndModelContext(request);
        ChatSession session = context.session();
        ModelConfig modelConfig = context.modelConfig();

        // 保存用户消息
        saveUserMessage(session, request.getContent());

        ChatMessage assistantMessage;
        try {
            String aiResponse = callAiModel(session, request.getContent(), modelConfig);
            long latency = System.currentTimeMillis() - startTime;

            assistantMessage = ChatMessage.builder()
                    .id(idGenerator.generateId())
                    .sessionId(session.getId())
                    .role(MessageRole.ASSISTANT)
                    .content(aiResponse)
                    .modelName(modelConfig.getModelName())
                    .latencyMs(latency)
                    .isError(false)
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
     * 构建消息列表(包含系统消息、历史消息和当前用户消息)
     *
     * @param session     聊天会话，包含系统消息模板
     * @param userContent 当前用户输入内容
     * @return 构建好的消息列表，用于发送给 AI 模型
     */
    private List<org.springframework.ai.chat.messages.Message> buildMessageList(ChatSession session, String userContent) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        // 系统消息
        if (session.getSystemMessage() != null && !session.getSystemMessage().isEmpty()) {
            messages.add(new SystemMessage(session.getSystemMessage()));
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
     * 保存用户消息
     */
    private ChatMessage saveUserMessage(ChatSession session, String content) {
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
     * </p>
     *
     * @param session     聊天会话，用于获取历史上下文
     * @param userContent 用户输入内容
     * @param modelConfig 模型配置，包含 API endpoint、模型名称、温度等参数
     * @return AI 模型的响应文本
     * @throws RuntimeException 模型调用失败时抛出
     */
    private String callAiModel(ChatSession session, String userContent, ModelConfig modelConfig) {
        List<Message> messages = buildMessageList(session, userContent);
        Prompt prompt = new Prompt(messages);

        try {
            log.info("Calling AI model: name={}, modelName={}, baseUrl={}",
                    modelConfig.getName(), modelConfig.getModelName(), modelConfig.getBaseUrl());

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

    /**
     * 获取会话的所有消息列表
     * <p>
     * 按消息创建时间升序排列，返回完整的对话历史。
     * </p>
     *
     * @param sessionId 会话唯一标识
     * @return 消息 DTO 列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getSessionMessages(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取会话的对话历史（用于构建 AI Prompt）
     * <p>
     * 返回用于构建多轮对话 Prompt 的历史消息。
     * 与 getSessionMessages 类似，但可能应用不同的过滤逻辑。
     * </p>
     *
     * @param sessionId 会话唯一标识
     * @return 消息 DTO 列表
     */
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
        return ChatSessionDTO.builder()
                .id(session.getId())
                .title(session.getTitle())
                .modelId(session.getModelId())
                .promptId(session.getPromptId())
                .systemMessage(session.getSystemMessage())
                .messageCount(session.getMessageCount())
                .isActive(session.getIsActive())
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
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * 发送消息并获取流式响应（SSE 格式）
     * <p>
     * 执行流程：
     * <ol>
     *   <li>使用 Mono.fromCallable 在 boundedElastic 线程池准备流式上下文</li>
     *   <li>调用 chatClient.stream() 获取响应 Flux</li>
     *   <li>逐块返回内容，累积完整响应</li>
     *   <li>完成后异步保存助手消息</li>
     * </ol>
     * </p>
     * <p>
     * 返回的 Flux 适合 SSE（Server-Sent Events）格式输出，
     * 每次响应包含一个内容片段。
     * </p>
     *
     * @param request 消息请求，包含会话ID、消息内容、可选模型ID
     * @return SSE 流式响应 Flux，每个元素为响应内容片段
     */
    @Override
    public Flux<String> sendMessageStream(ChatRequest request) {
        // 使用 Mono.fromCallable 包装阻塞操作，在 boundedElastic 线程池执行
        return Mono.fromCallable(() -> prepareStreamContext(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(context -> {
                    StringBuilder fullResponse = new StringBuilder();
                    return context.chatClient().stream(context.prompt())
                            .map(response -> {
                                String content = response.getResult().getOutput().getContent();
                                if (content != null) {
                                    fullResponse.append(content);
                                    return content;
                                }
                                return "";
                            })
                            .doOnComplete(() -> {
                                // 在 boundedElastic 线程池执行阻塞的保存操作
                                Mono.fromRunnable(() -> saveAssistantMessage(
                                                context.session(), context.modelConfig(), fullResponse.toString()))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe();
                            })
                            .doOnError(e -> {
                                log.error("Stream error: {}", e.getMessage());
                                Mono.fromRunnable(() -> saveAssistantMessage(
                                                context.session(), context.modelConfig(), ""))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe();
                            });
                });
    }

    /**
     * 准备流式上下文(阻塞操作,需在单独线程池执行)
     */
    private StreamContext prepareStreamContext(ChatRequest request) {
        // 使用公共方法获取 session 和 modelConfig
        SessionModelContext context = getSessionAndModelContext(request);
        ChatSession session = context.session();
        ModelConfig modelConfig = context.modelConfig();

        // 保存用户消息
        saveUserMessage(session, request.getContent());

        // 构建消息列表
        List<org.springframework.ai.chat.messages.Message> messages = buildMessageList(session, request.getContent());
        Prompt prompt = new Prompt(messages);
        OpenAiChatClient chatClient = buildChatClient(modelConfig);

        return new StreamContext(session, modelConfig, prompt, chatClient);
    }

    /**
     * 流式上下文，包含 session、modelConfig、prompt 和 chatClient
     */
    private record StreamContext(
            ChatSession session,
            ModelConfig modelConfig,
            Prompt prompt,
            OpenAiChatClient chatClient
    ) {
    }

    /**
     * 保存流式响应完成后生成的助手消息
     * <p>
     * 更新会话的消息计数和最后更新时间。
     * 如果响应内容为空，标记为错误消息。
     * </p>
     *
     * @param session    聊天会话实体
     * @param modelConfig 模型配置实体
     * @param content    助手响应内容
     */
    private void saveAssistantMessage(ChatSession session, ModelConfig modelConfig, String content) {
        ChatMessage assistantMessage = ChatMessage.builder()
                .id(idGenerator.generateId())
                .sessionId(session.getId())
                .role(MessageRole.ASSISTANT)
                .content(content)
                .modelName(modelConfig.getModelName())
                .isError(content.isEmpty())
                .errorMessage(content.isEmpty() ? "Stream failed" : null)
                .build();
        chatMessageRepository.save(assistantMessage);

        session.setMessageCount((int) chatMessageRepository.countBySessionId(session.getId()));
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
    }
}