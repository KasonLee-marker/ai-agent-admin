package com.aiagent.admin.service.impl;

import com.aiagent.admin.service.VectorTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 向量表管理服务实现类
 * <p>
 * 使用 JdbcTemplate 直接操作 PostgreSQL，
 * 动态创建和管理 pgvector 向量存储表。
 * </p>
 * <p>
 * 表结构设计：
 * <ul>
 *   <li>chunk_id: 分块ID，主键，关联 document_chunks 表</li>
 *   <li>document_id: 文档ID，便于按文档过滤检索</li>
 *   <li>embedding: pgvector 类型，存储向量数据</li>
 *   <li>created_at: 创建时间</li>
 * </ul>
 * </p>
 * <p>
 * 索引策略：使用 ivfflat 索引加速向量检索。
 * </p>
 *
 * @see VectorTableService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorTableServiceImpl implements VectorTableService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 向量表名前缀
     */
    private static final String TABLE_PREFIX = "document_embeddings_";

    /**
     * pgvector 扩展名
     */
    private static final String VECTOR_EXTENSION = "vector";

    @Override
    @Transactional
    public String ensureTableExists(int dimension) {
        String tableName = getTableName(dimension);

        // 先确保 pgvector 扩展已安装
        ensureVectorExtension();

        if (tableExists(dimension)) {
            log.debug("Vector table {} already exists", tableName);
            return tableName;
        }

        // 创建向量表
        String createTableSql = String.format("""
                CREATE TABLE %s (
                    chunk_id VARCHAR(64) PRIMARY KEY,
                    document_id VARCHAR(64) NOT NULL,
                    embedding vector(%d),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """, tableName, dimension);

        jdbcTemplate.execute(createTableSql);
        log.info("Created vector table: {} for dimension {}", tableName, dimension);

        // 创建索引（ivfflat 索引，适合大规模向量检索）
        String createIndexSql = String.format("""
                CREATE INDEX IF NOT EXISTS idx_%s_embedding
                ON %s USING ivfflat (embedding vector_cosine_ops)
                WITH (lists = 100)
                """, tableName, tableName);

        try {
            jdbcTemplate.execute(createIndexSql);
            log.info("Created ivfflat index on table {}", tableName);
        } catch (Exception e) {
            log.warn("Failed to create ivfflat index on {}, will use without index: {}", tableName, e.getMessage());
            // ivfflat 索引创建失败不影响基本功能
        }

        // 创建 document_id 索引，便于按文档过滤
        String createDocIndexSql = String.format("""
                CREATE INDEX IF NOT EXISTS idx_%s_document_id
                ON %s (document_id)
                """, tableName, tableName);

        jdbcTemplate.execute(createDocIndexSql);

        return tableName;
    }

    @Override
    public String getTableName(int dimension) {
        return TABLE_PREFIX + dimension;
    }

    @Override
    public boolean tableExists(int dimension) {
        String tableName = getTableName(dimension);
        try {
            String sql = """
                    SELECT EXISTS (
                        SELECT FROM information_schema.tables
                        WHERE table_schema = 'public'
                        AND table_name = ?
                    )
                    """;
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableName);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Failed to check table existence: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void dropTable(int dimension) {
        String tableName = getTableName(dimension);
        if (!tableExists(dimension)) {
            log.debug("Table {} does not exist, skip dropping", tableName);
            return;
        }

        String dropSql = String.format("DROP TABLE IF EXISTS %s CASCADE", tableName);
        jdbcTemplate.execute(dropSql);
        log.info("Dropped vector table: {}", tableName);
    }

    @Override
    public List<Integer> getExistingDimensions() {
        try {
            String sql = """
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                    AND table_name LIKE ?
                    """;
            List<String> tableNames = jdbcTemplate.queryForList(sql, String.class, TABLE_PREFIX + "%");

            return tableNames.stream()
                    .map(name -> name.substring(TABLE_PREFIX.length()))
                    .map(Integer::parseInt)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to get existing dimensions: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 确保 pgvector 扩展已安装
     */
    private void ensureVectorExtension() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.debug("pgvector extension ensured");
        } catch (Exception e) {
            log.error("Failed to create pgvector extension: {}", e.getMessage());
            throw new RuntimeException("pgvector extension is required but not available. Please install pgvector in PostgreSQL.");
        }
    }
}