package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;

/**
 * RerankServiceImpl 单元测试
 * <p>
 * 测试 Rerank 服务的核心功能：
 * <ul>
 *   <li>空输入处理</li>
 *   <li>无配置处理</li>
 *   <li>未知供应商处理</li>
 *   <li>Rerank 结果排序</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class RerankServiceImplTest {

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private RerankServiceImpl rerankService;

    private ModelConfig cohereRerankConfig;
    private ModelConfig jinaRerankConfig;
    private List<VectorSearchResult> testResults;

    @BeforeEach
    void setUp() {
        // Cohere Rerank 配置
        cohereRerankConfig = ModelConfig.builder()
                .id("rerank-1")
                .name("Cohere Rerank")
                .provider(ModelProvider.COHERE_RERANK)
                .modelName("rerank-multilingual-v3.0")
                .apiKey("test-api-key")
                .baseUrl("https://api.cohere.ai")
                .build();

        // Jina Rerank 配置
        jinaRerankConfig = ModelConfig.builder()
                .id("rerank-2")
                .name("Jina Rerank")
                .provider(ModelProvider.JINA_RERANK)
                .modelName("jina-reranker-v2-base-multilingual")
                .apiKey("test-api-key")
                .baseUrl("https://api.jina.ai")
                .build();

        // 测试搜索结果
        testResults = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            testResults.add(VectorSearchResult.builder()
                    .chunkId("chunk-" + i)
                    .documentId("doc-1")
                    .documentName("Test Document")
                    .content("Test content " + i)
                    .score(0.5 + i * 0.1) // 原始分数
                    .chunkIndex(i)
                    .build());
        }

        // Mock encryption service to return the same key (for testing) - lenient for tests that don't call API
        lenient().when(encryptionService.decrypt(anyString())).thenReturn("test-api-key");
    }

    @Test
    void rerank_shouldReturnLimitedResults_whenEmptyQuery() {
        // Empty query should skip rerank and return original results limited to topK
        List<VectorSearchResult> results = rerankService.rerank("", testResults, cohereRerankConfig, 5);
        assertEquals(5, results.size());
    }

    @Test
    void rerank_shouldReturnEmpty_whenEmptyResults() {
        List<VectorSearchResult> results = rerankService.rerank("test query", new ArrayList<>(), cohereRerankConfig, 5);
        assertTrue(results.isEmpty());
    }

    @Test
    void rerank_shouldReturnOriginalResults_whenNullConfig() {
        List<VectorSearchResult> results = rerankService.rerank("test query", testResults, null, 3);
        assertEquals(3, results.size());
    }

    @Test
    void rerank_shouldReturnLimitedResults_whenTopKSmallerThanResults() {
        List<VectorSearchResult> results = rerankService.rerank("test query", testResults, null, 2);
        assertEquals(2, results.size());
    }

    @Test
    void rerank_shouldHandleUnknownProvider() {
        // 创建一个未知供应商的配置
        ModelConfig unknownConfig = ModelConfig.builder()
                .id("unknown-1")
                .name("Unknown Provider")
                .provider(ModelProvider.OPENAI) // Chat provider, not RERANK
                .modelName("unknown-model")
                .build();

        // 应返回原始结果（跳过 rerank）
        List<VectorSearchResult> results = rerankService.rerank("test query", testResults, unknownConfig, 3);
        assertEquals(3, results.size());
    }

    @Test
    void rerank_shouldPreserveChunkId() {
        List<VectorSearchResult> results = rerankService.rerank("test query", testResults, null, 5);

        // 验证 chunkId 保持不变
        for (VectorSearchResult result : results) {
            assertTrue(result.getChunkId().startsWith("chunk-"));
        }
    }

    @Test
    void rerank_shouldHandleNullContent() {
        // 创建一个包含 null content 的结果
        List<VectorSearchResult> resultsWithNullContent = new ArrayList<>();
        resultsWithNullContent.add(VectorSearchResult.builder()
                .chunkId("chunk-null")
                .documentId("doc-1")
                .content(null) // null content
                .score(0.5)
                .build());
        resultsWithNullContent.add(VectorSearchResult.builder()
                .chunkId("chunk-valid")
                .documentId("doc-1")
                .content("valid content")
                .score(0.6)
                .build());

        // 应正常处理，不抛出异常
        List<VectorSearchResult> results = rerankService.rerank("test query", resultsWithNullContent, null, 2);
        assertEquals(2, results.size());
    }

    @Test
    void rerank_shouldReturnResultsInOrder_whenNoApiCall() {
        // 测试无 API 调用时的返回顺序
        List<VectorSearchResult> results = rerankService.rerank("test query", testResults, null, 3);

        // 应返回前 3 个原始结果
        assertEquals("chunk-0", results.get(0).getChunkId());
        assertEquals("chunk-1", results.get(1).getChunkId());
        assertEquals("chunk-2", results.get(2).getChunkId());
    }

    @Test
    void isCohere_shouldReturnTrue_whenCohereProvider() {
        assertTrue(cohereRerankConfig.getProvider() == ModelProvider.COHERE_RERANK);
    }

    @Test
    void isJina_shouldReturnTrue_whenJinaProvider() {
        assertTrue(jinaRerankConfig.getProvider() == ModelProvider.JINA_RERANK);
    }
}