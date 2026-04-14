package com.aiagent.admin.service;

import com.aiagent.admin.domain.entity.ModelConfig;

import java.util.List;

/**
 * Embedding 服务接口
 * <p>
 * 提供文本向量（Embedding）计算功能：
 * <ul>
 *   <li>单文本向量计算</li>
 *   <li>批量文本向量计算</li>
 *   <li>向量相似度计算</li>
 * </ul>
 * </p>
 * <p>
 * 支持的 Embedding 模型：
 * <ul>
 *   <li>OpenAI: text-embedding-ada-002, text-embedding-3-small, text-embedding-3-large</li>
 *   <li>DashScope: text-embedding-v1, text-embedding-v2, text-embedding-v3</li>
 * </ul>
 * </p>
 *
 * @see com.aiagent.admin.domain.entity.ModelConfig
 */
public interface EmbeddingService {

    /**
     * 计算单个文本的 Embedding 向量
     *
     * @param text 输入文本
     * @return Embedding 向量（浮点数组）
     */
    float[] embed(String text);

    /**
     * 批量计算文本的 Embedding 向量
     * <p>
     * 批量调用可以提高 API 效率，减少请求次数。
     * </p>
     *
     * @param texts 输入文本列表
     * @return Embedding 向量列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 使用指定的模型配置批量计算文本的 Embedding 向量
     * <p>
     * 不查找默认配置，直接使用传入的模型配置。
     * 用于文档向量化时指定特定的 embedding 模型。
     * </p>
     *
     * @param texts       输入文本列表
     * @param modelConfig 模型配置实体
     * @return Embedding 向量列表
     */
    List<float[]> embedBatchWithModel(List<String> texts, ModelConfig modelConfig);

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度值（0-1，值越大越相似）
     */
    float cosineSimilarity(float[] vector1, float[] vector2);

    /**
     * 计算两个文本的语义相似度
     * <p>
     * 先计算两个文本的 Embedding，再计算余弦相似度。
     * </p>
     *
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度值（0-1）
     */
    float semanticSimilarity(String text1, String text2);

    /**
     * 使用指定的模型配置计算单个文本的 Embedding 向量
     *
     * @param text        输入文本
     * @param modelConfig 模型配置实体
     * @return Embedding 向量（浮点数组）
     */
    float[] embedWithModel(String text, com.aiagent.admin.domain.entity.ModelConfig modelConfig);

    /**
     * 使用指定的模型配置计算两个文本的语义相似度
     *
     * @param text1       文本1
     * @param text2       文本2
     * @param modelConfig 模型配置实体
     * @return 相似度值（0-1）
     */
    float semanticSimilarityWithModel(String text1, String text2, com.aiagent.admin.domain.entity.ModelConfig modelConfig);

    /**
     * 获取 Embedding 向量的维度
     * <p>
     * 不同的模型有不同的维度：
     * <ul>
     *   <li>text-embedding-ada-002: 1536</li>
     *   <li>text-embedding-3-large: 3072</li>
     *   <li>text-embedding-v3: 1024</li>
     * </ul>
     * </p>
     *
     * @return 向量维度
     */
    int getEmbeddingDimension();
}