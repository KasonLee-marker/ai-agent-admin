package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.RerankService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rerank 重排序服务实现类
 * <p>
 * 支持 Cohere 和 Jina Rerank API：
 * <ul>
 *   <li>Cohere Rerank API: POST /v1/rerank</li>
 *   <li>Jina Rerank API: POST /v1/rerank</li>
 * </ul>
 * </p>
 * <p>
 * API 调用格式：
 * <ul>
 *   <li>Cohere: {"model": "xxx", "query": "query", "documents": ["doc1", "doc2"], "top_n": 5}</li>
 *   <li>Jina: {"model": "xxx", "query": "query", "documents": ["doc1", "doc2"], "top_n": 5}</li>
 * </ul>
 * </p>
 *
 * @see RerankService
 * @see ModelConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RerankServiceImpl implements RerankService {

    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 对搜索结果进行重排序
     * <p>
     * 执行流程：
     * <ol>
     *   <li>验证输入参数</li>
     *   <li>提取候选结果的文本内容作为 documents</li>
     *   <li>根据模型供应商构建 API 请求</li>
     *   <li>调用 Rerank API</li>
     *   <li>解析响应中的 relevance_score</li>
     *   <li>更新结果的 score 字段</li>
     *   <li>按新分数降序排序，返回前 topK 个</li>
     * </ol>
     * </p>
     *
     * @param query        查询文本
     * @param results      原始搜索结果列表
     * @param rerankConfig Rerank 模型配置
     * @param topK         最终返回数量
     * @return 重排序后的结果列表
     */
    @Override
    public List<VectorSearchResult> rerank(String query, List<VectorSearchResult> results,
                                           ModelConfig rerankConfig, int topK) {
        if (query == null || query.isEmpty() || results == null || results.isEmpty()) {
            log.warn("Rerank skipped: empty query or results");
            return results != null ? results.stream().limit(topK).collect(Collectors.toList()) : new ArrayList<>();
        }

        if (rerankConfig == null) {
            log.warn("Rerank skipped: no rerank config provided");
            return results.stream().limit(topK).collect(Collectors.toList());
        }

        log.info("Reranking {} results for query '{}' using model {}", results.size(), query, rerankConfig.getName());

        try {
            // 1. 提取文本内容作为 documents
            List<String> documents = results.stream()
                    .map(r -> r.getContent() != null ? r.getContent() : "")
                    .collect(Collectors.toList());

            // 2. 根据供应商调用对应 API
            List<RerankResult> rerankResults;
            if (isCohere(rerankConfig)) {
                rerankResults = callCohereRerankApi(query, documents, rerankConfig, topK);
            } else if (isJina(rerankConfig)) {
                rerankResults = callJinaRerankApi(query, documents, rerankConfig, topK);
            } else {
                log.warn("Unknown rerank provider: {}, skipping rerank", rerankConfig.getProvider());
                return results.stream().limit(topK).collect(Collectors.toList());
            }

            // 3. 更新结果的 score 并排序
            List<VectorSearchResult> rerankedResults = new ArrayList<>();
            for (RerankResult rr : rerankResults) {
                if (rr.index < results.size()) {
                    VectorSearchResult original = results.get(rr.index);
                    // 创建新的结果对象，更新 score
                    VectorSearchResult updated = VectorSearchResult.builder()
                            .chunkId(original.getChunkId())
                            .documentId(original.getDocumentId())
                            .documentName(original.getDocumentName())
                            .chunkIndex(original.getChunkIndex())
                            .content(original.getContent())
                            .score(rr.relevanceScore) // 更新为 Rerank 分数
                            .metadata(original.getMetadata())
                            .build();
                    rerankedResults.add(updated);
                }
            }

            // 按分数降序排序
            rerankedResults.sort(Comparator.comparing(VectorSearchResult::getScore).reversed());

            log.info("Rerank completed: {} results returned", rerankedResults.size());
            return rerankedResults.stream().limit(topK).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Rerank failed: {}", e.getMessage(), e);
            // 失败时返回原始结果
            return results.stream().limit(topK).collect(Collectors.toList());
        }
    }

    /**
     * 判断是否为 Cohere Rerank Provider
     */
    private boolean isCohere(ModelConfig config) {
        return config.getProvider() == ModelProvider.COHERE_RERANK;
    }

    /**
     * 判断是否为 Jina Rerank Provider
     */
    private boolean isJina(ModelConfig config) {
        return config.getProvider() == ModelProvider.JINA_RERANK;
    }

    /**
     * 调用 Cohere Rerank API
     * <p>
     * Cohere API 格式：
     * <ul>
     *   <li>Endpoint: POST /v1/rerank</li>
     *   <li>Body: {"model": "xxx", "query": "query", "documents": ["doc1", "doc2"], "top_n": 5}</li>
     *   <li>Response: {"results": [{"index": 0, "relevance_score": 0.9}, ...]}</li>
     * </ul>
     * </p>
     *
     * @param query        查询文本
     * @param documents    文档列表
     * @param rerankConfig Rerank 模型配置
     * @param topN         返回数量
     * @return Rerank 结果列表
     */
    private List<RerankResult> callCohereRerankApi(String query, List<String> documents,
                                                   ModelConfig rerankConfig, int topN) throws Exception {
        String baseUrl = rerankConfig.getBaseUrl() != null ? rerankConfig.getBaseUrl()
                : rerankConfig.getProvider().getDefaultBaseUrl();
        String apiKey = encryptionService.decrypt(rerankConfig.getApiKey());
        String modelName = rerankConfig.getModelName();

        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        // Cohere Rerank API 请求体
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", modelName);
        bodyMap.put("query", query);
        bodyMap.put("documents", documents);
        bodyMap.put("top_n", Math.min(topN, documents.size()));

        String requestBody = objectMapper.writeValueAsString(bodyMap);

        log.debug("Calling Cohere Rerank API: baseUrl={}, model={}, docsCount={}", baseUrl, modelName, documents.size());

        String response = client.post()
                .uri("/v1/rerank")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseCohereResponse(response);
    }

    /**
     * 调用 Jina Rerank API
     * <p>
     * Jina API 格式：
     * <ul>
     *   <li>Endpoint: POST /v1/rerank</li>
     *   <li>Body: {"model": "xxx", "query": "query", "documents": ["doc1", "doc2"], "top_n": 5}</li>
     *   <li>Response: {"results": [{"index": 0, "relevance_score": 0.9}, ...]}</li>
     * </ul>
     * </p>
     *
     * @param query        查询文本
     * @param documents    文档列表
     * @param rerankConfig Rerank 模型配置
     * @param topN         返回数量
     * @return Rerank 结果列表
     */
    private List<RerankResult> callJinaRerankApi(String query, List<String> documents,
                                                 ModelConfig rerankConfig, int topN) throws Exception {
        String baseUrl = rerankConfig.getBaseUrl() != null ? rerankConfig.getBaseUrl()
                : rerankConfig.getProvider().getDefaultBaseUrl();
        String apiKey = encryptionService.decrypt(rerankConfig.getApiKey());
        String modelName = rerankConfig.getModelName();

        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        // Jina Rerank API 请求体（格式与 Cohere 类似）
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", modelName);
        bodyMap.put("query", query);
        bodyMap.put("documents", documents);
        bodyMap.put("top_n", Math.min(topN, documents.size()));

        String requestBody = objectMapper.writeValueAsString(bodyMap);

        log.debug("Calling Jina Rerank API: baseUrl={}, model={}, docsCount={}", baseUrl, modelName, documents.size());

        String response = client.post()
                .uri("/v1/rerank")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseJinaResponse(response);
    }

    /**
     * 解析 Cohere Rerank API 响应
     * <p>
     * Cohere 返回格式：
     * {"results": [{"index": 0, "relevance_score": 0.9}, {"index": 1, "relevance_score": 0.7}]}
     * </p>
     *
     * @param response API 响应 JSON
     * @return Rerank 结果列表
     */
    private List<RerankResult> parseCohereResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        List<RerankResult> results = new ArrayList<>();

        JsonNode resultsNode = root.path("results");
        if (resultsNode.isArray()) {
            for (JsonNode item : resultsNode) {
                int index = item.path("index").asInt();
                double relevanceScore = item.path("relevance_score").asDouble();
                results.add(new RerankResult(index, relevanceScore));
            }
        }

        log.debug("Parsed Cohere response: {} results", results.size());
        return results;
    }

    /**
     * 解析 Jina Rerank API 响应
     * <p>
     * Jina 返回格式：
     * {"results": [{"index": 0, "relevance_score": 0.9}, {"index": 1, "relevance_score": 0.7}]}
     * </p>
     *
     * @param response API 响应 JSON
     * @return Rerank 结果列表
     */
    private List<RerankResult> parseJinaResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        List<RerankResult> results = new ArrayList<>();

        JsonNode resultsNode = root.path("results");
        if (resultsNode.isArray()) {
            for (JsonNode item : resultsNode) {
                int index = item.path("index").asInt();
                double relevanceScore = item.path("relevance_score").asDouble();
                results.add(new RerankResult(index, relevanceScore));
            }
        }

        log.debug("Parsed Jina response: {} results", results.size());
        return results;
    }

    /**
     * Rerank 结果内部类
     */
    private record RerankResult(int index, double relevanceScore) {
    }
}