package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.RagChatResponse;
import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.DocumentService;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.RagService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RagServiceImpl implements RagService {

    private final DocumentService documentService;
    private final ModelConfigRepository modelConfigRepository;
    private final EncryptionService encryptionService;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个智能助手，请根据以下参考信息回答用户的问题。
            如果参考信息中没有相关内容，请诚实地说"根据现有信息无法回答该问题"。

            参考信息：
            {context}

            请基于以上参考信息回答用户问题。
            """;

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
        String systemPromptTemplate = request.getSystemPromptTemplate() != null
                ? request.getSystemPromptTemplate()
                : DEFAULT_SYSTEM_PROMPT;
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