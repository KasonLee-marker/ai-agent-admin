package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.service.EmbeddingStorageService;
import com.aiagent.admin.service.VectorTableService;
import com.aiagent.admin.service.mapper.VectorSearchResultRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding 向量存储服务实现类
 * <p>
 * 使用 JdbcTemplate 操作 pgvector 向量表，
 * 实现向量的存储、删除和检索功能。
 * </p>
 *
 * @see EmbeddingStorageService
 * @see VectorTableService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingStorageServiceImpl implements EmbeddingStorageService {

    private final NamedParameterJdbcTemplate namedTemplate;
    private final VectorTableService vectorTableService;

    private static final VectorSearchResultRowMapper RESULT_ROW_MAPPER = new VectorSearchResultRowMapper();

    @Override
    @Transactional
    public void storeVector(String chunkId, String documentId, float[] vector, int dimension, String tableName) {
        tableName = resolveTableNameForInsert(dimension, tableName);
        String vectorStr = formatVector(vector);

        String sql = String.format("""
                INSERT INTO %s (chunk_id, document_id, embedding, created_at)
                VALUES (:chunkId, :documentId, :vector::vector, CURRENT_TIMESTAMP)
                ON CONFLICT (chunk_id) DO UPDATE SET
                    embedding = EXCLUDED.embedding,
                    created_at = CURRENT_TIMESTAMP
                """, tableName);

        namedTemplate.update(sql, Map.of("chunkId", chunkId, "documentId", documentId, "vector", vectorStr));
        log.debug("Stored vector for chunk {} in table {}", chunkId, tableName);
    }

    @Override
    @Transactional
    public void storeVectorsBatch(List<VectorData> vectors, int dimension, String tableName) {
        if (vectors == null || vectors.isEmpty()) {
            return;
        }

        tableName = resolveTableNameForInsert(dimension, tableName);

        for (VectorData data : vectors) {
            String vectorStr = formatVector(data.vector());
            String sql = String.format("""
                    INSERT INTO %s (chunk_id, document_id, embedding, created_at)
                    VALUES (:chunkId, :documentId, :vector::vector, CURRENT_TIMESTAMP)
                    ON CONFLICT (chunk_id) DO UPDATE SET
                        embedding = EXCLUDED.embedding,
                        created_at = CURRENT_TIMESTAMP
                    """, tableName);

            namedTemplate.update(sql, Map.of("chunkId", data.chunkId(), "documentId", data.documentId(), "vector", vectorStr));
        }

        log.debug("Stored {} vectors in table {}", vectors.size(), tableName);
    }

    @Override
    @Transactional
    public void deleteByDocument(String documentId, String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            log.warn("Cannot delete vectors without table name for document {}", documentId);
            return;
        }

        String sql = String.format("DELETE FROM %s WHERE document_id = :documentId", tableName);
        int deleted = namedTemplate.update(sql, Map.of("documentId", documentId));
        log.debug("Deleted {} vectors from table {} for document {}", deleted, tableName, documentId);
    }

    /**
     * 解析插入操作的表名
     */
    private String resolveTableNameForInsert(int dimension, String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return vectorTableService.ensureTableExists(dimension);
        }
        return tableName;
    }

    @Override
    public List<VectorSearchResult> searchSimilar(float[] queryVector, ModelConfig embeddingConfig,
                                                  String documentId, String knowledgeBaseId, int topK, double threshold) {
        String tableName = resolveTableName(embeddingConfig);
        if (tableName == null) {
            log.warn("No embedding table available for model {}", embeddingConfig.getName());
            return List.of();
        }

        String queryVectorStr = formatVector(queryVector);
        Map<String, Object> params = buildSearchParams(queryVectorStr, documentId, knowledgeBaseId, threshold, topK);
        String sql = buildSearchSql(tableName, documentId, knowledgeBaseId);

        List<VectorSearchResult> results = namedTemplate.query(sql, params, RESULT_ROW_MAPPER);
        log.debug("Found {} similar vectors in table {} with threshold {}", results.size(), tableName, threshold);
        return results;
    }

    /**
     * 解析向量表名
     */
    private String resolveTableName(ModelConfig embeddingConfig) {
        String tableName = embeddingConfig.getEmbeddingTableName();
        if (tableName != null && !tableName.isEmpty()) {
            return tableName;
        }
        Integer dimension = embeddingConfig.getEmbeddingDimension();
        if (dimension != null && vectorTableService.tableExists(dimension)) {
            return vectorTableService.getTableName(dimension);
        }
        return null;
    }

    /**
     * 构建搜索参数
     */
    private Map<String, Object> buildSearchParams(String vector, String documentId,
                                                  String knowledgeBaseId, double threshold, int topK) {
        Map<String, Object> params = new HashMap<>();
        params.put("vector", vector);
        params.put("threshold", threshold);
        params.put("topK", topK);
        if (documentId != null && !documentId.isEmpty()) {
            params.put("documentId", documentId);
        }
        if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
            params.put("knowledgeBaseId", knowledgeBaseId);
        }
        return params;
    }

    /**
     * 构建搜索 SQL（JOIN document_chunks 获取 content）
     */
    private String buildSearchSql(String tableName, String documentId, String knowledgeBaseId) {
        boolean hasDocumentId = documentId != null && !documentId.isEmpty();
        boolean hasKnowledgeBaseId = knowledgeBaseId != null && !knowledgeBaseId.isEmpty();

        if (hasDocumentId) {
            return String.format("""
                    SELECT e.chunk_id, e.document_id, dc.content, 1 - (e.embedding <=> :vector::vector) as score
                    FROM %s e
                    JOIN document_chunks dc ON e.chunk_id = dc.id
                    WHERE e.document_id = :documentId AND 1 - (e.embedding <=> :vector::vector) > :threshold
                    ORDER BY e.embedding <=> :vector::vector ASC
                    LIMIT :topK
                    """, tableName);
        } else if (hasKnowledgeBaseId) {
            return String.format("""
                    SELECT e.chunk_id, e.document_id, dc.content, 1 - (e.embedding <=> :vector::vector) as score
                    FROM %s e
                    JOIN document_chunks dc ON e.chunk_id = dc.id
                    JOIN documents d ON e.document_id = d.id
                    WHERE d.knowledge_base_id = :knowledgeBaseId AND 1 - (e.embedding <=> :vector::vector) > :threshold
                    ORDER BY e.embedding <=> :vector::vector ASC
                    LIMIT :topK
                    """, tableName);
        } else {
            return String.format("""
                    SELECT e.chunk_id, e.document_id, dc.content, 1 - (e.embedding <=> :vector::vector) as score
                    FROM %s e
                    JOIN document_chunks dc ON e.chunk_id = dc.id
                    WHERE 1 - (e.embedding <=> :vector::vector) > :threshold
                    ORDER BY e.embedding <=> :vector::vector ASC
                    LIMIT :topK
                    """, tableName);
        }
    }

    /**
     * 将向量数组格式化为 PostgreSQL vector 字符串
     * <p>
     * 格式：[0.1, 0.2, 0.3, ...]
     * </p>
     *
     * @param vector 向量数组
     * @return PostgreSQL vector 格式字符串
     */
    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}