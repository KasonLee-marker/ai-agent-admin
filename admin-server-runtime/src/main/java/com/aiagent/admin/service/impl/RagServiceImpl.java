package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.RagChatResponse;
import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.entity.PromptTemplate;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.domain.repository.PromptTemplateRepository;
import com.aiagent.admin.service.DocumentService;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.RagService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

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

    /**
     * 执行 RAG 对话
     * <p>
     * 执行流程：
     * <ol>
     *   <li>检索相关文档片段（向量相似度搜索）</li>
     *   <li>构建上下文文本</li>
     *   <li>生成系统提示词</li>
     *   <li>获取模型配置</li>
     *   <li>调用 LLM 生成回复</li>
     * </ol>
     * </p>
     *
     * @param request RAG 对话请求，包含问题、可选模型ID、topK 等
     * @return RAG 对话响应，包含回答、来源文档、延迟等
     */
    @Override
    public RagChatResponse chat(RagChatRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Retrieve relevant documents
        VectorSearchRequest searchRequest = new VectorSearchRequest();
        searchRequest.setQuery(request.getQuestion());
        searchRequest.setTopK(request.getTopK() != null ? request.getTopK() : 5);
        searchRequest.setDocumentId(request.getDocumentId());

        List<VectorSearchResult> sources = documentService.searchSimilar(searchRequest);

        // 2. Build context from retrieved documents
        String context = sources.stream()
                .map(VectorSearchResult::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 3. Build system prompt
        // 优先级：promptTemplateId > systemPromptTemplate > DEFAULT_SYSTEM_PROMPT
        String systemPromptTemplate;
        if (request.getPromptTemplateId() != null && !request.getPromptTemplateId().isEmpty()) {
            PromptTemplate template = promptTemplateRepository.findById(request.getPromptTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("Prompt template not found with id: " + request.getPromptTemplateId()));
            systemPromptTemplate = template.getContent();
        } else if (request.getSystemPromptTemplate() != null && !request.getSystemPromptTemplate().isEmpty()) {
            systemPromptTemplate = request.getSystemPromptTemplate();
        } else {
            systemPromptTemplate = DEFAULT_SYSTEM_PROMPT;
        }
        String systemPrompt = systemPromptTemplate.replace("{context}", context);

        // 4. Get model configuration
        ModelConfig modelConfig;
        if (request.getModelId() != null) {
            modelConfig = modelConfigRepository.findById(request.getModelId())
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + request.getModelId()));
        } else {
            modelConfig = modelConfigRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new IllegalStateException("No default model configured"));
        }

        // 5. Call LLM
        OpenAiChatClient chatClient = buildChatClient(modelConfig);
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(request.getQuestion())
        ));

        String answer = chatClient.call(prompt).getResult().getOutput().getContent();

        long latency = System.currentTimeMillis() - startTime;

        return RagChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .latencyMs(latency)
                .modelName(modelConfig.getModelName())
                .build();
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
}