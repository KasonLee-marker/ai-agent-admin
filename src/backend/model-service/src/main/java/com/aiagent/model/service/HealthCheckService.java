package com.aiagent.model.service;

import com.aiagent.model.domain.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final ModelService modelService;
    private final ModelConfigService modelConfigService;

    private static final String HEALTH_CHECK_PROMPT = "Hi";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * Perform health check on a model
     *
     * @param modelId the model ID to check
     * @return true if healthy, false otherwise
     */
    public boolean healthCheck(String modelId) {
        try {
            ModelConfig config = modelConfigService.findEntityById(modelId)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

            if (!Boolean.TRUE.equals(config.getIsActive())) {
                log.warn("Model {} is not active, skipping health check", modelId);
                modelConfigService.updateHealthStatus(modelId, ModelConfig.HealthStatus.UNHEALTHY);
                return false;
            }

            log.debug("Performing health check for model: {}", modelId);

            ChatClient chatClient = modelService.getChatClient(modelId);

            // Perform async health check with timeout
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    ChatResponse response = chatClient.prompt()
                            .user(HEALTH_CHECK_PROMPT)
                            .call()
                            .chatResponse();

                    boolean hasContent = response != null &&
                            response.getResult() != null &&
                            response.getResult().getOutput() != null &&
                            response.getResult().getOutput().getText() != null;

                    return hasContent;
                } catch (Exception e) {
                    log.error("Health check failed for model {}: {}", modelId, e.getMessage());
                    return false;
                }
            });

            boolean isHealthy = future.get(TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            ModelConfig.HealthStatus status = isHealthy ?
                    ModelConfig.HealthStatus.HEALTHY : ModelConfig.HealthStatus.UNHEALTHY;

            modelConfigService.updateHealthStatus(modelId, status);

            log.info("Health check for model {}: {}", modelId, status);
            return isHealthy;

        } catch (Exception e) {
            log.error("Health check error for model {}: {}", modelId, e.getMessage());
            modelConfigService.updateHealthStatus(modelId, ModelConfig.HealthStatus.UNHEALTHY);
            return false;
        }
    }

    /**
     * Perform health check on all active models
     */
    public void healthCheckAll() {
        modelConfigService.findByFilters(null, true, null).forEach(model -> {
            try {
                healthCheck(model.getId());
            } catch (Exception e) {
                log.error("Failed to health check model {}: {}", model.getId(), e.getMessage());
            }
        });
    }
}
