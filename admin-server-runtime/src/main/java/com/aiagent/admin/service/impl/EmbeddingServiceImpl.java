package com.aiagent.admin.service.impl;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.EmbeddingService;
import com.aiagent.admin.service.EncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding 服务实现类
 * <p>
 * 使用 OpenAI Compatible API 计算文本向量：
 * <ul>
 *   <li>支持 OpenAI 和 DashScope Embedding API</li>
 *   <li>复用 ModelConfig 配置 embedding 模型</li>
 *   <li>支持单文本和批量计算</li>
 *   <li>实现余弦相似度计算</li>
 * </ul>
 * </p>
 * <p>
 * API 调用格式：
 * <ul>
 *   <li>OpenAI: POST /v1/embeddings, body: {"model": "xxx", "input": "text"}</li>
 *   <li>DashScope: POST /services/embeddings/text-embedding/text-embedding,
 *       body: {"model": "xxx", "input": {"texts": ["text"]}, "parameters": {"text_type": "document"}}</li>
 * </ul>
 * </p>
 *
 * @see EmbeddingService
 * @see ModelConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    private final ModelConfigRepository modelConfigRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 默认 Embedding 模型（当没有配置专用 embedding 模型时使用）
     */
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-ada-002";
    private static final String DEFAULT_EMBEDDING_BASE_URL = "https://api.openai.com";

    /**
     * 测试文本内容（用于 DashScope）
     */
    private static final String TEST_TEXT = "test";

    /**
     * 计算单个文本的 Embedding 向量
     * <p>
     * 执行流程：
     * <ol>
     *   <li>获取 embedding 模型配置</li>
     *   <li>调用 Embedding API（区分 OpenAI 和 DashScope）</li>
     *   <li>解析返回的向量数据</li>
     * </ol>
     * </p>
     *
     * @param text 输入文本
     * @return Embedding 向量
     */
    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[0];
        }

        ModelConfig embeddingConfig = getEmbeddingModelConfig();
        return callEmbeddingApi(text, embeddingConfig);
    }

    /**
     * 批量计算文本的 Embedding 向量
     * <p>
     * 使用批量 API 提高效率。一次请求最多支持 2048 个文本。
     * </p>
     *
     * @param texts 输入文本列表
     * @return Embedding 向量列表
     */
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        ModelConfig embeddingConfig = getEmbeddingModelConfig();

        // DashScope 不支持标准批量格式，需要逐个处理或使用特殊格式
        if (isDashScope(embeddingConfig)) {
            // DashScope 批量格式: input = {"texts": [...]}
            return callDashScopeBatchEmbeddingApi(texts, embeddingConfig);
        } else {
            // OpenAI 标准批量格式
            return callOpenAiBatchEmbeddingApi(texts, embeddingConfig);
        }
    }

    /**
     * 使用指定的模型配置批量计算文本的 Embedding 向量
     * <p>
     * 不查找默认配置，直接使用传入的模型配置。
     * 用于文档向量化时指定特定的 embedding 模型。
     * </p>
     *
     * @param texts        输入文本列表
     * @param modelConfig  模型配置实体
     * @return Embedding 向量列表
     */
    @Override
    public List<float[]> embedBatchWithModel(List<String> texts, ModelConfig modelConfig) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        // 直接使用传入的模型配置
        if (isDashScope(modelConfig)) {
            return callDashScopeBatchEmbeddingApi(texts, modelConfig);
        } else {
            return callOpenAiBatchEmbeddingApi(texts, modelConfig);
        }
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度值（0-1）
     */
    @Override
    public float cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return 0f;
        }

        float dotProduct = 0f;
        float norm1 = 0f;
        float norm2 = 0f;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0f;
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 计算两个文本的语义相似度
     *
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度值（0-1）
     */
    @Override
    public float semanticSimilarity(String text1, String text2) {
        float[] embedding1 = embed(text1);
        float[] embedding2 = embed(text2);
        return cosineSimilarity(embedding1, embedding2);
    }

    /**
     * 使用指定的模型配置计算单个文本的 Embedding 向量
     *
     * @param text        输入文本
     * @param modelConfig 模型配置实体
     * @return Embedding 向量
     */
    @Override
    public float[] embedWithModel(String text, ModelConfig modelConfig) {
        if (text == null || text.isEmpty()) {
            return new float[0];
        }
        return callEmbeddingApi(text, modelConfig);
    }

    /**
     * 使用指定的模型配置计算两个文本的语义相似度
     *
     * @param text1       文本1
     * @param text2       文本2
     * @param modelConfig 模型配置实体
     * @return 相似度值（0-1）
     */
    @Override
    public float semanticSimilarityWithModel(String text1, String text2, ModelConfig modelConfig) {
        float[] embedding1 = embedWithModel(text1, modelConfig);
        float[] embedding2 = embedWithModel(text2, modelConfig);
        return cosineSimilarity(embedding1, embedding2);
    }

    /**
     * 获取 Embedding 向量维度
     *
     * @return 向量维度
     */
    @Override
    public int getEmbeddingDimension() {
        ModelConfig config = getEmbeddingModelConfig();
        String modelName = config.getModelName();

        if (modelName.contains("large")) {
            return 3072;
        } else if (modelName.contains("v1") || modelName.contains("v3")) {
            return 1024;
        } else {
            return 1536; // 默认维度
        }
    }

    /**
     * 判断是否为 DashScope Embedding Provider
     */
    private boolean isDashScope(ModelConfig config) {
        return config.getProvider() == ModelProvider.DASHSCOPE_EMBEDDING;
    }

    /**
     * 调用 Embedding API（自动区分 OpenAI 和 DashScope）
     */
    private float[] callEmbeddingApi(String text, ModelConfig config) {
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : config.getProvider().getDefaultBaseUrl();
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String modelName = config.getModelName();

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String uriPath;
            String requestBody;

            if (isDashScope(config)) {
                // DashScope 格式
                uriPath = "/services/embeddings/text-embedding/text-embedding";
                Map<String, Object> bodyMap = new HashMap<>();
                bodyMap.put("model", modelName);
                bodyMap.put("input", Map.of("texts", List.of(text)));
                bodyMap.put("parameters", Map.of("text_type", "document"));
                requestBody = objectMapper.writeValueAsString(bodyMap);
            } else {
                // OpenAI 格式
                uriPath = "/v1/embeddings";
                requestBody = objectMapper.writeValueAsString(Map.of(
                        "model", modelName,
                        "input", text
                ));
            }

            log.debug("Calling embedding API: baseUrl={}, uriPath={}, model={}", baseUrl, uriPath, modelName);

            String response = client.post()
                    .uri(uriPath)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseEmbeddingResponse(response, isDashScope(config));
        } catch (Exception e) {
            log.error("Failed to get embedding for text (length {}): {}", text.length(), e.getMessage());
            throw new RuntimeException("Embedding API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 调用 OpenAI 批量 Embedding API
     */
    private List<float[]> callOpenAiBatchEmbeddingApi(List<String> texts, ModelConfig config) {
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : config.getProvider().getDefaultBaseUrl();
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String modelName = config.getModelName();

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", modelName,
                    "input", texts
            ));

            String response = client.post()
                    .uri("/v1/embeddings")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseBatchEmbeddingResponse(response, false);
        } catch (Exception e) {
            log.error("Failed to get batch embeddings for {} texts: {}", texts.size(), e.getMessage());
            throw new RuntimeException("Batch embedding API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 调用 DashScope 批量 Embedding API
     * <p>
     * DashScope 批量格式：input = {"texts": ["text1", "text2", ...]}
     * </p>
     */
    private List<float[]> callDashScopeBatchEmbeddingApi(List<String> texts, ModelConfig config) {
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : config.getProvider().getDefaultBaseUrl();
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String modelName = config.getModelName();

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("model", modelName);
            bodyMap.put("input", Map.of("texts", texts));
            bodyMap.put("parameters", Map.of("text_type", "document"));

            String requestBody = objectMapper.writeValueAsString(bodyMap);

            log.debug("Calling DashScope batch embedding API: baseUrl={}, model={}, textsCount={}", baseUrl, modelName, texts.size());

            String response = client.post()
                    .uri("/services/embeddings/text-embedding/text-embedding")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseBatchEmbeddingResponse(response, true);
        } catch (Exception e) {
            log.error("Failed to get DashScope batch embeddings for {} texts: {}", texts.size(), e.getMessage());
            throw new RuntimeException("DashScope batch embedding API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 Embedding API 响应
     *
     * @param response     API 响应 JSON
     * @param isDashScope  是否为 DashScope 格式
     * @return Embedding 向量
     */
    private float[] parseEmbeddingResponse(String response, boolean isDashScope) throws Exception {
        JsonNode root = objectMapper.readTree(response);

        if (isDashScope) {
            // DashScope 返回格式：output.embeddings[0].embedding
            JsonNode embeddings = root.path("output").path("embeddings");
            if (embeddings.isArray() && embeddings.size() > 0) {
                JsonNode embeddingNode = embeddings.get(0).path("embedding");
                return parseEmbeddingArray(embeddingNode);
            }
        } else {
            // OpenAI 返回格式：data[0].embedding
            JsonNode data = root.path("data");
            if (data.isArray() && data.size() > 0) {
                JsonNode embeddingNode = data.get(0).path("embedding");
                return parseEmbeddingArray(embeddingNode);
            }
        }

        throw new RuntimeException("Invalid embedding response: no embedding data found. Response: " + response);
    }

    /**
     * 解析批量 Embedding API 响应
     *
     * @param response     API 响应 JSON
     * @param isDashScope  是否为 DashScope 格式
     * @return Embedding 向量列表
     */
    private List<float[]> parseBatchEmbeddingResponse(String response, boolean isDashScope) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        List<float[]> embeddings = new ArrayList<>();

        if (isDashScope) {
            // DashScope 返回格式：output.embeddings[].embedding
            JsonNode embeddingsNode = root.path("output").path("embeddings");
            if (embeddingsNode.isArray()) {
                for (JsonNode item : embeddingsNode) {
                    JsonNode embeddingNode = item.path("embedding");
                    embeddings.add(parseEmbeddingArray(embeddingNode));
                }
            }
        } else {
            // OpenAI 返回格式：data[].embedding
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    JsonNode embeddingNode = item.path("embedding");
                    embeddings.add(parseEmbeddingArray(embeddingNode));
                }
            }
        }

        if (embeddings.isEmpty()) {
            throw new RuntimeException("Invalid batch embedding response: no embedding data found. Response: " + response);
        }

        return embeddings;
    }

    /**
     * 解析 embedding 数组节点
     */
    private float[] parseEmbeddingArray(JsonNode embeddingNode) {
        float[] embedding = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            embedding[i] = (float) embeddingNode.get(i).asDouble();
        }
        return embedding;
    }

    /**
     * 获取 Embedding 模型配置
     * <p>
     * 查找策略（优先级从高到低）：
     * <ol>
     *   <li>查找明确标记为 isDefaultEmbedding 的模型</li>
     *   <li>查找 OPENAI_EMBEDDING 或 DASHSCOPE_EMBEDDING 类型的激活配置</li>
     *   <li>如无专用配置，查找默认 Chat 模型（会记录警告）</li>
     *   <li>仍无配置，返回系统默认配置（需要用户配置 API Key）</li>
     * </ol>
     * </p>
     *
     * @return Embedding 模型配置
     */
    private ModelConfig getEmbeddingModelConfig() {
        // 1. 优先查找明确标记为默认 Embedding 的模型
        ModelConfig defaultEmbedding = modelConfigRepository.findByIsDefaultEmbeddingTrue()
                .orElse(null);
        if (defaultEmbedding != null && Boolean.TRUE.equals(defaultEmbedding.getIsActive())) {
            log.debug("Using default embedding model: {}", defaultEmbedding.getName());
            return defaultEmbedding;
        }

        // 2. 查找专用 Embedding Provider 的激活配置
        List<ModelConfig> embeddingConfigs = modelConfigRepository.findByProviderInAndIsActiveTrue(
                List.of(ModelProvider.OPENAI_EMBEDDING, ModelProvider.DASHSCOPE_EMBEDDING));

        if (!embeddingConfigs.isEmpty()) {
            ModelConfig config = embeddingConfigs.get(0);
            log.debug("Using embedding provider config: {}", config.getName());
            return config;
        }

        // 3. 查找默认 Chat 模型（有些 Chat 模型也支持 embedding）
        ModelConfig defaultConfig = modelConfigRepository.findByIsDefaultTrueAndIsActiveTrue().orElse(null);
        if (defaultConfig != null) {
            log.warn("No dedicated embedding model configured, using default chat model: {}. " +
                    "Please configure an embedding model for better performance.", defaultConfig.getName());
            return defaultConfig;
        }

        // 4. 创建系统默认配置（兜底方案，需要用户配置 API Key）
        log.warn("No embedding model configured, using system default: {}. " +
                "Please configure an embedding model in Model Management.", DEFAULT_EMBEDDING_MODEL);
        return ModelConfig.builder()
                .id("system-default-embedding")
                .name("System Default Embedding")
                .provider(ModelProvider.OPENAI_EMBEDDING)
                .modelName(DEFAULT_EMBEDDING_MODEL)
                .baseUrl(DEFAULT_EMBEDDING_BASE_URL)
                .apiKey("") // 需要用户配置
                .build();
    }
}