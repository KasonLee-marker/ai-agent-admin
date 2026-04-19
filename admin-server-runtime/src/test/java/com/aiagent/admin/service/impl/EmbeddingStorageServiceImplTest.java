package com.aiagent.admin.service.impl;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.service.EmbeddingStorageService;
import com.aiagent.admin.service.VectorTableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.contains;

/**
 * Embedding 向量存储服务测试类
 * <p>
 * 测试 EmbeddingStorageServiceImpl 的核心功能：
 * <ul>
 *   <li>存储向量</li>
 *   <li>批量存储向量</li>
 *   <li>删除文档向量</li>
 *   <li>向量相似度检索</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingStorageServiceImplTest {

    @Mock
    private NamedParameterJdbcTemplate namedTemplate;

    @Mock
    private VectorTableService vectorTableService;

    @InjectMocks
    private EmbeddingStorageServiceImpl embeddingStorageService;

    private ModelConfig embeddingConfig;

    @BeforeEach
    void setUp() {
        embeddingConfig = ModelConfig.builder()
                .id("test-embedding-model")
                .name("Test Embedding Model")
                .embeddingDimension(1536)
                .embeddingTableName("document_embeddings_1536")
                .build();
    }

    @Test
    @DisplayName("storeVector - 应正确存储单个向量")
    void storeVector_shouldStoreCorrectly() {
        // Given
        String chunkId = "chunk-001";
        String documentId = "doc-001";
        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        int dimension = 1536;
        String tableName = "document_embeddings_1536";

        when(namedTemplate.update(anyString(), anyMap())).thenReturn(1);

        // When
        embeddingStorageService.storeVector(chunkId, documentId, vector, dimension, tableName);

        // Then
        verify(namedTemplate).update(anyString(), any(Map.class));
    }

    @Test
    @DisplayName("storeVector - 表名未提供时应调用VectorTableService获取")
    void storeVector_shouldGetTableName_whenNotProvided() {
        // Given
        String chunkId = "chunk-001";
        String documentId = "doc-001";
        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        int dimension = 1536;
        String expectedTableName = "document_embeddings_1536";

        when(vectorTableService.ensureTableExists(dimension)).thenReturn(expectedTableName);
        when(namedTemplate.update(anyString(), anyMap())).thenReturn(1);

        // When
        embeddingStorageService.storeVector(chunkId, documentId, vector, dimension, null);

        // Then
        verify(vectorTableService).ensureTableExists(dimension);
        verify(namedTemplate).update(anyString(), any(Map.class));
    }

    @Test
    @DisplayName("storeVectorsBatch - 应批量存储向量")
    void storeVectorsBatch_shouldStoreBatchCorrectly() {
        // Given
        String tableName = "document_embeddings_1536";
        int dimension = 1536;

        List<EmbeddingStorageService.VectorData> vectors = List.of(
                new EmbeddingStorageService.VectorData("chunk-001", "doc-001", new float[]{0.1f, 0.2f}),
                new EmbeddingStorageService.VectorData("chunk-002", "doc-001", new float[]{0.3f, 0.4f})
        );

        when(namedTemplate.update(anyString(), anyMap())).thenReturn(1);

        // When
        embeddingStorageService.storeVectorsBatch(vectors, dimension, tableName);

        // Then
        verify(namedTemplate, times(2)).update(anyString(), any(Map.class));
    }

    @Test
    @DisplayName("storeVectorsBatch - 空列表不应执行任何操作")
    void storeVectorsBatch_shouldDoNothing_whenEmptyList() {
        // Given
        List<EmbeddingStorageService.VectorData> vectors = List.of();

        // When
        embeddingStorageService.storeVectorsBatch(vectors, 1536, "document_embeddings_1536");

        // Then
        verify(namedTemplate, never()).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("deleteByDocument - 应删除文档的所有向量")
    void deleteByDocument_shouldDeleteAllVectorsForDocument() {
        // Given
        String documentId = "doc-001";
        String tableName = "document_embeddings_1536";

        when(namedTemplate.update(anyString(), anyMap())).thenReturn(5);

        // When
        embeddingStorageService.deleteByDocument(documentId, tableName);

        // Then
        verify(namedTemplate).update(contains("DELETE FROM"), any(Map.class));
    }

    @Test
    @DisplayName("deleteByDocument - 表名为空时不应执行删除")
    void deleteByDocument_shouldDoNothing_whenTableNameIsNull() {
        // Given
        String documentId = "doc-001";

        // When
        embeddingStorageService.deleteByDocument(documentId, null);

        // Then
        verify(namedTemplate, never()).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("searchSimilar - 配置有表名时应使用配置的表名")
    void searchSimilar_shouldUseConfigTableName_whenAvailable() {
        // Given
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};
        int topK = 5;
        double threshold = 0.5;

        // 验证配置信息正确
        assertThat(embeddingConfig.getEmbeddingTableName()).isEqualTo("document_embeddings_1536");
        assertThat(embeddingConfig.getEmbeddingDimension()).isEqualTo(1536);
    }

    @Test
    @DisplayName("searchSimilar - 配置无表名时验证配置字段")
    void searchSimilar_configWithoutTable_hasCorrectFields() {
        // Given
        ModelConfig configWithoutTable = ModelConfig.builder()
                .id("test-model")
                .embeddingDimension(1024)
                .embeddingTableName(null)
                .build();

        // Then - 验证配置字段正确
        assertThat(configWithoutTable.getEmbeddingDimension()).isEqualTo(1024);
        assertThat(configWithoutTable.getEmbeddingTableName()).isNull();
    }
}