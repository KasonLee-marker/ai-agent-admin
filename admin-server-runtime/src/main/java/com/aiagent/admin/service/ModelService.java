package com.aiagent.admin.service;

import com.aiagent.admin.domain.entity.ModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model Service for managing AI model clients.
 * Note: Spring AI integration is disabled in this version to avoid dependency issues.
 * To enable Spring AI, add the following dependencies:
 * - spring-ai-openai-spring-boot-starter
 * - spring-ai-ollama-spring-boot-starter
 * - spring-ai-azure-openai-spring-boot-starter
 */
@Slf4j
@Service
public class ModelService {

    private final ModelConfigService modelConfigService;
    private final EncryptionService encryptionService;

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
     * Get current active model ID
     */
    public String getActiveModelId() {
        return activeModelId;
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

        this.activeModelId = modelId;
        log.info("Switched to model: {} ({})", config.getName(), modelId);
    }

    /**
     * Clear cache for a specific model (useful after config updates)
     * Note: Currently a no-op since Spring AI is disabled
     */
    public void clearCache(String modelId) {
        log.debug("Cleared cache for model: {}", modelId);
    }

    /**
     * Clear all caches
     * Note: Currently a no-op since Spring AI is disabled
     */
    public void clearAllCaches() {
        log.debug("Cleared all model caches");
    }
}
