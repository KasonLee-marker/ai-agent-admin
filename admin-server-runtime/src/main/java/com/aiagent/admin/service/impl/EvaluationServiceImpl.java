package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.*;
import com.aiagent.admin.domain.repository.*;
import com.aiagent.admin.service.EvaluationService;
import com.aiagent.admin.service.ModelConfigService;
import com.aiagent.admin.service.mapper.EvaluationMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评估服务实现类
 * <p>
 * 提供模型评估的核心功能：
 * <ul>
 *   <li>评估任务创建、更新、删除、查询</li>
 *   <li>异步执行评估任务（委托给 EvaluationAsyncService）</li>
 *   <li>评估结果收集和统计</li>
 *   <li>评估任务取消支持</li>
 *   <li>评估对比（比较两个任务的指标）</li>
 * </ul>
 * </p>
 * <p>
 * 异步执行由 EvaluationAsyncService 处理，避免 @Async 自调用不生效的问题。
 * </p>
 *
 * @see EvaluationService
 * @see EvaluationAsyncService
 * @see EvaluationJob
 * @see EvaluationResult
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationJobRepository evaluationJobRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final DatasetRepository datasetRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final ModelConfigService modelConfigService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EvaluationMapper evaluationMapper;
    private final EvaluationAsyncService evaluationAsyncService;

    /**
     * 创建新的评估任务
     * <p>
     * 验证请求参数后创建评估任务实体。
     * 需要指定提示词模板、模型配置和数据集。
     * </p>
     *
     * @param request 创建任务请求
     * @return 创建成功的任务响应 DTO
     * @throws IllegalArgumentException 必要参数缺失时抛出
     */
    @Override
    @Transactional
    public EvaluationJobResponse createJob(EvaluationJobCreateRequest request) {
        validateJobRequest(request);

        EvaluationJob entity = evaluationMapper.toJobEntity(request);
        EvaluationJob saved = evaluationJobRepository.save(entity);
        return toJobResponseWithNames(saved);
    }

    /**
     * 更新评估任务配置
     * <p>
     * 不允许更新正在运行中的任务。
     * </p>
     *
     * @param id      任务唯一标识
     * @param request 更新请求
     * @return 更新后的任务响应 DTO
     * @throws EntityNotFoundException   任务不存在时抛出
     * @throws IllegalStateException      任务正在运行时抛出
     */
    @Override
    @Transactional
    public EvaluationJobResponse updateJob(String id, EvaluationJobUpdateRequest request) {
        EvaluationJob existing = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));

        if (existing.getStatus() == EvaluationJob.JobStatus.RUNNING) {
            throw new IllegalStateException("Cannot update a running job");
        }

        evaluationMapper.updateJobEntity(existing, request);
        EvaluationJob updated = evaluationJobRepository.save(existing);
        return toJobResponseWithNames(updated);
    }

    /**
     * 删除评估任务及其所有结果
     * <p>
     * 不允许删除正在运行中的任务。
     * 同时删除任务实体和所有评估结果记录。
     * </p>
     *
     * @param id 任务唯一标识
     * @throws EntityNotFoundException  任务不存在时抛出
     * @throws IllegalStateException     任务正在运行时抛出
     */
    @Override
    @Transactional
    public void deleteJob(String id) {
        EvaluationJob job = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));

        if (job.getStatus() == EvaluationJob.JobStatus.RUNNING) {
            throw new IllegalStateException("Cannot delete a running job");
        }

        evaluationResultRepository.deleteByJobId(id);
        evaluationJobRepository.delete(job);
    }

    /**
     * 根据ID获取评估任务详情
     *
     * @param id 任务唯一标识
     * @return 任务响应 DTO（包含关联实体名称）
     * @throws EntityNotFoundException 任务不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public EvaluationJobResponse getJob(String id) {
        EvaluationJob entity = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));
        return toJobResponseWithNames(entity);
    }

    /**
     * 分页查询评估任务列表
     * <p>
     * 支持按状态、关键词筛选，按创建时间倒序排列。
     * </p>
     *
     * @param status   状态过滤条件（可选）
     * @param keyword  搜索关键词（可选）
     * @param pageable 分页参数
     * @return 分页的任务响应 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<EvaluationJobResponse> listJobs(String status, String keyword, Pageable pageable) {
        Page<EvaluationJob> page;
        
        if (keyword != null && !keyword.isEmpty()) {
            page = evaluationJobRepository.searchByKeyword(keyword, pageable);
        } else if (status != null && !status.isEmpty()) {
            try {
                EvaluationJob.JobStatus jobStatus = EvaluationJob.JobStatus.valueOf(status.toUpperCase());
                page = evaluationJobRepository.findByStatusOrderByCreatedAtDesc(jobStatus, pageable);
            } catch (IllegalArgumentException e) {
                page = evaluationJobRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else {
            page = evaluationJobRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Page<EvaluationJobResponse> responsePage = page.map(this::toJobResponseWithNames);
        return PageResponse.from(responsePage);
    }

    /**
     * 异步执行评估任务
     * <p>
     * 委托给 EvaluationAsyncService 执行，避免 @Async 自调用不生效的问题。
     * 执行流程：
     * <ol>
     *   <li>验证任务、提示词模板、模型配置、数据集存在</li>
     *   <li>预加载所需数据并更新状态为 PENDING（异步执行前）</li>
     *   <li>委托 EvaluationAsyncService 异步执行评估</li>
     * </ol>
     * </p>
     *
     * @param id 任务唯一标识
     * @return 异步执行结果（立即返回，异步执行在后端进行）
     * @throws EntityNotFoundException   任务或关联实体不存在时抛出
     * @throws IllegalStateException      任务已在运行时抛出
     */
    @Override
    @Transactional
    public EvaluationJobResponse runJob(String id) {
        EvaluationJob job = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));

        if (job.getStatus() == EvaluationJob.JobStatus.RUNNING) {
            throw new IllegalStateException("Job is already running");
        }

        // 验证并加载所需数据
        PromptTemplate promptTemplate;
        if (job.getPromptTemplateId() != null && !job.getPromptTemplateId().isEmpty()) {
            promptTemplate = promptTemplateRepository.findById(job.getPromptTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("Prompt template not found"));
            job.setPromptTemplateVersion(promptTemplate.getVersion());
        } else {
            promptTemplate = null;
        }

        ModelConfig modelConfig;
        if (job.getModelConfigId() != null && !job.getModelConfigId().isEmpty()) {
            modelConfig = modelConfigRepository.findById(job.getModelConfigId())
                    .orElseThrow(() -> new EntityNotFoundException("Model config not found"));
        } else {
            modelConfig = modelConfigService.findDefaultEntity()
                    .orElseThrow(() -> new EntityNotFoundException("No default model configured"));
            log.info("Using default model {} for evaluation job {}", modelConfig.getId(), id);
        }

        Dataset dataset = datasetRepository.findById(job.getDatasetId())
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found"));

        // RAG 配置处理
        if (Boolean.TRUE.equals(job.getEnableRag()) && job.getKnowledgeBaseId() != null
                && (job.getEmbeddingModelId() == null || job.getEmbeddingModelId().isEmpty())) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(job.getKnowledgeBaseId())
                    .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found"));
            if (kb.getDefaultEmbeddingModelId() != null && !kb.getDefaultEmbeddingModelId().isEmpty()) {
                job.setEmbeddingModelId(kb.getDefaultEmbeddingModelId());
                log.info("Using knowledge base default embedding model {} for RAG evaluation", kb.getDefaultEmbeddingModelId());
            }
        }

        // 预加载数据集项（避免懒加载问题）
        List<DatasetItem> items = datasetItemRepository.findByDatasetIdAndVersionAndStatusNot(
                job.getDatasetId(), dataset.getVersion(), DatasetItem.ItemStatus.DELETED);

        // 更新任务状态为 RUNNING（异步执行前）
        job.setStatus(EvaluationJob.JobStatus.RUNNING);
        // 设置开始时间
        job.setStartedAt(LocalDateTime.now());
        // 清除完成数量
        job.setCompletedItems(0);
        // 清除完成时间
        job.setCompletedAt(null);
        // 清除成功和失败数量
        job.setSuccessCount(0);
        job.setFailedCount(0);
        // 清除总延迟
        // 清除总延迟
        job.setTotalLatencyMs(0L);
        // 清除总 Token 使用
        job.setTotalInputTokens(0L);
        job.setTotalOutputTokens(0L);
        // 清除错误信息
        job.setErrorMessage(null);
        job.setTotalItems(items.size());
        evaluationJobRepository.save(job);


        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 委托异步服务执行
                    evaluationAsyncService.executeEvaluation(
                            id, items, promptTemplate, modelConfig,
                            job.getEmbeddingModelId(), job.getKnowledgeBaseId(), job.getEnableRag(),
                            items.size()  // 传递 totalItems，避免事务可见性问题
                    );
                }
            });
        } else {
            // 委托异步服务执行
            evaluationAsyncService.executeEvaluation(
                    id, items, promptTemplate, modelConfig,
                    job.getEmbeddingModelId(), job.getKnowledgeBaseId(), job.getEnableRag(),
                    items.size()  // 传递 totalItems，避免事务可见性问题
            );
        }


        return toJobResponseWithNames(job);
    }

    /**
     * 取消正在运行的评估任务
     * <p>
     * 委托给 EvaluationAsyncService 设置取消标记，任务循环会在下一次迭代时检查并停止。
     * </p>
     *
     * @param id 任务唯一标识
     * @throws EntityNotFoundException  任务不存在时抛出
     * @throws IllegalStateException     任务不在运行状态时抛出
     */
    @Override
    public void cancelJob(String id) {
        EvaluationJob job = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));

        if (job.getStatus() != EvaluationJob.JobStatus.RUNNING) {
            throw new IllegalStateException("Job is not running");
        }

        evaluationAsyncService.requestCancellation(id);
    }

    /**
     * 分页查询评估任务的评估结果
     * <p>
     * 按创建时间升序排列，返回任务的评估结果列表。
     * </p>
     *
     * @param jobId    任务唯一标识
     * @param pageable 分页参数
     * @return 分页的评估结果响应 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<EvaluationResultResponse> listResults(String jobId, Pageable pageable) {
        Page<EvaluationResult> page = evaluationResultRepository.findByJobIdOrderByCreatedAtAsc(jobId, pageable);
        Page<EvaluationResultResponse> responsePage = page.map(evaluationMapper::toResultResponse);
        return PageResponse.from(responsePage);
    }

    /**
     * 根据ID获取单个评估结果
     *
     * @param resultId 结果唯一标识
     * @return 评估结果响应 DTO
     * @throws EntityNotFoundException 结果不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public EvaluationResultResponse getResult(String resultId) {
        EvaluationResult result = evaluationResultRepository.findById(resultId)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation result not found with id: " + resultId));
        return evaluationMapper.toResultResponse(result);
    }

    /**
     * 获取评估任务的指标统计
     * <p>
     * 返回任务的汇总指标：成功率、平均延迟、Token 使用等。
     * 同时计算平均 AI 得分、语义相似度、检索得分、忠实度等质量指标。
     * </p>
     *
     * @param jobId 任务唯一标识
     * @return 指标响应 DTO
     * @throws EntityNotFoundException 任务不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public EvaluationMetricsResponse getMetrics(String jobId) {
        EvaluationJob job = evaluationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + jobId));

        EvaluationMetricsResponse metrics = evaluationMapper.toMetricsResponse(job);

        // 计算平均得分等指标（从评估结果表聚合）
        metrics.setAverageScore(evaluationResultRepository.calculateAverageScoreByJobId(jobId));
        metrics.setAverageSemanticSimilarity(evaluationResultRepository.calculateAverageSemanticSimilarityByJobId(jobId));
        metrics.setAverageFaithfulness(evaluationResultRepository.calculateAverageFaithfulnessByJobId(jobId));

        return metrics;
    }

    /**
     * 对比两个评估任务的指标
     * <p>
     * 返回两个任务的详细指标对比数据，用于分析模型或提示词的差异。
     * </p>
     *
     * @param request 对比请求，包含两个任务的 ID
     * @return 对比响应 DTO
     * @throws EntityNotFoundException 任一任务不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public EvaluationCompareResponse compareJobs(EvaluationCompareRequest request) {
        EvaluationJob job1 = evaluationJobRepository.findById(request.getJobId1())
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + request.getJobId1()));
        
        EvaluationJob job2 = evaluationJobRepository.findById(request.getJobId2())
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + request.getJobId2()));
        
        EvaluationCompareResponse response = new EvaluationCompareResponse();
        response.setJob1(toJobResponseWithNames(job1));
        response.setJob2(toJobResponseWithNames(job2));
        response.setMetrics(evaluationMapper.toComparisonMetrics(job1, job2));
        
        return response;
    }

    /**
     * 验证评估任务创建请求参数
     * <p>
     * 检查必要参数：模型配置 ID、数据集 ID。
     * Prompt模板ID是可选的，如果不指定则使用默认系统提示词。
     * </p>
     *
     * @param request 创建任务请求
     * @throws IllegalArgumentException 必要参数缺失时抛出
     */
    private void validateJobRequest(EvaluationJobCreateRequest request) {
        // promptTemplateId 和 modelConfigId 现在都是可选的
        // 如果不指定 modelConfigId，评估时会使用系统默认对话模型
        if (request.getDatasetId() == null || request.getDatasetId().isEmpty()) {
            throw new IllegalArgumentException("Dataset ID is required");
        }
    }

    /**
     * 将评估任务实体转换为响应 DTO（包含关联实体名称）
     * <p>
     * 除了基本字段转换，还查询并填充：
     * <ul>
     *   <li>提示词模板名称</li>
     *   <li>模型配置名称</li>
     *   <li>数据集名称</li>
     *   <li>计算的成功率和平均延迟</li>
     * </ul>
     * </p>
     *
     * @param job 评估任务实体
     * @return 包含完整信息的任务响应 DTO
     */
    private EvaluationJobResponse toJobResponseWithNames(EvaluationJob job) {
        EvaluationJobResponse response = evaluationMapper.toJobResponse(job);

        // Fetch names for related entities (promptTemplateId is optional)
        if (job.getPromptTemplateId() != null && !job.getPromptTemplateId().isEmpty()) {
            promptTemplateRepository.findById(job.getPromptTemplateId())
                    .ifPresent(pt -> response.setPromptTemplateName(pt.getName()));
        }

        // Fetch model config name if specified
        if (job.getModelConfigId() != null && !job.getModelConfigId().isEmpty()) {
            modelConfigRepository.findById(job.getModelConfigId())
                    .ifPresent(mc -> response.setModelConfigName(mc.getName()));
        }

        datasetRepository.findById(job.getDatasetId())
                .ifPresent(ds -> response.setDatasetName(ds.getName()));

        // Fill embedding model name if specified
        if (job.getEmbeddingModelId() != null && !job.getEmbeddingModelId().isEmpty()) {
            modelConfigRepository.findById(job.getEmbeddingModelId())
                    .ifPresent(em -> response.setEmbeddingModelName(em.getName()));
        }

        // Fill knowledge base name if specified
        if (job.getKnowledgeBaseId() != null && !job.getKnowledgeBaseId().isEmpty()) {
            knowledgeBaseRepository.findById(job.getKnowledgeBaseId())
                    .ifPresent(kb -> response.setKnowledgeBaseName(kb.getName()));
        }

        // Set computed metrics
        response.setSuccessRate(job.getSuccessRate());
        response.setAverageLatencyMs(job.getAverageLatencyMs());

        return response;
    }

    /**
     * 重新运行评估任务
     * <p>
     * 删除之前的评估结果，重置任务状态和计数器，然后委托给 runJob 异步执行。
     * 注意：先保存 PENDING 状态，然后在事务提交后再启动异步任务，避免状态不一致。
     * </p>
     *
     * @param id 任务唯一标识
     * @return 异步执行结果（立即返回）
     * @throws EntityNotFoundException 任务不存在时抛出
     * @throws IllegalStateException   任务正在运行时抛出
     */
    @Override
    @Transactional
    public EvaluationJobResponse rerunJob(String id) {
        EvaluationJob job = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));

        if (job.getStatus() == EvaluationJob.JobStatus.RUNNING) {
            throw new IllegalStateException("Job is already running, cannot rerun");
        }

        // 删除之前的评估结果
        evaluationResultRepository.deleteByJobId(id);

        // 重置任务状态为 PENDING
        job.setStatus(EvaluationJob.JobStatus.PENDING);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setCompletedItems(0);
        job.setSuccessCount(0);
        job.setFailedCount(0);
        job.setTotalLatencyMs(0L);
        job.setTotalInputTokens(0L);
        job.setTotalOutputTokens(0L);
        job.setErrorMessage(null);
        EvaluationJob savedJob = evaluationJobRepository.save(job);

        // 注册事务提交后的回调，确保事务提交后再启动异步任务
        // 这样可以确保前端查询时能看到 PENDING -> RUNNING 的状态转换
        // 通过 evaluationAsyncService.triggerRerunAfterCommit 调用，避免 @Transactional 自调用问题
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 事务已提交，通过异步服务触发 runJob，确保 @Transactional 生效
                    evaluationAsyncService.triggerRerunAfterCommit(EvaluationServiceImpl.this, id);
                }
            });
        } else {
            // 如果没有事务（理论上不应该发生），直接启动异步任务
            evaluationAsyncService.triggerRerunAfterCommit(EvaluationServiceImpl.this, id);
        }

        // 立即返回前端响应（基于保存的 PENDING 状态）
        return toJobResponseWithNames(savedJob);
    }
}
