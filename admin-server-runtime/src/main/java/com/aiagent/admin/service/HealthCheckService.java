package com.aiagent.admin.service;

import com.aiagent.admin.domain.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Health Check Service for AI models.
 * Note: Spring AI integration is disabled in this version.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final ModelConfigService modelConfigService;

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * Perform health check on a model
     * Note: Currently returns true for active models since Spring AI is disabled
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
            
            // Since Spring AI is disabled, we just mark active models as healthy
            modelConfigService.updateHealthStatus(modelId, ModelConfig.HealthStatus.HEALTHY);
            log.info("Health check for model {}: HEALTHY (Spring AI disabled)", modelId);
            return true;

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
