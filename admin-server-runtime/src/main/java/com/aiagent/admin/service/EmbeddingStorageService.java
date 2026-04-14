package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.ModelConfig;

import java.util.List;

/**
 * Embedding 向量存储服务接口
 * <p>
 * 管理 embedding 向量的存储和检索：
 * <ul>
 *   <li>存储向量到对应的维度表</li>
 *   <li>按文档删除向量</li>
 *   <li>向量相似度检索</li>
 * </ul>
 * </p>
 * <p>
 * 不同维度存储到不同的表（document_embeddings_1024, document_embeddings_1536 等）。
 * 检索时根据 embedding 模型配置选择正确的表。
 * </p>
 *
 * @see VectorTableService
 * @see ModelConfig
 */
public interface EmbeddingStorageService {

    /**
     * 存储向量
     * <p>
     * 根据维度选择正确的向量表进行存储。
     * </p>
     *
     * @param chunkId    分块 ID
     * @param documentId 文档 ID
     * @param vector     向量数据
     * @param dimension  向量维度
     * @param tableName  目标表名（可选，如不提供则根据维度推断）
     */
    void storeVector(String chunkId, String documentId, float[] vector, int dimension, String tableName);

    /**
     * 批量存储向量
     *
     * @param vectors   向量数据列表，每个元素包含 chunkId, documentId, vector
     * @param dimension 向量维度
     * @param tableName 目标表名
     */
    void storeVectorsBatch(List<VectorData> vectors, int dimension, String tableName);

    /**
     * 删除文档的所有向量
     *
     * @param documentId 文档 ID
     * @param tableName  向量表名
     */
    void deleteByDocument(String documentId, String tableName);

    /**
     * 向量相似度检索
     * <p>
     * 使用 pgvector 的向量检索功能，返回最相似的文档分块。
     * </p>
     *
     * @param queryVector     查询向量
     * @param embeddingConfig Embedding 模型配置（包含维度和表名）
     * @param documentId      文档 ID 过滤（可选）
     * @param topK            返回数量
     * @param threshold       相似度阈值
     * @return 相似度检索结果列表
     */
    List<VectorSearchResult> searchSimilar(float[] queryVector, ModelConfig embeddingConfig,
                                           String documentId, int topK, double threshold);

    /**
     * 向量数据包装类
     */
    record VectorData(String chunkId, String documentId, float[] vector) {
    }
}