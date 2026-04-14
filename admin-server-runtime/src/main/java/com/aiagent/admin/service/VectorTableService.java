package com.aiagent.admin.service;

/**
 * 向量表管理服务接口
 * <p>
 * 管理 pgvector 向量存储表：
 * <ul>
 *   <li>按维度创建不同的向量表（document_embeddings_1024, document_embeddings_1536 等）</li>
 *   <li>检查表是否存在</li>
 *   <li>获取表名</li>
 * </ul>
 * </p>
 * <p>
 * 设计思路：不同 embedding 模型产生不同维度的向量，
 * 需要存储到对应的维度表中，便于 pgvector 进行高效检索。
 * </p>
 *
 * @see com.aiagent.admin.domain.entity.ModelConfig
 */
public interface VectorTableService {

    /**
     * 确保指定维度的向量表存在
     * <p>
     * 如果表不存在，自动创建：
     * <pre>
     * CREATE TABLE document_embeddings_{dimension} (
     *   chunk_id VARCHAR(64) PRIMARY KEY,
     *   document_id VARCHAR(64) NOT NULL,
     *   embedding vector({dimension}),
     *   created_at TIMESTAMP NOT NULL
     * );
     * CREATE INDEX ON document_embeddings_{dimension} USING ivfflat (embedding vector_cosine_ops);
     * </pre>
     * </p>
     *
     * @param dimension 向量维度（如 1536）
     * @return 表名
     */
    String ensureTableExists(int dimension);

    /**
     * 获取指定维度的向量表名
     *
     * @param dimension 向量维度
     * @return 表名，格式：document_embeddings_{dimension}
     */
    String getTableName(int dimension);

    /**
     * 检查指定维度的向量表是否存在
     *
     * @param dimension 向量维度
     * @return 是否存在
     */
    boolean tableExists(int dimension);

    /**
     * 删除指定维度的向量表
     * <p>
     * 仅在确认无数据时使用，谨慎操作。
     * </p>
     *
     * @param dimension 向量维度
     */
    void dropTable(int dimension);

    /**
     * 获取所有已创建的向量表维度列表
     *
     * @return 维度列表
     */
    java.util.List<Integer> getExistingDimensions();
}