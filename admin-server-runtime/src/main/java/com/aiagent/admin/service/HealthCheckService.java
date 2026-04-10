package com.aiagent.admin.service;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

/**
 * Health Check Service for AI models.
 * Actually tests the API connection by sending a simple prompt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final ModelConfigRepository modelConfigRepository;
    private final EncryptionService encryptionService;

    private static final String TEST_PROMPT = "Hello";

    /**
     * Perform health check on a model by actually testing the API
     *
     * @param modelId the model ID to check
     * @return true if healthy, false otherwise
     */
    public boolean healthCheck(String modelId) {
        try {
            ModelConfig config = modelConfigRepository.findById(modelId)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

            if (!Boolean.TRUE.equals(config.getIsActive())) {
                log.warn("Model {} is not active, skipping health check", modelId);
                updateHealthStatus(modelId, ModelConfig.HealthStatus.UNHEALTHY);
                return false;
            }

            log.info("Performing health check for model: {} ({})", config.getName(), config.getModelName());

            boolean isHealthy = testApiConnection(config);

            updateHealthStatus(modelId, isHealthy ? ModelConfig.HealthStatus.HEALTHY : ModelConfig.HealthStatus.UNHEALTHY);
            log.info("Health check for model {}: {}", modelId, isHealthy ? "HEALTHY" : "UNHEALTHY");
            return isHealthy;

        } catch (Exception e) {
            log.error("Health check error for model {}: {}", modelId, e.getMessage());
            updateHealthStatus(modelId, ModelConfig.HealthStatus.UNHEALTHY);
            return false;
        }
    }

    /**
     * 实际测试 API 连接
     */
    private boolean testApiConnection(ModelConfig config) {
        try {
            String apiKey = encryptionService.decrypt(config.getApiKey());
            String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() :
                    config.getProvider().getDefaultBaseUrl();

            log.debug("Testing API: baseUrl={}, model={}", baseUrl, config.getModelName());

            OpenAiApi api = new OpenAiApi(baseUrl, apiKey);

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(config.getModelName())
                    .withMaxTokens(10)
                    .build();

            OpenAiChatClient client = new OpenAiChatClient(api, options);

            Prompt prompt = new Prompt(new UserMessage(TEST_PROMPT));
            client.call(prompt);

            log.info("API test successful for model: {}", config.getModelName());
            return true;
        } catch (Exception e) {
            log.error("API test failed for model {}: {}", config.getModelName(), e.getMessage());
            return false;
        }
    }

    private void updateHealthStatus(String modelId, ModelConfig.HealthStatus status) {
        modelConfigRepository.findById(modelId).ifPresent(config -> {
            config.setHealthStatus(status);
            config.setLastHealthCheck(java.time.LocalDateTime.now());
            modelConfigRepository.save(config);
        });
    }

    /**
     * Perform health check on all active models
     */
    public void healthCheckAll() {
        modelConfigRepository.findByIsActiveTrue().forEach(config -> {
            try {
                healthCheck(config.getId());
            } catch (Exception e) {
                log.error("Failed to health check model {}: {}", config.getId(), e.getMessage());
            }
        });
    }
}