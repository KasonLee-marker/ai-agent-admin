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
import java.util.List;

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
 * <pre>
 * POST /v1/embeddings
 * {
 *   "model": "text-embedding-ada-002",
 *   "input": "文本内容"
 * }
 * </pre>
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
     * 计算单个文本的 Embedding 向量
     * <p>
     * 执行流程：
     * <ol>
     *   <li>获取 embedding 模型配置</li>
     *   <li>调用 Embedding API</li>
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
        String baseUrl = embeddingConfig.getBaseUrl() != null
                ? embeddingConfig.getBaseUrl()
                : embeddingConfig.getProvider().getDefaultBaseUrl();
        String apiKey = encryptionService.decrypt(embeddingConfig.getApiKey());
        String modelName = embeddingConfig.getModelName();

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String requestBody = objectMapper.writeValueAsString(createMap(
                    "model", modelName,
                    "input", text
            ));

            String response = client.post()
                    .uri("/v1/embeddings")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseEmbeddingResponse(response);
        } catch (Exception e) {
            log.error("Failed to get embedding for text: {}", text.substring(0, Math.min(50, text.length())), e);
            throw new RuntimeException("Embedding API call failed: " + e.getMessage(), e);
        }
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
        String baseUrl = embeddingConfig.getBaseUrl() != null
                ? embeddingConfig.getBaseUrl()
                : embeddingConfig.getProvider().getDefaultBaseUrl();
        String apiKey = encryptionService.decrypt(embeddingConfig.getApiKey());
        String modelName = embeddingConfig.getModelName();

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String requestBody = objectMapper.writeValueAsString(createMap(
                    "model", modelName,
                    "input", texts
            ));

            String response = client.post()
                    .uri("/v1/embeddings")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseBatchEmbeddingResponse(response);
        } catch (Exception e) {
            log.error("Failed to get batch embeddings for {} texts", texts.size(), e);
            throw new RuntimeException("Batch embedding API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 计算两个向量的余弦相似度
     * <p>
     * 公式：cos(A, B) = (A · B) / (||A|| * ||B||)
     * </p>
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
     * 获取 Embedding 向量维度
     * <p>
     * 根据模型名称推断维度：
     * <ul>
     *   <li>text-embedding-3-large: 3072</li>
     *   <li>text-embedding-ada-002, text-embedding-3-small: 1536</li>
     *   <li>text-embedding-v1/v2/v3: 1024-1536</li>
     * </ul>
     * </p>
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
     * 获取 Embedding 模型配置
     * <p>
     * 查找策略：
     * <ol>
     *   <li>查找 OPENAI_EMBEDDING 或 DASHSCOPE_EMBEDDING 类型的配置</li>
     *   <li>如无专用配置，查找默认模型</li>
     *   <li>仍无配置，返回系统默认配置</li>
     * </ol>
     * </p>
     *
     * @return Embedding 模型配置
     */
    private ModelConfig getEmbeddingModelConfig() {
        // 查找专用 Embedding 配置
        List<ModelConfig> embeddingConfigs = modelConfigRepository.findByProviderInAndIsActiveTrue(
                List.of(ModelProvider.OPENAI_EMBEDDING, ModelProvider.DASHSCOPE_EMBEDDING));

        if (!embeddingConfigs.isEmpty()) {
            return embeddingConfigs.get(0);
        }

        // 查找默认模型（可能是 chat 模型，但有些也支持 embedding）
        ModelConfig defaultConfig = modelConfigRepository.findByIsDefaultTrueAndIsActiveTrue().orElse(null);
        if (defaultConfig != null) {
            log.warn("No dedicated embedding model configured, using default model: {}", defaultConfig.getName());
            return defaultConfig;
        }

        // 创建默认配置（兜底）
        log.warn("No embedding model configured, using system default: {}", DEFAULT_EMBEDDING_MODEL);
        return ModelConfig.builder()
                .id("system-default-embedding")
                .name("System Default Embedding")
                .provider(ModelProvider.OPENAI_EMBEDDING)
                .modelName(DEFAULT_EMBEDDING_MODEL)
                .baseUrl(DEFAULT_EMBEDDING_BASE_URL)
                .apiKey("") // 需要配置
                .build();
    }

    /**
     * 解析 Embedding API 响应
     *
     * @param response API 响应 JSON
     * @return Embedding 向量
     */
    private float[] parseEmbeddingResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode data = root.path("data");

        if (data.isArray() && data.size() > 0) {
            JsonNode embeddingNode = data.get(0).path("embedding");
            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            return embedding;
        }

        throw new RuntimeException("Invalid embedding response: no embedding data found");
    }

    /**
     * 解析批量 Embedding API 响应
     *
     * @param response API 响应 JSON
     * @return Embedding 向量列表
     */
    private List<float[]> parseBatchEmbeddingResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode data = root.path("data");

        List<float[]> embeddings = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                JsonNode embeddingNode = item.path("embedding");
                float[] embedding = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    embedding[i] = (float) embeddingNode.get(i).asDouble();
                }
                embeddings.add(embedding);
            }
        }

        return embeddings;
    }

    /**
     * 创建简单的 Map（用于构建 JSON 请求体）
     */
    private java.util.Map<String, Object> createMap(String k1, Object v1, String k2, Object v2) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
}