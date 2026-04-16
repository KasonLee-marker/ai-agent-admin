package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.service.BM25SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * BM25 关键词检索服务实现类
 * <p>
 * 使用 PostgreSQL 全文搜索功能实现关键词检索：
 * <ul>
 *   <li>tsvector: 文本向量化存储</li>
 *   <li>tsquery: 查询表达式</li>
 *   <li>ts_rank: 相关性评分函数</li>
 *   <li>GIN 索引: 高效全文搜索索引</li>
 * </ul>
 * </p>
 * <p>
 * 搜索流程：
 * <ol>
 *   <li>将查询文本转换为 tsquery</li>
 *   <li>使用 ts_rank 计算相关性分数</li>
 *   <li>按分数降序返回 topK 结果</li>
 * </ol>
 * </p>
 *
 * @see BM25SearchService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BM25SearchServiceImpl implements BM25SearchService {

    private final JdbcTemplate jdbcTemplate;
    private final DocumentChunkRepository documentChunkRepository;

    /**
     * BM25 关键词检索
     * <p>
     * 使用 PostgreSQL ts_rank 函数计算文本相关性分数。
     * 搜索结果按相关性降序排列。
     * </p>
     *
     * @param query           查询文本（关键词）
     * @param knowledgeBaseId 知识库 ID 过滤（可选）
     * @param documentId      文档 ID 过滤（可选）
     * @param topK            返回数量
     * @return 搜索结果列表
     */
    @Override
    public List<VectorSearchResult> searchBM25(String query, String knowledgeBaseId, String documentId, int topK) {
        // 预处理查询文本：移除特殊字符，转换为 PostgreSQL tsquery 格式
        String tsQuery = preprocessQuery(query);

        if (tsQuery.isEmpty()) {
            log.warn("Empty query after preprocessing: {}", query);
            return List.of();
        }

        boolean hasKnowledgeBaseId = knowledgeBaseId != null && !knowledgeBaseId.isEmpty();
        boolean hasDocumentId = documentId != null && !documentId.isEmpty();

        String sql;
        if (hasDocumentId) {
            // 指定文档 ID：直接过滤
            sql = """
                    SELECT dc.id as chunk_id, dc.document_id, dc.content,
                           ts_rank(dc.content_tsv, to_tsquery('simple', ?)) as score
                    FROM document_chunks dc
                    WHERE dc.document_id = ? AND dc.content_tsv @@ to_tsquery('simple', ?)
                    ORDER BY score DESC
                    LIMIT ?
                    """;
        } else if (hasKnowledgeBaseId) {
            // 指定知识库：关联 documents 表过滤
            sql = """
                    SELECT dc.id as chunk_id, dc.document_id, dc.content,
                           ts_rank(dc.content_tsv, to_tsquery('simple', ?)) as score
                    FROM document_chunks dc
                    JOIN documents d ON dc.document_id = d.id
                    WHERE d.knowledge_base_id = ? AND dc.content_tsv @@ to_tsquery('simple', ?)
                    ORDER BY score DESC
                    LIMIT ?
                    """;
        } else {
            // 无过滤条件
            sql = """
                    SELECT dc.id as chunk_id, dc.document_id, dc.content,
                           ts_rank(dc.content_tsv, to_tsquery('simple', ?)) as score
                    FROM document_chunks dc
                    WHERE dc.content_tsv @@ to_tsquery('simple', ?)
                    ORDER BY score DESC
                    LIMIT ?
                    """;
        }

        List<VectorSearchResult> results = new ArrayList<>();

        if (hasDocumentId) {
            jdbcTemplate.query(sql, ps -> {
                ps.setString(1, tsQuery);      // ts_rank's tsquery
                ps.setString(2, documentId);   // WHERE document_id
                ps.setString(3, tsQuery);      // WHERE tsquery
                ps.setInt(4, topK);            // LIMIT
            }, rs -> {
                results.add(VectorSearchResult.builder()
                        .chunkId(rs.getString("chunk_id"))
                        .documentId(rs.getString("document_id"))
                        .content(rs.getString("content"))
                        .score(rs.getDouble("score"))
                        .build());
            });
        } else if (hasKnowledgeBaseId) {
            jdbcTemplate.query(sql, ps -> {
                ps.setString(1, tsQuery);           // ts_rank's tsquery
                ps.setString(2, knowledgeBaseId);   // WHERE knowledge_base_id
                ps.setString(3, tsQuery);           // WHERE tsquery
                ps.setInt(4, topK);                 // LIMIT
            }, rs -> {
                results.add(VectorSearchResult.builder()
                        .chunkId(rs.getString("chunk_id"))
                        .documentId(rs.getString("document_id"))
                        .content(rs.getString("content"))
                        .score(rs.getDouble("score"))
                        .build());
            });
        } else {
            jdbcTemplate.query(sql, ps -> {
                ps.setString(1, tsQuery);  // ts_rank's tsquery
                ps.setString(2, tsQuery);  // WHERE tsquery
                ps.setInt(3, topK);        // LIMIT
            }, rs -> {
                results.add(VectorSearchResult.builder()
                        .chunkId(rs.getString("chunk_id"))
                        .documentId(rs.getString("document_id"))
                        .content(rs.getString("content"))
                        .score(rs.getDouble("score"))
                        .build());
            });
        }

        // 补充 chunkIndex 信息
        results.forEach(result -> {
            documentChunkRepository.findById(result.getChunkId()).ifPresent(chunk -> {
                result.setChunkIndex(chunk.getChunkIndex());
                result.setMetadata(chunk.getMetadata());
            });
        });

        // 补充 documentName 信息
        results.forEach(result -> {
            jdbcTemplate.query(
                    "SELECT name FROM documents WHERE id = ?",
                    ps -> ps.setString(1, result.getDocumentId()),
                    rs -> {
                        if (rs.next()) {
                            result.setDocumentName(rs.getString("name"));
                        }
                    }
            );
        });

        log.debug("BM25 search found {} results for query: {}", results.size(), query);
        return results;
    }

    /**
     * 确保全文搜索索引存在
     * <p>
     * 检查 document_chunks 表是否有 content_tsv 列和 GIN 索引，
     * 如果不存在则创建。
     * </p>
     */
    @Override
    @Transactional
    public void ensureFullTextSearchIndex() {
        // 检查 content_tsv 列是否存在
        String checkColumnSql = """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'document_chunks' AND column_name = 'content_tsv'
                """;
        Integer columnExists = jdbcTemplate.queryForObject(checkColumnSql, Integer.class);

        if (columnExists == null || columnExists == 0) {
            log.info("Adding content_tsv column to document_chunks table");
            // 添加 tsvector 列
            jdbcTemplate.execute("ALTER TABLE document_chunks ADD COLUMN content_tsv tsvector");

            // 创建触发器函数（自动更新 tsvector）
            String createFunctionSql = """
                    CREATE OR REPLACE FUNCTION update_content_tsv() RETURNS trigger AS $$
                    BEGIN
                      NEW.content_tsv := to_tsvector('simple', COALESCE(NEW.content, ''));
                      RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql
                    """;
            jdbcTemplate.execute(createFunctionSql);

            // 创建触发器
            String createTriggerSql = """
                    DROP TRIGGER IF EXISTS trg_update_content_tsv ON document_chunks;
                    CREATE TRIGGER trg_update_content_tsv
                    BEFORE INSERT OR UPDATE ON document_chunks
                    FOR EACH ROW EXECUTE FUNCTION update_content_tsv()
                    """;
            jdbcTemplate.execute(createTriggerSql);

            // 更新现有数据
            jdbcTemplate.execute("UPDATE document_chunks SET content_tsv = to_tsvector('simple', COALESCE(content, ''))");

            log.info("content_tsv column and trigger created successfully");
        }

        // 检查 GIN 索引是否存在
        String checkIndexSql = """
                SELECT COUNT(*) FROM pg_indexes
                WHERE tablename = 'document_chunks' AND indexname = 'idx_document_chunks_tsv'
                """;
        Integer indexExists = jdbcTemplate.queryForObject(checkIndexSql, Integer.class);

        if (indexExists == null || indexExists == 0) {
            log.info("Creating GIN index on content_tsv column");
            jdbcTemplate.execute("CREATE INDEX idx_document_chunks_tsv ON document_chunks USING GIN(content_tsv)");
            log.info("GIN index created successfully");
        }
    }

    /**
     * 预处理查询文本
     * <p>
     * 将查询文本转换为 PostgreSQL tsquery 格式：
     * <ul>
     *   <li>移除特殊字符</li>
     *   <li>转换为 AND 查询（多个词用 & 连接）</li>
     *   <li>处理空查询</li>
     * </ul>
     * </p>
     *
     * @param query 原始查询文本
     * @return 处理后的 tsquery 格式字符串
     */
    private String preprocessQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        // 移除特殊字符，保留字母、数字、中文、空格
        String cleaned = query.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", " ");

        // 分词并用 & 连接（AND 查询）
        String[] words = cleaned.trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return "";
        }

        return String.join(" & ", words);
    }
}