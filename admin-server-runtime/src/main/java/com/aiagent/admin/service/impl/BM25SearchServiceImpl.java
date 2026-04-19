package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.service.BM25SearchService;
import com.aiagent.admin.service.mapper.VectorSearchResultRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BM25 关键词检索服务实现类
 * <p>
 * 使用 PostgreSQL 全文搜索功能实现关键词检索：
 * <ul>
 *   <li>英文关键词：使用 tsvector/tsquery (BM25)</li>
 *   <li>中文关键词：使用 pg_trgm similarity 匹配</li>
 *   <li>GIN 索引: 高效全文搜索索引</li>
 * </ul>
 * </p>
 * <p>
 * 搜索流程：
 * <ol>
 *   <li>检测查询文本语言（中文/英文）</li>
 *   <li>中文使用 pg_trgm similarity，英文使用 ts_rank</li>
 *   <li>按相似度降序返回 topK 结果</li>
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
    private final NamedParameterJdbcTemplate namedTemplate;
    private final DocumentChunkRepository documentChunkRepository;

    private static final VectorSearchResultRowMapper RESULT_ROW_MAPPER = new VectorSearchResultRowMapper();

    /**
     * 中文相似度阈值（使用 jieba ts_rank）
     * <p>
     * ts_rank 对于中文分词的分数通常在 0.01-0.5 之间，
     * 使用较低阈值以确保能匹配到结果。
     * </p>
     */
    private static final double CHINESE_RANK_THRESHOLD = 0.01;

    /**
     * BM25 关键词检索
     * <p>
     * 根据查询语言选择搜索策略：
     * <ul>
     *   <li>中文查询：使用 pg_trgm similarity 匹配</li>
     *   <li>英文查询：使用 ts_rank BM25 评分</li>
     * </ul>
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
        if (query == null || query.isBlank()) {
            log.warn("Empty query");
            return List.of();
        }

        // 检测是否包含中文
        boolean containsChinese = query.matches(".*[\\u4e00-\\u9fa5].*");

        List<VectorSearchResult> results;
        if (containsChinese) {
            // 中文查询：使用 pg_trgm similarity
            results = searchChinese(query, knowledgeBaseId, documentId, topK);
            log.debug("Chinese search (pg_trgm) found {} results for query: {}", results.size(), query);
        } else {
            // 英文查询：使用 ts_rank BM25
            String tsQuery = preprocessQuery(query);
            if (tsQuery.isEmpty()) {
                log.warn("Empty query after preprocessing: {}", query);
                return List.of();
            }
            Map<String, Object> params = buildBM25Params(tsQuery, knowledgeBaseId, documentId, topK);
            String sql = buildBM25Sql(documentId, knowledgeBaseId);
            results = namedTemplate.query(sql, params, RESULT_ROW_MAPPER);
            log.debug("English search (BM25) found {} results for query: {}", results.size(), query);
        }

        // 补充 chunkIndex 和 metadata
        enrichChunkMetadata(results);
        // 补充 documentName
        enrichDocumentNames(results);

        return results;
    }

    /**
     * 中文关键词搜索（使用 jieba 分词 ts_rank）
     * <p>
     * pg_jieba 将中文文本分词为词元，使用 tsvector/tsquery 进行全文搜索，
     * ts_rank 计算相关性分数。
     * </p>
     *
     * @param query           查询文本
     * @param knowledgeBaseId 知识库 ID 过滤
     * @param documentId      文档 ID 过滤
     * @param topK            返回数量
     * @return 搜索结果列表
     */
    private List<VectorSearchResult> searchChinese(String query, String knowledgeBaseId, String documentId, int topK) {
        // 预处理查询文本为 tsquery 格式（OR 查询）
        String tsQuery = preprocessChineseQuery(query);
        if (tsQuery.isEmpty()) {
            log.warn("Empty Chinese query after preprocessing: {}", query);
            return List.of();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("tsQuery", tsQuery);
        params.put("threshold", CHINESE_RANK_THRESHOLD);
        params.put("topK", topK);

        boolean hasDocumentId = documentId != null && !documentId.isEmpty();
        boolean hasKnowledgeBaseId = knowledgeBaseId != null && !knowledgeBaseId.isEmpty();

        String sql;
        if (hasDocumentId) {
            sql = """
                    SELECT dc.id as chunk_id, dc.document_id, dc.content,
                           ts_rank(dc.content_tsv_jieba, to_tsquery('jiebamp', :tsQuery)) as score
                    FROM document_chunks dc
                    WHERE dc.document_id = :documentId AND dc.content_tsv_jieba @@ to_tsquery('jiebamp', :tsQuery)
                    ORDER BY score DESC
                    LIMIT :topK
                    """;
            params.put("documentId", documentId);
        } else if (hasKnowledgeBaseId) {
            sql = """
                    SELECT dc.id as chunk_id, dc.document_id, dc.content,
                           ts_rank(dc.content_tsv_jieba, to_tsquery('jiebamp', :tsQuery)) as score
                    FROM document_chunks dc
                    JOIN documents d ON dc.document_id = d.id
                    WHERE d.knowledge_base_id = :knowledgeBaseId AND dc.content_tsv_jieba @@ to_tsquery('jiebamp', :tsQuery)
                    ORDER BY score DESC
                    LIMIT :topK
                    """;
            params.put("knowledgeBaseId", knowledgeBaseId);
        } else {
            sql = """
                    SELECT dc.id as chunk_id, dc.document_id, dc.content,
                           ts_rank(dc.content_tsv_jieba, to_tsquery('jiebamp', :tsQuery)) as score
                    FROM document_chunks dc
                    WHERE dc.content_tsv_jieba @@ to_tsquery('jiebamp', :tsQuery)
                    ORDER BY score DESC
                    LIMIT :topK
                    """;
        }

        return namedTemplate.query(sql, params, RESULT_ROW_MAPPER);
    }

    /**
     * 预处理中文查询文本
     * <p>
     * 将查询文本转换为 PostgreSQL tsquery 格式：
     * <ul>
     *   <li>移除特殊字符</li>
     *   <li>转换为 OR 查询（多个词用 | 连接）</li>
     * </ul>
     * </p>
     *
     * @param query 原始查询文本
     * @return 处理后的 tsquery 格式字符串
     */
    private String preprocessChineseQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        // 移除特殊字符，保留中文、字母、数字、空格
        String cleaned = query.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", " ");

        // 分词并用 | 连接（OR 查询）
        // 对于中文，按字符分割效果不好，直接使用整个查询
        // jieba 会自动分词
        String[] words = cleaned.trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return "";
        }

        return String.join(" | ", words);
    }

    /**
     * 构建 BM25 搜索参数（英文）
     */
    private Map<String, Object> buildBM25Params(String tsQuery, String knowledgeBaseId, String documentId, int topK) {
        Map<String, Object> params = new HashMap<>();
        params.put("tsQuery", tsQuery);
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
     * 构建 BM25 搜索 SQL（英文）
     */
    private String buildBM25Sql(String documentId, String knowledgeBaseId) {
        boolean hasDocumentId = documentId != null && !documentId.isEmpty();
        boolean hasKnowledgeBaseId = knowledgeBaseId != null && !knowledgeBaseId.isEmpty();

        if (hasDocumentId) {
            return """
                    SELECT dc.id as chunk_id, dc.document_id, dc.content,
                           ts_rank(dc.content_tsv, to_tsquery('simple', :tsQuery)) as score
                    FROM document_chunks dc
                    WHERE dc.document_id = :documentId AND dc.content_tsv @@ to_tsquery('simple', :tsQuery)
                    ORDER BY score DESC
                    LIMIT :topK
                    """;
        } else if (hasKnowledgeBaseId) {
            return """
                    SELECT dc.id as chunk_id, dc.document_id, dc.content,
                           ts_rank(dc.content_tsv, to_tsquery('simple', :tsQuery)) as score
                    FROM document_chunks dc
                    JOIN documents d ON dc.document_id = d.id
                    WHERE d.knowledge_base_id = :knowledgeBaseId AND dc.content_tsv @@ to_tsquery('simple', :tsQuery)
                    ORDER BY score DESC
                    LIMIT :topK
                    """;
        } else {
            return """
                    SELECT dc.id as chunk_id, dc.document_id, dc.content,
                           ts_rank(dc.content_tsv, to_tsquery('simple', :tsQuery)) as score
                    FROM document_chunks dc
                    WHERE dc.content_tsv @@ to_tsquery('simple', :tsQuery)
                    ORDER BY score DESC
                    LIMIT :topK
                    """;
        }
    }

    /**
     * 补充分块元数据信息
     */
    private void enrichChunkMetadata(List<VectorSearchResult> results) {
        results.forEach(result -> {
            documentChunkRepository.findById(result.getChunkId()).ifPresent(chunk -> {
                result.setChunkIndex(chunk.getChunkIndex());
                result.setMetadata(chunk.getMetadata());
            });
        });
    }

    /**
     * 补充文档名称信息
     */
    private void enrichDocumentNames(List<VectorSearchResult> results) {
        results.forEach(result -> {
            namedTemplate.query(
                    "SELECT name FROM documents WHERE id = :documentId",
                    Map.of("documentId", result.getDocumentId()),
                    rs -> {
                        if (rs.next()) {
                            result.setDocumentName(rs.getString("name"));
                        }
                    }
            );
        });
    }

    @Override
    @Transactional
    public void ensureFullTextSearchIndex() {
        // 检查 content_tsv 列是否存在（英文）
        Integer columnExists = namedTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'document_chunks' AND column_name = 'content_tsv'",
                Map.of(),
                Integer.class);

        if (columnExists == null || columnExists == 0) {
            log.info("Adding content_tsv column to document_chunks table");
            createFullTextSearchInfrastructure();
        }

        // 检查 content_tsv_jieba 列是否存在（中文）
        Integer jiebaColumnExists = namedTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'document_chunks' AND column_name = 'content_tsv_jieba'",
                Map.of(),
                Integer.class);

        if (jiebaColumnExists == null || jiebaColumnExists == 0) {
            log.info("Adding content_tsv_jieba column for Chinese full-text search");
            createJiebaFullTextSearchInfrastructure();
        }

        // 检查 GIN 索引是否存在（英文 tsvector）
        Integer indexExists = namedTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'document_chunks' AND indexname = 'idx_document_chunks_tsv'",
                Map.of(),
                Integer.class);

        if (indexExists == null || indexExists == 0) {
            log.info("Creating GIN index on content_tsv column");
            jdbcTemplate.execute("CREATE INDEX idx_document_chunks_tsv ON document_chunks USING GIN(content_tsv)");
            log.info("GIN index created successfully");
        }

        // 检查 GIN 索引是否存在（中文 jieba tsvector）
        Integer jiebaIndexExists = namedTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'document_chunks' AND indexname = 'idx_document_chunks_tsv_jieba'",
                Map.of(),
                Integer.class);

        if (jiebaIndexExists == null || jiebaIndexExists == 0) {
            log.info("Creating GIN index on content_tsv_jieba column");
            jdbcTemplate.execute("CREATE INDEX idx_document_chunks_tsv_jieba ON document_chunks USING GIN(content_tsv_jieba)");
            log.info("Jieba GIN index created successfully");
        }

        // 确保 pg_trgm 扩展已安装（用于后备匹配）
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
    }

    /**
     * 创建中文全文搜索基础设施（jiebamp tsvector 列、触发器）
     * <p>
     * 使用 jiebamp parser（最大概率模式），分词效果更好。
     * </p>
     */
    private void createJiebaFullTextSearchInfrastructure() {
        // 添加 jiebamp tsvector 列
        jdbcTemplate.execute("ALTER TABLE document_chunks ADD COLUMN content_tsv_jieba tsvector");

        // 创建触发器函数（自动更新 jiebamp tsvector）
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION update_content_tsv_jieba() RETURNS trigger AS $$
                BEGIN
                  NEW.content_tsv_jieba := to_tsvector('jiebamp', COALESCE(NEW.content, ''));
                  RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);

        // 创建触发器
        jdbcTemplate.execute("""
                DROP TRIGGER IF EXISTS trg_update_content_tsv_jieba ON document_chunks;
                CREATE TRIGGER trg_update_content_tsv_jieba
                BEFORE INSERT OR UPDATE ON document_chunks
                FOR EACH ROW EXECUTE FUNCTION update_content_tsv_jieba()
                """);

        // 更新现有数据
        jdbcTemplate.execute("UPDATE document_chunks SET content_tsv_jieba = to_tsvector('jiebamp', COALESCE(content, ''))");

        log.info("content_tsv_jieba column and trigger created successfully using jiebamp parser");
    }

    /**
     * 创建全文搜索基础设施（列、触发器、函数）
     */
    private void createFullTextSearchInfrastructure() {
        // 添加 tsvector 列
        jdbcTemplate.execute("ALTER TABLE document_chunks ADD COLUMN content_tsv tsvector");

        // 创建触发器函数（自动更新 tsvector）
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION update_content_tsv() RETURNS trigger AS $$
                BEGIN
                  NEW.content_tsv := to_tsvector('simple', COALESCE(NEW.content, ''));
                  RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);

        // 创建触发器
        jdbcTemplate.execute("""
                DROP TRIGGER IF EXISTS trg_update_content_tsv ON document_chunks;
                CREATE TRIGGER trg_update_content_tsv
                BEFORE INSERT OR UPDATE ON document_chunks
                FOR EACH ROW EXECUTE FUNCTION update_content_tsv()
                """);

        // 更新现有数据
        jdbcTemplate.execute("UPDATE document_chunks SET content_tsv = to_tsvector('simple', COALESCE(content, ''))");

        log.info("content_tsv column and trigger created successfully");
    }

    /**
     * 预处理查询文本（英文）
     * <p>
     * 将查询文本转换为 PostgreSQL tsquery 格式：
     * <ul>
     *   <li>移除特殊字符</li>
     *   <li>转换为 OR 查询（多个词用 | 连接）</li>
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

        // 分词并用 | 连接（OR 查询）
        String[] words = cleaned.trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return "";
        }

        return String.join(" | ", words);
    }
}