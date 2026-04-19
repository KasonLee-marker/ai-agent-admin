package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.entity.PromptTemplate;
import com.aiagent.admin.domain.enums.MessageRole;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.domain.repository.PromptTemplateRepository;
import com.aiagent.admin.service.DocumentService;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.RagService;
import com.aiagent.admin.service.RagSessionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检索增强生成（RAG）服务实现类
 * <p>
 * 提供基于文档检索的对话功能：
 * <ul>
 *   <li>向量相似度检索相关文档片段</li>
 *   <li>构建包含检索上下文的提示词</li>
 *   <li>调用 AI 模型生成回复</li>
 * </ul>
 * </p>
 * <p>
 * RAG 流程：
 * <ol>
 *   <li>将用户问题转换为查询</li>
 *   <li>在向量数据库中检索 topK 个相似文档片段</li>
 *   <li>将检索结果拼接成上下文文本</li>
 *   <li>使用系统提示词模板构建完整 Prompt</li>
 *   <li>调用模型生成最终回复</li>
 * </ol>
 * </p>
 *
 * @see RagService
 * @see DocumentService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagServiceImpl implements RagService {

    private final DocumentService documentService;
    private final ModelConfigRepository modelConfigRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final RagSessionService ragSessionService;
    private final EncryptionService encryptionService;

    /**
     * 默认系统提示词模板，用于构建 RAG 上下文
     */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个智能助手，请根据以下参考信息回答用户的问题。
            如果参考信息中没有相关内容，请诚实地说"根据现有信息无法回答该问题"。

            参考信息：
            {context}

            请基于以上参考信息回答用户问题。
            """;

    @Override
    public List<VectorSearchResult> retrieve(RagChatRequest request) {
        VectorSearchRequest searchRequest = getVectorSearchRequest(request);

        return documentService.searchSimilar(searchRequest);
    }

    @Override
    public RagChatResponse chat(RagChatRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. 获取或创建会话
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            // 创建新会话
            RagSessionDTO sessionDto = ragSessionService.createSession(
                    request.getKnowledgeBaseId(),
                    request.getModelId(),
                    request.getEmbeddingModelId(),
                    "anonymous"); // TODO: 从请求头获取用户标识
            sessionId = sessionDto.getId();
        }

        // 2. 保存用户消息
        ragSessionService.saveUserMessage(sessionId, request.getQuestion());

        // 3. 检索相关文档
        VectorSearchRequest searchRequest = getVectorSearchRequest(request);

        List<VectorSearchResult> sources = documentService.searchSimilar(searchRequest);

        // 4. 构建上下文
        String context = sources.stream()
                .map(VectorSearchResult::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 5. 构建系统提示词
        String systemPromptTemplate;
        if (request.getPromptTemplateId() != null && !request.getPromptTemplateId().isEmpty()) {
            PromptTemplate template = promptTemplateRepository.findById(request.getPromptTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("Prompt template not found"));
            systemPromptTemplate = template.getContent();
        } else if (request.getSystemPromptTemplate() != null && !request.getSystemPromptTemplate().isEmpty()) {
            systemPromptTemplate = request.getSystemPromptTemplate();
        } else {
            systemPromptTemplate = DEFAULT_SYSTEM_PROMPT;
        }
        String systemPrompt = systemPromptTemplate.replace("{context}", context);

        // 6. 获取模型配置
        ModelConfig modelConfig;
        if (request.getModelId() != null) {
            modelConfig = modelConfigRepository.findById(request.getModelId())
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + request.getModelId()));
        } else {
            modelConfig = modelConfigRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new IllegalStateException("No default model configured"));
        }

        // 7. 构建消息列表（包含历史）
        List<Message> messages = buildMessagesWithHistory(sessionId, systemPrompt, request.getQuestion());

        // 8. 调用 LLM
        OpenAiChatClient chatClient = buildChatClient(modelConfig);
        Prompt prompt = new Prompt(messages);
        String answer = chatClient.call(prompt).getResult().getOutput().getContent();

        long latency = System.currentTimeMillis() - startTime;

        // 9. 保存助手消息
        ragSessionService.saveAssistantMessage(sessionId, answer, sources, modelConfig.getModelName(), latency);

        return RagChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .sessionId(sessionId)
                .latencyMs(latency)
                .modelName(modelConfig.getModelName())
                .build();
    }

    /**
     * 获取向量检索请求
     *
     * @param request RAG 聊天请求
     * @return 向量检索请求
     */
    private static @NonNull VectorSearchRequest getVectorSearchRequest(RagChatRequest request) {
        VectorSearchRequest searchRequest = new VectorSearchRequest();
        searchRequest.setQuery(request.getQuestion());
        searchRequest.setTopK(request.getTopK() != null ? request.getTopK() : 5);
        searchRequest.setThreshold(request.getThreshold() != null ? request.getThreshold() : 0.5);
        searchRequest.setDocumentId(request.getDocumentId());
        searchRequest.setEmbeddingModelId(request.getEmbeddingModelId());
        searchRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());
        searchRequest.setStrategy(request.getStrategy());
        searchRequest.setEnableRerank(request.getEnableRerank());
        searchRequest.setRerankModelId(request.getRerankModelId());
        return searchRequest;
    }

    /**
     * 构建包含历史消息的消息列表
     *
     * @param sessionId    会话 ID
     * @param systemPrompt 系统提示词
     * @param userContent  当前用户问题
     * @return 消息列表
     */
    private List<Message> buildMessagesWithHistory(String sessionId, String systemPrompt, String userContent) {
        List<Message> messages = new ArrayList<>();

        // 系统消息
        messages.add(new SystemMessage(systemPrompt));

        // 获取历史消息（最多 10 条）
        List<RagMessageDTO> history = ragSessionService.getHistory(sessionId, 10);

        // 添加历史消息（排除最后一条，因为那是刚才保存的用户消息）
        for (int i = 0; i < history.size() - 1; i++) {
            RagMessageDTO msg = history.get(i);
            if (msg.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        // 当前用户消息
        messages.add(new UserMessage(userContent));

        return messages;
    }

    /**
     * 构建 OpenAI ChatClient
     * <p>
     * 根据模型配置创建 OpenAiApi 和 OpenAiChatClient 实例。
     * 配置参数包括：baseUrl、apiKey、modelName、temperature、maxTokens。
     * </p>
     *
     * @param config 模型配置实体
     * @return 配置好的 OpenAiChatClient 实例
     */
    private OpenAiChatClient buildChatClient(ModelConfig config) {
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl()
                : config.getProvider().getDefaultBaseUrl();

        OpenAiApi api = new OpenAiApi(baseUrl, apiKey);

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .withModel(config.getModelName());

        if (config.getTemperature() != null) {
            optionsBuilder.withTemperature(config.getTemperature().floatValue());
        }
        if (config.getMaxTokens() != null) {
            optionsBuilder.withMaxTokens(config.getMaxTokens());
        }

        return new OpenAiChatClient(api, optionsBuilder.build());
    }

    @Override
    public Flux<String> chatStream(RagChatRequest request) {
        // 使用 Mono.fromCallable 包装阻塞操作，在 boundedElastic 线程池执行
        return Mono.fromCallable(() -> prepareStreamContext(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(context -> {
                    StringBuilder fullResponse = new StringBuilder();

                    // 首先发送 sessionId（格式: SESSION_ID:xxx）
                    Flux<String> sessionIdFlux = Flux.just("SESSION_ID:" + context.sessionId());

                    // 然后发送内容流
                    Flux<String> contentFlux = context.chatClient().stream(context.prompt())
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
                                Mono.fromRunnable(() -> saveStreamAssistantMessage(
                                                context.sessionId(),
                                                fullResponse.toString(),
                                                context.sources(),
                                                context.modelConfig()))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe();
                            })
                            .doOnError(e -> {
                                log.error("RAG stream error: {}", e.getMessage());
                                Mono.fromRunnable(() -> saveStreamAssistantMessage(
                                                context.sessionId(),
                                                "",
                                                context.sources(),
                                                context.modelConfig()))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe();
                            });

                    // 合并 sessionId 和内容流
                    return Flux.concat(sessionIdFlux, contentFlux);
                });
    }

    /**
     * 准备流式对话上下文（支持多轮）
     *
     * @param request RAG 对话请求
     * @return 流式上下文（包含 sessionId、chatClient、prompt、sources、modelConfig）
     */
    private StreamContext prepareStreamContext(RagChatRequest request) {
        // 1. 获取或创建会话
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            RagSessionDTO sessionDto = ragSessionService.createSession(
                    request.getKnowledgeBaseId(),
                    request.getModelId(),
                    request.getEmbeddingModelId(),
                    "anonymous");
            sessionId = sessionDto.getId();
        }

        // 2. 保存用户消息
        ragSessionService.saveUserMessage(sessionId, request.getQuestion());

        // 3. 检索相关文档
        VectorSearchRequest searchRequest = getVectorSearchRequest(request);

        List<VectorSearchResult> sources = documentService.searchSimilar(searchRequest);

        // 4. 构建上下文
        String context = sources.stream()
                .map(VectorSearchResult::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 5. 构建系统提示词
        String systemPromptTemplate;
        if (request.getPromptTemplateId() != null && !request.getPromptTemplateId().isEmpty()) {
            PromptTemplate template = promptTemplateRepository.findById(request.getPromptTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("Prompt template not found"));
            systemPromptTemplate = template.getContent();
        } else if (request.getSystemPromptTemplate() != null && !request.getSystemPromptTemplate().isEmpty()) {
            systemPromptTemplate = request.getSystemPromptTemplate();
        } else {
            systemPromptTemplate = DEFAULT_SYSTEM_PROMPT;
        }
        String systemPrompt = systemPromptTemplate.replace("{context}", context);

        // 6. 获取模型配置
        ModelConfig modelConfig;
        if (request.getModelId() != null) {
            modelConfig = modelConfigRepository.findById(request.getModelId())
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + request.getModelId()));
        } else {
            modelConfig = modelConfigRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new IllegalStateException("No default model configured"));
        }

        // 7. 创建 ChatClient 和消息列表（包含历史）
        OpenAiChatClient chatClient = buildChatClient(modelConfig);
        List<Message> messages = buildMessagesWithHistory(sessionId, systemPrompt, request.getQuestion());
        Prompt prompt = new Prompt(messages);

        return new StreamContext(sessionId, chatClient, prompt, sources, modelConfig);
    }

    /**
     * 保存流式响应完成后的助手消息
     *
     * @param sessionId   会话 ID
     * @param content     AI 回答内容
     * @param sources     检索来源
     * @param modelConfig 模型配置
     */
    private void saveStreamAssistantMessage(String sessionId, String content,
                                            List<VectorSearchResult> sources, ModelConfig modelConfig) {
        ragSessionService.saveAssistantMessage(
                sessionId,
                content,
                sources,
                modelConfig.getModelName(),
                null); // 流式响应不记录延迟
    }

    /**
     * 流式上下文内部类（扩展支持 sessionId 和 modelConfig）
     */
    private record StreamContext(
            String sessionId,
            OpenAiChatClient chatClient,
            Prompt prompt,
            List<VectorSearchResult> sources,
            ModelConfig modelConfig
    ) {
    }
}