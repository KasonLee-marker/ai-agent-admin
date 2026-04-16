package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.service.EmbeddingStorageService;
import com.aiagent.admin.service.VectorTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    private final JdbcTemplate jdbcTemplate;
    private final VectorTableService vectorTableService;

    @Override
    @Transactional
    public void storeVector(String chunkId, String documentId, float[] vector, int dimension, String tableName) {
        // 如果未提供表名，根据维度获取
        if (tableName == null || tableName.isEmpty()) {
            tableName = vectorTableService.ensureTableExists(dimension);
        }

        // 将向量转换为 PostgreSQL vector 格式的字符串
        String vectorStr = formatVector(vector);

        String sql = String.format("""
                INSERT INTO %s (chunk_id, document_id, embedding, created_at)
                VALUES (?, ?, ?::vector, CURRENT_TIMESTAMP)
                ON CONFLICT (chunk_id) DO UPDATE SET
                    embedding = EXCLUDED.embedding,
                    created_at = CURRENT_TIMESTAMP
                """, tableName);

        jdbcTemplate.update(sql, chunkId, documentId, vectorStr);
        log.debug("Stored vector for chunk {} in table {}", chunkId, tableName);
    }

    @Override
    @Transactional
    public void storeVectorsBatch(List<VectorData> vectors, int dimension, String tableName) {
        if (vectors == null || vectors.isEmpty()) {
            return;
        }

        // 如果未提供表名，根据维度获取
        if (tableName == null || tableName.isEmpty()) {
            tableName = vectorTableService.ensureTableExists(dimension);
        }

        // 批量插入
        for (VectorData data : vectors) {
            String vectorStr = formatVector(data.vector());

            String sql = String.format("""
                    INSERT INTO %s (chunk_id, document_id, embedding, created_at)
                    VALUES (?, ?, ?::vector, CURRENT_TIMESTAMP)
                    ON CONFLICT (chunk_id) DO UPDATE SET
                        embedding = EXCLUDED.embedding,
                        created_at = CURRENT_TIMESTAMP
                    """, tableName);

            jdbcTemplate.update(sql, data.chunkId(), data.documentId(), vectorStr);
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

        String sql = String.format("DELETE FROM %s WHERE document_id = ?", tableName);
        int deleted = jdbcTemplate.update(sql, documentId);
        log.debug("Deleted {} vectors from table {} for document {}", deleted, tableName, documentId);
    }

    @Override
    public List<VectorSearchResult> searchSimilar(float[] queryVector, ModelConfig embeddingConfig,
                                                  String documentId, String knowledgeBaseId, int topK, double threshold) {
        String tableName = embeddingConfig.getEmbeddingTableName();
        if (tableName == null || tableName.isEmpty()) {
            // 尝试根据维度获取表名
            if (embeddingConfig.getEmbeddingDimension() != null) {
                tableName = vectorTableService.getTableName(embeddingConfig.getEmbeddingDimension());
                if (!vectorTableService.tableExists(embeddingConfig.getEmbeddingDimension())) {
                    log.warn("Vector table {} does not exist", tableName);
                    return List.of();
                }
            } else {
                log.warn("No embedding table configured for model {}", embeddingConfig.getName());
                return List.of();
            }
        }

        String queryVectorStr = formatVector(queryVector);

        // 使用 pgvector 的余弦相似度运算符 <=> 进行检索
        // 注意：<=> 返回的是距离（0=最相似），所以需要取前 topK 个最小的距离
        // 根据 documentId 和 knowledgeBaseId 组合构建不同的 SQL
        boolean hasDocumentId = documentId != null && !documentId.isEmpty();
        boolean hasKnowledgeBaseId = knowledgeBaseId != null && !knowledgeBaseId.isEmpty();

        String sql;
        if (hasDocumentId) {
            // 指定文档 ID：直接过滤
            sql = String.format("""
                    SELECT e.chunk_id, e.document_id, 1 - (e.embedding <=> ?::vector) as score, e.created_at
                    FROM %s e
                    WHERE e.document_id = ? AND 1 - (e.embedding <=> ?::vector) > ?
                    ORDER BY e.embedding <=> ?::vector ASC
                    LIMIT ?
                    """, tableName);
        } else if (hasKnowledgeBaseId) {
            // 指定知识库：关联 documents 表过滤
            sql = String.format("""
                    SELECT e.chunk_id, e.document_id, 1 - (e.embedding <=> ?::vector) as score, e.created_at
                    FROM %s e
                    JOIN documents d ON e.document_id = d.id
                    WHERE d.knowledge_base_id = ? AND 1 - (e.embedding <=> ?::vector) > ?
                    ORDER BY e.embedding <=> ?::vector ASC
                    LIMIT ?
                    """, tableName);
        } else {
            // 无过滤条件
            sql = String.format("""
                    SELECT chunk_id, document_id, 1 - (embedding <=> ?::vector) as score, created_at
                    FROM %s
                    WHERE 1 - (embedding <=> ?::vector) > ?
                    ORDER BY embedding <=> ?::vector ASC
                    LIMIT ?
                    """, tableName);
        }

        List<VectorSearchResult> results = new ArrayList<>();

        if (hasDocumentId) {
            jdbcTemplate.query(sql, ps -> {
                ps.setString(1, queryVectorStr);
                ps.setString(2, documentId);
                ps.setString(3, queryVectorStr);
                ps.setDouble(4, threshold);
                ps.setString(5, queryVectorStr);
                ps.setInt(6, topK);
            }, rs -> {
                results.add(VectorSearchResult.builder()
                        .chunkId(rs.getString("chunk_id"))
                        .documentId(rs.getString("document_id"))
                        .score(rs.getDouble("score"))
                        .build());
            });
        } else if (hasKnowledgeBaseId) {
            jdbcTemplate.query(sql, ps -> {
                ps.setString(1, queryVectorStr);         // SELECT's vector
                ps.setString(2, knowledgeBaseId);        // WHERE's knowledge_base_id
                ps.setString(3, queryVectorStr);         // WHERE's vector
                ps.setDouble(4, threshold);              // WHERE's threshold
                ps.setString(5, queryVectorStr);         // ORDER BY's vector
                ps.setInt(6, topK);                      // LIMIT
            }, rs -> {
                results.add(VectorSearchResult.builder()
                        .chunkId(rs.getString("chunk_id"))
                        .documentId(rs.getString("document_id"))
                        .score(rs.getDouble("score"))
                        .build());
            });
        } else {
            jdbcTemplate.query(sql, ps -> {
                ps.setString(1, queryVectorStr);  // SELECT's vector
                ps.setString(2, queryVectorStr);  // WHERE's vector
                ps.setDouble(3, threshold);       // WHERE's threshold
                ps.setString(4, queryVectorStr);  // ORDER BY's vector
                ps.setInt(5, topK);               // LIMIT
            }, rs -> {
                results.add(VectorSearchResult.builder()
                        .chunkId(rs.getString("chunk_id"))
                        .documentId(rs.getString("document_id"))
                        .score(rs.getDouble("score"))
                        .build());
            });
        }

        log.debug("Found {} similar vectors in table {} with threshold {}", results.size(), tableName, threshold);
        return results;
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