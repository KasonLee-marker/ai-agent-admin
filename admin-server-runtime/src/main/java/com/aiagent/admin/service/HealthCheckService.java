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
 * AI 模型健康检查服务
 * <p>
 * 通过实际调用模型 API 来验证连接状态：
 * <ul>
 *   <li>发送简单测试消息验证 API 连通性</li>
 *   <li>更新模型健康状态（HEALTHY/UNHEALTHY）</li>
 *   <li>记录最后健康检查时间</li>
 *   <li>批量检查所有活跃模型</li>
 * </ul>
 * </p>
 * <p>
 * 健康检查通过发送 "Hello" 测试消息来验证 API 可用性。
 * 检查失败时自动更新模型状态为 UNHEALTHY。
 * </p>
 *
 * @see ModelConfig
 * @see EncryptionService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final ModelConfigRepository modelConfigRepository;
    private final EncryptionService encryptionService;

    /**
     * 测试消息内容
     */
    private static final String TEST_PROMPT = "Hello";

    /**
     * 对单个模型执行健康检查
     * <p>
     * 执行流程：
     * <ol>
     *   <li>验证模型存在且活跃</li>
     *   <li>调用模型 API 发送测试消息</li>
     *   <li>根据响应结果更新健康状态</li>
     * </ol>
     * </p>
     *
     * @param modelId 模型配置唯一标识
     * @return true 表示健康，false 表示不健康
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
     * <p>
     * 创建 OpenAiChatClient 并发送简单的测试消息。
     * 使用最小 Token 数（10）来降低测试成本。
     * </p>
     *
     * @param config 模型配置实体
     * @return true 表示连接成功，false 表示连接失败
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

    /**
     * 更新模型健康状态
     * <p>
     * 同时更新健康状态和最后检查时间。
     * </p>
     *
     * @param modelId 模型配置唯一标识
     * @param status  健康状态（HEALTHY/UNHEALTHY）
     */
    private void updateHealthStatus(String modelId, ModelConfig.HealthStatus status) {
        modelConfigRepository.findById(modelId).ifPresent(config -> {
            config.setHealthStatus(status);
            config.setLastHealthCheck(java.time.LocalDateTime.now());
            modelConfigRepository.save(config);
        });
    }

    /**
     * 批量检查所有活跃模型
     * <p>
     * 遍历所有活跃模型并执行健康检查。
     * 检查过程中捕获异常避免影响后续模型检查。
     * </p>
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