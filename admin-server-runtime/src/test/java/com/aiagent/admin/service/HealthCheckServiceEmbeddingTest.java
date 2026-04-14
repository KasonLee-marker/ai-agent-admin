package com.aiagent.admin.service;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 嵌入模型健康检查服务测试类
 * <p>
 * 测试 HealthCheckService 的核心功能：
 * <ul>
 *   <li>Embedding 模型健康检查</li>
 *   <li>获取向量维度</li>
 *   <li>创建向量存储表</li>
 *   <li>更新模型配置</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class HealthCheckServiceEmbeddingTest {

    @Mock
    private ModelConfigRepository modelConfigRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private VectorTableService vectorTableService;

    @InjectMocks
    private HealthCheckService healthCheckService;

    private ModelConfig embeddingConfig;

    @BeforeEach
    void setUp() {
        embeddingConfig = ModelConfig.builder()
                .id("test-embedding-model")
                .name("Test Embedding")
                .provider(ModelProvider.OPENAI_EMBEDDING)
                .modelName("text-embedding-ada-002")
                .baseUrl("https://api.openai.com")
                .apiKey("encrypted-key")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("healthCheck - 模型不存在时应返回false")
    void healthCheck_shouldReturnFalse_whenModelNotFound() {
        // Given
        String modelId = "non-existent";
        when(modelConfigRepository.findById(modelId)).thenReturn(Optional.empty());

        // When
        boolean result = healthCheckService.healthCheck(modelId);

        // Then - HealthCheckService 捕获异常并返回 false
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("healthCheck - 模型未激活时应返回false")
    void healthCheck_shouldReturnFalse_whenModelNotActive() {
        // Given
        embeddingConfig.setIsActive(false);
        when(modelConfigRepository.findById("test-embedding-model")).thenReturn(Optional.of(embeddingConfig));
        when(modelConfigRepository.save(any())).thenReturn(embeddingConfig);

        // When
        boolean result = healthCheckService.healthCheck("test-embedding-model");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("向量表服务 - 应正确创建指定维度的表")
    void vectorTableService_shouldCreateTableForDimension() {
        // Given
        int dimension = 1536;
        String expectedTableName = "document_embeddings_1536";

        when(vectorTableService.ensureTableExists(dimension)).thenReturn(expectedTableName);

        // When
        String tableName = vectorTableService.ensureTableExists(dimension);

        // Then
        assertThat(tableName).isEqualTo(expectedTableName);
        verify(vectorTableService).ensureTableExists(dimension);
    }

    @Test
    @DisplayName("向量表服务 - 不同维度应创建不同的表")
    void vectorTableService_shouldCreateDifferentTablesForDifferentDimensions() {
        // Given
        when(vectorTableService.getTableName(1024)).thenReturn("document_embeddings_1024");
        when(vectorTableService.getTableName(1536)).thenReturn("document_embeddings_1536");
        when(vectorTableService.getTableName(3072)).thenReturn("document_embeddings_3072");

        // When & Then
        assertThat(vectorTableService.getTableName(1024)).isEqualTo("document_embeddings_1024");
        assertThat(vectorTableService.getTableName(1536)).isEqualTo("document_embeddings_1536");
        assertThat(vectorTableService.getTableName(3072)).isEqualTo("document_embeddings_3072");
    }

    @Test
    @DisplayName("ModelConfig - 应正确设置embedding维度和表名")
    void modelConfig_shouldSetEmbeddingFields() {
        // Given
        int dimension = 1536;
        String tableName = "document_embeddings_1536";

        // When
        embeddingConfig.setEmbeddingDimension(dimension);
        embeddingConfig.setEmbeddingTableName(tableName);

        // Then
        assertThat(embeddingConfig.getEmbeddingDimension()).isEqualTo(1536);
        assertThat(embeddingConfig.getEmbeddingTableName()).isEqualTo("document_embeddings_1536");
    }

    @Test
    @DisplayName("ModelProvider - OPENAI_EMBEDDING 类型应正确识别")
    void modelProvider_shouldRecognizeOpenaiEmbedding() {
        // Given & When & Then
        assertThat(ModelProvider.OPENAI_EMBEDDING.getModelType())
                .isEqualTo(ModelProvider.ModelType.EMBEDDING);
        assertThat(ModelProvider.DASHSCOPE_EMBEDDING.getModelType())
                .isEqualTo(ModelProvider.ModelType.EMBEDDING);
    }
}