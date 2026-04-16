package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.ModelConfig;

import java.util.List;

/**
 * Rerank 重排序服务接口
 * <p>
 * 提供检索结果的二次排序功能：
 * <ul>
 *   <li>接收向量检索返回的候选结果</li>
 *   <li>调用 Rerank API 计算精确相关性分数</li>
 *   <li>按新分数重新排序，返回更精准的 topK 结果</li>
 * </ul>
 * </p>
 * <p>
 * Rerank 流程：
 * <ol>
 *   <li>向量检索获取候选结果（如 topK=20）</li>
 *   <li>构建 Rerank API 请求（query + documents）</li>
 *   <li>调用 Cohere/Jina Rerank API</li>
 *   <li>解析响应中的 relevance_score</li>
 *   <li>按新分数排序，返回最终 topK（如 5）</li>
 * </ol>
 * </p>
 *
 * @see VectorSearchResult
 * @see ModelConfig
 */
public interface RerankService {

    /**
     * 对搜索结果进行重排序
     * <p>
     * 执行流程：
     * <ol>
     *   <li>提取候选结果的文本内容</li>
     *   <li>根据模型供应商选择 API 格式（Cohere 或 Jina）</li>
     *   <li>调用 Rerank API 获取精确相关性分数</li>
     *   <li>更新每个结果的 score 字段</li>
     *   <li>按新分数降序排序</li>
     *   <li>返回前 topK 个结果</li>
     * </ol>
     * </p>
     *
     * @param query        查询文本
     * @param results      原始搜索结果列表（候选结果）
     * @param rerankConfig Rerank 模型配置
     * @param topK         最终返回数量
     * @return 重排序后的结果列表（按新分数排序）
     */
    List<VectorSearchResult> rerank(String query, List<VectorSearchResult> results,
                                    ModelConfig rerankConfig, int topK);
}