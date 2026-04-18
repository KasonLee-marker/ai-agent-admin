package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import org.springframework.data.domain.Pageable;

/**
 * 评估服务接口
 * <p>
 * 提供 AI 模型评估的核心功能：
 * <ul>
 *   <li>评估任务的创建、查询、执行、取消</li>
 *   <li>评估结果的查询和分析</li>
 *   <li>多指标评估（AI评分、语义相似度、检索得分等）</li>
 *   <li>评估任务对比分析</li>
 * </ul>
 * </p>
 * <p>
 * 支持两种评估模式：
 * <ul>
 *   <li>普通评估：使用数据集测试模型响应质量</li>
 *   <li>RAG 评估：结合知识库检索测试 RAG 系统性能</li>
 * </ul>
 * </p>
 *
 * @see EvaluationJobResponse
 * @see EvaluationResultResponse
 * @see EvaluationMetricsResponse
 */
public interface EvaluationService {

    /**
     * 创建评估任务
     *
     * @param request 创建请求，包含模型ID、数据集ID等
     * @return 创建成功的评估任务响应 DTO
     */
    EvaluationJobResponse createJob(EvaluationJobCreateRequest request);

    /**
     * 更新评估任务信息
     *
     * @param id      评估任务唯一标识
     * @param request 更新请求
     * @return 更新后的评估任务响应 DTO
     */
    EvaluationJobResponse updateJob(String id, EvaluationJobUpdateRequest request);

    /**
     * 删除评估任务
     * <p>
     * 同时删除任务的所有评估结果。
     * </p>
     *
     * @param id 评估任务唯一标识
     */
    void deleteJob(String id);

    /**
     * 获取评估任务详情
     *
     * @param id 评估任务唯一标识
     * @return 评估任务响应 DTO
     */
    EvaluationJobResponse getJob(String id);

    /**
     * 分页查询评估任务列表
     *
     * @param status   任务状态过滤（可选）
     * @param keyword  搜索关键词（可选）
     * @param pageable 分页参数
     * @return 评估任务分页响应
     */
    PageResponse<EvaluationJobResponse> listJobs(String status, String keyword, Pageable pageable);

    /**
     * 执行评估任务
     * <p>
     * 异步执行，遍历数据集进行评估，生成评估结果。
     * </p>
     *
     * @param id 评估任务唯一标识
     * @return 执行中的评估任务响应 DTO
     */
    EvaluationJobResponse runJob(String id);

    /**
     * 取消正在执行的评估任务
     *
     * @param id 评估任务唯一标识
     */
    void cancelJob(String id);

    /**
     * 分页查询评估结果列表
     *
     * @param jobId   评估任务 ID
     * @param pageable 分页参数
     * @return 评估结果分页响应
     */
    PageResponse<EvaluationResultResponse> listResults(String jobId, Pageable pageable);

    /**
     * 获取评估结果详情
     *
     * @param resultId 评估结果唯一标识
     * @return 评估结果响应 DTO
     */
    EvaluationResultResponse getResult(String resultId);

    /**
     * 获取评估任务的汇总指标
     *
     * @param jobId 评估任务 ID
     * @return 评估指标响应 DTO
     */
    EvaluationMetricsResponse getMetrics(String jobId);

    /**
     * 对比多个评估任务的结果
     *
     * @param request 对比请求，包含多个任务 ID
     * @return 评估对比响应 DTO
     */
    EvaluationCompareResponse compareJobs(EvaluationCompareRequest request);

    /**
     * 重新运行评估任务
     * <p>
     * 删除之前的评估结果，重置任务状态和计数器，然后重新执行评估。
     * </p>
     *
     * @param id 任务唯一标识
     * @return 异步执行结果（CompletableFuture）
     */
    EvaluationJobResponse rerunJob(String id);
}
