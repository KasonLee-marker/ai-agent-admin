package com.aiagent.admin.service;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 模型健康检查服务
 * <p>
 * 通过实际调用模型 API 来验证连接状态：
 * <ul>
 *   <li>CHAT 模型：发送简单测试消息验证 API 连通性</li>
 *   <li>EMBEDDING 模型：发送文本计算向量验证 API 连通性</li>
 *   <li>获取 Embedding 维度并创建对应的向量存储表</li>
 *   <li>更新模型健康状态（HEALTHY/UNHEALTHY）</li>
 *   <li>记录最后健康检查时间和向量维度</li>
 * </ul>
 * </p>
 *
 * @see ModelConfig
 * @see VectorTableService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final ModelConfigRepository modelConfigRepository;
    private final EncryptionService encryptionService;
    private final VectorTableService vectorTableService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试消息内容（CHAT 模型）
     */
    private static final String TEST_PROMPT = "Hello";

    /**
     * 测试文本内容（EMBEDDING 模型）
     */
    private static final String TEST_TEXT = "test";

    /**
     * 对单个模型执行健康检查
     * <p>
     * 根据模型类型选择不同的测试方式：
     * <ul>
     *   <li>CHAT 模型：调用 chat completions API</li>
     *   <li>EMBEDDING 模型：调用 embeddings API，获取维度并建表</li>
     * </ul>
     * </p>
     *
     * @param modelId 模型配置唯一标识
     * @return true 表示健康，false 表示不健康
     */
    @Transactional
    public boolean healthCheck(String modelId) {
        try {
            ModelConfig config = modelConfigRepository.findById(modelId)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

            if (!Boolean.TRUE.equals(config.getIsActive())) {
                log.warn("Model {} is not active, skipping health check", modelId);
                config.setHealthStatus(ModelConfig.HealthStatus.UNHEALTHY);
                config.setLastHealthCheck(LocalDateTime.now());
                modelConfigRepository.save(config);
                return false;
            }

            log.info("Performing health check for model: {} ({}) type={}",
                    config.getName(), config.getModelName(), config.getProvider().getModelType());

            boolean isHealthy;
            if (config.getProvider().getModelType() == ModelProvider.ModelType.EMBEDDING) {
                isHealthy = testEmbeddingApiConnection(config);
            } else {
                isHealthy = testChatApiConnection(config);
            }

            // 统一更新健康状态（维度和表名已在 testEmbeddingApiConnection 中设置）
            config.setHealthStatus(isHealthy ? ModelConfig.HealthStatus.HEALTHY : ModelConfig.HealthStatus.UNHEALTHY);
            config.setLastHealthCheck(LocalDateTime.now());
            modelConfigRepository.save(config);

            log.info("Health check for model {}: {}", modelId, isHealthy ? "HEALTHY" : "UNHEALTHY");
            return isHealthy;

        } catch (Exception e) {
            log.error("Health check error for model {}: {}", modelId, e.getMessage());
            updateHealthStatus(modelId, ModelConfig.HealthStatus.UNHEALTHY);
            return false;
        }
    }

    /**
     * 测试 CHAT 模型 API 连接
     *
     * @param config 模型配置实体
     * @return true 表示连接成功，false 表示连接失败
     */
    private boolean testChatApiConnection(ModelConfig config) {
        try {
            String apiKey = encryptionService.decrypt(config.getApiKey());
            String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() :
                    config.getProvider().getDefaultBaseUrl();

            log.debug("Testing CHAT API: baseUrl={}, model={}", baseUrl, config.getModelName());

            OpenAiApi api = new OpenAiApi(baseUrl, apiKey);

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(config.getModelName())
                    .withMaxTokens(10)
                    .build();

            OpenAiChatClient client = new OpenAiChatClient(api, options);

            Prompt prompt = new Prompt(new UserMessage(TEST_PROMPT));
            client.call(prompt);

            log.info("CHAT API test successful for model: {}", config.getModelName());
            return true;
        } catch (Exception e) {
            log.error("CHAT API test failed for model {}: {}", config.getModelName(), e.getMessage());
            return false;
        }
    }

    /**
     * 测试 EMBEDDING 模型 API 连接
     * <p>
     * 执行流程：
     * <ol>
     *   <li>调用 Embedding API 发送测试文本</li>
     *   <li>解析响应，验证是否有 embedding 数据</li>
     *   <li>获取向量维度</li>
     *   <li>调用 VectorTableService 创建对应的向量存储表</li>
     *   <li>更新 ModelConfig 的 embeddingDimension 和 embeddingTableName</li>
     * </ol>
     * </p>
     * <p>
     * API 格式差异：
     * <ul>
     *   <li>OpenAI: POST /v1/embeddings, body: {"model": "xxx", "input": "text"}</li>
     *   <li>DashScope: POST /services/embeddings/text-embedding/text-embedding,
     *       body: {"model": "xxx", "input": {"texts": ["text"]}}</li>
     * </ul>
     * </p>
     *
     * @param config 模型配置实体
     * @return true 表示连接成功，false 表示连接失败
     */
    private boolean testEmbeddingApiConnection(ModelConfig config) {
        try {
            String apiKey = encryptionService.decrypt(config.getApiKey());
            String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() :
                    config.getProvider().getDefaultBaseUrl();

            log.debug("Testing EMBEDDING API: baseUrl={}, model={}, provider={}",
                    baseUrl, config.getModelName(), config.getProvider());

            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String requestBody;
            String uriPath;

            // DashScope 使用不同的 API 格式
            if (config.getProvider() == ModelProvider.DASHSCOPE_EMBEDDING) {
                uriPath = "/services/embeddings/text-embedding/text-embedding";
                requestBody = objectMapper.writeValueAsString(Map.of(
                        "model", config.getModelName(),
                        "input", Map.of("texts", List.of(TEST_TEXT)),
                        "parameters", Map.of("text_type", "document")
                ));
            } else {
                // OpenAI 格式
                uriPath = "/v1/embeddings";
                requestBody = objectMapper.writeValueAsString(Map.of(
                        "model", config.getModelName(),
                        "input", TEST_TEXT
                ));
            }

            String response = client.post()
                    .uri(uriPath)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // 解析响应
            JsonNode root = objectMapper.readTree(response);

            // 获取 embedding 数据和维度
            JsonNode embeddingNode = null;
            if (root.has("output")) {
                // DashScope 返回格式：output.embeddings[0].embedding
                JsonNode embeddings = root.path("output").path("embeddings");
                if (embeddings.isArray() && embeddings.size() > 0) {
                    embeddingNode = embeddings.get(0).path("embedding");
                }
            } else if (root.has("data")) {
                // OpenAI 返回格式：data[0].embedding
                JsonNode data = root.path("data");
                if (data.isArray() && data.size() > 0) {
                    embeddingNode = data.get(0).path("embedding");
                }
            }

            if (embeddingNode == null || !embeddingNode.isArray() || embeddingNode.size() == 0) {
                log.error("EMBEDDING API response invalid for model {}: {}", config.getModelName(), response);
                return false;
            }

            // 获取向量维度
            int dimension = embeddingNode.size();
            log.info("EMBEDDING API test successful for model: {}, dimension: {}", config.getModelName(), dimension);

            // 创建对应的向量存储表
            String tableName = vectorTableService.ensureTableExists(dimension);
            log.info("Created/verified vector table: {} for dimension {}", tableName, dimension);

            // 设置 ModelConfig 的维度和表名（不单独保存，由 healthCheck 方法统一保存）
            config.setEmbeddingDimension(dimension);
            config.setEmbeddingTableName(tableName);

            return true;
        } catch (Exception e) {
            log.error("EMBEDDING API test failed for model {}: {}", config.getModelName(), e.getMessage());
            return false;
        }
    }

    /**
     * 更新模型健康状态
     *
     * @param modelId 模型配置唯一标识
     * @param status  健康状态（HEALTHY/UNHEALTHY）
     */
    private void updateHealthStatus(String modelId, ModelConfig.HealthStatus status) {
        modelConfigRepository.findById(modelId).ifPresent(config -> {
            config.setHealthStatus(status);
            config.setLastHealthCheck(LocalDateTime.now());
            modelConfigRepository.save(config);
        });
    }

    /**
     * 批量检查所有活跃模型
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