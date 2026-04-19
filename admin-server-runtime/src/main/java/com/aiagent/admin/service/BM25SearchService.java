package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.VectorSearchResult;

import java.util.List;

/**
 * BM25 关键词检索服务接口
 * <p>
 * 使用 PostgreSQL 全文搜索（tsvector/tsquery）实现 BM25-like 关键词检索。
 * 适用于精确匹配场景，如搜索代码片段、专有名词等。
 * </p>
 * <p>
 * 与向量检索的区别：
 * <ul>
 *   <li>向量检索：语义相似度，适合模糊匹配、同义词</li>
 *   <li>BM25 检索：关键词匹配，适合精确查找、专有术语</li>
 * </ul>
 * </p>
 *
 * @see DocumentService
 * @see EmbeddingStorageService
 */
public interface BM25SearchService {

    /**
     * BM25 关键词检索
     * <p>
     * 使用 PostgreSQL ts_rank 函数计算文本相关性分数。
     * 搜索结果按相关性降序排列。
     * </p>
     * <p>
     * 注意：BM25 分数范围与向量相似度不同：
     * <ul>
     *   <li>向量相似度：0-1（余弦相似度）</li>
     *   <li>BM25 分数：0.01-0.5（ts_rank）</li>
     * </ul>
     * 建议阈值设置：BM25 使用 0.01-0.1，向量使用 0.3-0.7
     * </p>
     *
     * @param query           查询文本（关键词）
     * @param knowledgeBaseId 知识库 ID 过滤（可选，null 表示不限制）
     * @param documentId      文档 ID 过滤（可选，null 表示不限制）
     * @param topK            返回数量
     * @param threshold       相关性阈值（可选，null 表示不限制）
     * @return 搜索结果列表，包含 chunkId、documentId、content、score
     */
    List<VectorSearchResult> searchBM25(String query, String knowledgeBaseId, String documentId, int topK, Double threshold);

    /**
     * 确保全文搜索索引存在
     * <p>
     * 检查 document_chunks 表是否有 content_tsv 列和 GIN 索引，
     * 如果不存在则创建。该方法应在应用启动时调用。
     * </p>
     */
    void ensureFullTextSearchIndex();
}