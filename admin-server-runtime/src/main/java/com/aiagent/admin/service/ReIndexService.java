package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.ReindexProgressResponse;
import com.aiagent.admin.api.dto.ReindexRequest;
import jakarta.persistence.EntityNotFoundException;

/**
 * 知识库重索引服务接口
 * <p>
 * 提供知识库 Embedding 模型切换后的批量重计算功能：
 * <ul>
 *   <li>启动重索引任务（异步执行）</li>
 *   <li>查询重索引进度</li>
 *   <li>取消正在进行的重索引</li>
 * </ul>
 * </p>
 * <p>
 * 重索引流程：
 * <ol>
 *   <li>验证知识库和新 Embedding 模型</li>
 *   <li>获取所有文档和分块</li>
 *   <li>删除旧向量数据</li>
 *   <li>分批计算新 Embedding</li>
 *   <li>存储到新向量表</li>
 *   <li>更新文档的 Embedding 模型信息</li>
 * </ol>
 * </p>
 *
 * @see ReindexProgressResponse
 * @see ReindexRequest
 */
public interface ReIndexService {

    /**
     * 启动知识库重索引（异步执行）
     * <p>
     * 执行流程：
     * <ol>
     *   <li>验证知识库存在且无正在进行的重索引</li>
     *   <li>验证新 Embedding 模型已健康检查</li>
     *   <li>初始化进度状态</li>
     *   <li>异步执行重索引任务</li>
     * </ol>
     * </p>
     *
     * @param knowledgeBaseId     知识库 ID
     * @param newEmbeddingModelId 新 Embedding 模型 ID
     * @return 重索引进度响应
     * @throws IllegalStateException   如果已有重索引任务正在进行
     * @throws EntityNotFoundException 如果知识库或模型不存在
     */
    ReindexProgressResponse startReindex(String knowledgeBaseId, String newEmbeddingModelId);

    /**
     * 获取重索引进度
     * <p>
     * 返回当前重索引状态、进度百分比、错误信息等。
     * </p>
     *
     * @param knowledgeBaseId 知识库 ID
     * @return 重索引进度响应
     */
    ReindexProgressResponse getReindexProgress(String knowledgeBaseId);

    /**
     * 取消正在进行的重索引
     * <p>
     * 仅当状态为 IN_PROGRESS 时可取消。取消后状态变为 FAILED，
     * 保留已处理的进度信息。
     * </p>
     *
     * @param knowledgeBaseId 知识库 ID
     * @throws IllegalStateException 如果重索引未在进行中
     */
    void cancelReindex(String knowledgeBaseId);
}