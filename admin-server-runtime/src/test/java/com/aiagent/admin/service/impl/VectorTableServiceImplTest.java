package com.aiagent.admin.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 向量表管理服务测试类
 * <p>
 * 测试 VectorTableServiceImpl 的核心功能：
 * <ul>
 *   <li>创建向量表</li>
 *   <li>检查表是否存在</li>
 *   <li>获取表名</li>
 *   <li>获取已存在的维度列表</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class VectorTableServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private VectorTableServiceImpl vectorTableService;

    @BeforeEach
    void setUp() {
        // Mock pgvector extension creation - 使用 lenient() 处理 void 方法，避免 UnnecessaryStubbingException
        lenient().doNothing().when(jdbcTemplate).execute("CREATE EXTENSION IF NOT EXISTS vector");
    }

    @Test
    @DisplayName("ensureTableExists - 表不存在时应创建新表")
    void ensureTableExists_shouldCreateTable_whenNotExists() {
        // Given
        int dimension = 1536;
        String expectedTableName = "document_embeddings_1536";

        // Mock 表不存在
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(expectedTableName)))
                .thenReturn(false);

        // Mock 创建表和索引 - void 方法用 doNothing
        doNothing().when(jdbcTemplate).execute(anyString());

        // When
        String tableName = vectorTableService.ensureTableExists(dimension);

        // Then
        assertThat(tableName).isEqualTo(expectedTableName);
        verify(jdbcTemplate).execute("CREATE EXTENSION IF NOT EXISTS vector");
        verify(jdbcTemplate).queryForObject(anyString(), eq(Boolean.class), eq(expectedTableName));
        verify(jdbcTemplate, atLeast(1)).execute(anyString()); // 创建表 + 索引
    }

    @Test
    @DisplayName("ensureTableExists - 表存在时应跳过创建")
    void ensureTableExists_shouldSkipCreation_whenTableExists() {
        // Given
        int dimension = 1536;
        String expectedTableName = "document_embeddings_1536";

        // Mock 表已存在
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(expectedTableName)))
                .thenReturn(true);

        // When
        String tableName = vectorTableService.ensureTableExists(dimension);

        // Then
        assertThat(tableName).isEqualTo(expectedTableName);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Boolean.class), eq(expectedTableName));
        verify(jdbcTemplate, never()).execute(contains("CREATE TABLE")); // 不应创建表
    }

    @Test
    @DisplayName("getTableName - 应返回正确格式表名")
    void getTableName_shouldReturnCorrectFormat() {
        // Given & When & Then
        assertThat(vectorTableService.getTableName(1024)).isEqualTo("document_embeddings_1024");
        assertThat(vectorTableService.getTableName(1536)).isEqualTo("document_embeddings_1536");
        assertThat(vectorTableService.getTableName(3072)).isEqualTo("document_embeddings_3072");
    }

    @Test
    @DisplayName("tableExists - 应正确检查表是否存在")
    void tableExists_shouldCheckCorrectly() {
        // Given
        int dimension = 1536;
        String tableName = "document_embeddings_1536";

        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(tableName)))
                .thenReturn(true);

        // When
        boolean exists = vectorTableService.tableExists(dimension);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("tableExists - 表不存在时应返回false")
    void tableExists_shouldReturnFalse_whenNotExists() {
        // Given
        int dimension = 1024;
        String tableName = "document_embeddings_1024";

        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(tableName)))
                .thenReturn(false);

        // When
        boolean exists = vectorTableService.tableExists(dimension);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("getExistingDimensions - 应返回已存在的维度列表")
    void getExistingDimensions_shouldReturnListOfExistingDimensions() {
        // Given
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(List.of("document_embeddings_1536", "document_embeddings_3072"));

        // When
        List<Integer> dimensions = vectorTableService.getExistingDimensions();

        // Then
        assertThat(dimensions).containsExactly(1536, 3072);
    }

    @Test
    @DisplayName("dropTable - 应删除指定的向量表")
    void dropTable_shouldDropTable() {
        // Given
        int dimension = 1536;
        String tableName = "document_embeddings_1536";

        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(tableName)))
                .thenReturn(true);
        doNothing().when(jdbcTemplate).execute(anyString());

        // When
        vectorTableService.dropTable(dimension);

        // Then
        verify(jdbcTemplate).execute(contains("DROP TABLE"));
    }
}