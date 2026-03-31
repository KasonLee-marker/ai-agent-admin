package com.aiagent.model.service;

import com.aiagent.model.domain.entity.ModelConfig;
import com.aiagent.model.domain.entity.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ModelService {

    private final ModelConfigService modelConfigService;
    private final EncryptionService encryptionService;

    // Cache for ChatClient instances
    private final Map<String, ChatClient> chatClientCache = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    // Current active model ID
    private volatile String activeModelId;

    public ModelService(ModelConfigService modelConfigService, EncryptionService encryptionService) {
        this.modelConfigService = modelConfigService;
        this.encryptionService = encryptionService;

        // Initialize with default model
        modelConfigService.findDefaultEntity().ifPresent(config -> {
            this.activeModelId = config.getId();
            log.info("Initialized with default model: {}", config.getId());
        });
    }

    /**
     * Get ChatClient for the specified model ID
     */
    public ChatClient getChatClient(String modelId) {
        if (modelId == null) {
            return getDefaultChatClient();
        }

        return chatClientCache.computeIfAbsent(modelId, id -> {
            ModelConfig config = modelConfigService.findEntityById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
            return createChatClient(config);
        });
    }

    /**
     * Get default ChatClient
     */
    public ChatClient getDefaultChatClient() {
        String defaultId = activeModelId != null ? activeModelId :
                modelConfigService.findDefaultEntity()
                        .map(ModelConfig::getId)
                        .orElseThrow(() -> new IllegalStateException("No default model configured"));
        return getChatClient(defaultId);
    }

    /**
     * Get EmbeddingModel for the specified model ID
     */
    public EmbeddingModel getEmbeddingModel(String modelId) {
        if (modelId == null) {
            return getDefaultEmbeddingModel();
        }

        return embeddingModelCache.computeIfAbsent(modelId, id -> {
            ModelConfig config = modelConfigService.findEntityById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
            return createEmbeddingModel(config);
        });
    }

    /**
     * Get default EmbeddingModel
     */
    public EmbeddingModel getDefaultEmbeddingModel() {
        String defaultId = activeModelId != null ? activeModelId :
                modelConfigService.findDefaultEntity()
                        .map(ModelConfig::getId)
                        .orElseThrow(() -> new IllegalStateException("No default model configured"));
        return getEmbeddingModel(defaultId);
    }

    /**
     * Switch to a different model at runtime
     */
    public void switchModel(String modelId) {
        ModelConfig config = modelConfigService.findEntityById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        if (!Boolean.TRUE.equals(config.getIsActive())) {
            throw new IllegalStateException("Model is not active: " + modelId);
        }

        // Pre-warm the cache
        getChatClient(modelId);

        this.activeModelId = modelId;
        log.info("Switched to model: {} ({})", config.getName(), modelId);
    }

    /**
     * Get current active model ID
     */
    public String getActiveModelId() {
        return activeModelId;
    }

    /**
     * Clear cache for a specific model (useful after config updates)
     */
    public void clearCache(String modelId) {
        chatClientCache.remove(modelId);
        embeddingModelCache.remove(modelId);
        log.debug("Cleared cache for model: {}", modelId);
    }

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        chatClientCache.clear();
        embeddingModelCache.clear();
        log.debug("Cleared all model caches");
    }

    private ChatClient createChatClient(ModelConfig config) {
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : config.getProvider().getDefaultBaseUrl();

        ChatModel chatModel = switch (config.getProvider()) {
            case OPENAI, DASHSCOPE, DEEPSEEK -> {
                OpenAiApi api = new OpenAiApi(baseUrl, apiKey);
                yield new OpenAiChatModel(api);
            }
            case OLLAMA -> {
                OllamaApi api = new OllamaApi(baseUrl);
                yield OllamaChatModel.builder()
                        .ollamaApi(api)
                        .defaultOptions(org.springframework.ai.ollama.api.OllamaOptions.builder()
                                .model(config.getModelName())
                                .build())
                        .build();
            }
        };

        return ChatClient.builder(chatModel)
                .defaultOptions(buildOptions(config))
                .build();
    }

    private EmbeddingModel createEmbeddingModel(ModelConfig config) {
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : config.getProvider().getDefaultBaseUrl();

        return switch (config.getProvider()) {
            case OPENAI, DASHSCOPE, DEEPSEEK -> {
                OpenAiApi api = new OpenAiApi(baseUrl, apiKey);
                yield new OpenAiEmbeddingModel(api);
            }
            case OLLAMA -> {
                OllamaApi api = new OllamaApi(baseUrl);
                yield new OllamaEmbeddingModel(api,
                        org.springframework.ai.ollama.api.OllamaOptions.builder()
                                .model(config.getModelName())
                                .build(),
                        null, null);
            }
        };
    }

    private org.springframework.ai.chat.prompt.ChatOptions buildOptions(ModelConfig config) {
        var builder = org.springframework.ai.openai.OpenAiChatOptions.builder()
                .model(config.getModelName());

        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature().floatValue());
        }
        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }
        if (config.getTopP() != null) {
            builder.topP(config.getTopP().floatValue());
        }

        return builder.build();
    }
}
