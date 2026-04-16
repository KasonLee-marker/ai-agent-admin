package com.aiagent.admin.domain.enums;

/**
 * 检索策略枚举
 * <p>
 * 定义 RAG 检索的不同策略：
 * <ul>
 *   <li>VECTOR - 纯向量检索，使用余弦相似度</li>
 *   <li>BM25 - 关键词检索，使用 PostgreSQL 全文搜索（P2 实现）</li>
 *   <li>HYBRID - 混合检索，融合向量检索和 BM25 结果（P2 实现）</li>
 * </ul>
 * </p>
 */
public enum RetrievalStrategy {

    /**
     * 纯向量检索
     * <p>
     * 使用 Embedding 向量计算相似度，适合语义相关性高的场景。
     * </p>
     */
    VECTOR,

    /**
     * BM25 关键词检索
     * <p>
     * 使用 PostgreSQL 全文搜索（tsvector/tsquery），
     * 适合精确匹配和术语查询场景。
     * </p>
     * <p>
     * 注：此策略为 P2 功能，当前版本尚未实现。
     * </p>
     */
    BM25,

    /**
     * 混合检索
     * <p>
     * 结合向量检索和 BM25 检索结果，使用 RRF（Reciprocal Rank Fusion）
     * 算法融合排名，适合综合场景。
     * </p>
     * <p>
     * 注：此策略为 P2 功能，当前版本尚未实现。
     * </p>
     */
    HYBRID
}