package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.*;
import com.aiagent.admin.domain.repository.*;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.EvaluationService;
import com.aiagent.admin.service.mapper.EvaluationMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 评估服务实现类
 * <p>
 * 提供模型评估的核心功能：
 * <ul>
 *   <li>评估任务创建、更新、删除、查询</li>
 *   <li>异步执行评估任务（使用 evaluationTaskExecutor）</li>
 *   <li>评估结果收集和统计</li>
 *   <li>评估任务取消支持</li>
 *   <li>评估对比（比较两个任务的指标）</li>
 * </ul>
 * </p>
 * <p>
 * 评估流程：
 * <ol>
 *   <li>选择提示词模板、模型配置和数据集</li>
 *   <li>遍历数据集项，渲染提示词并调用模型</li>
 *   <li>收集响应、延迟、Token 使用等指标</li>
 *   <li>生成评估报告和统计数据</li>
 * </ol>
 * </p>
 *
 * @see EvaluationService
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
    private final EvaluationMapper evaluationMapper;
    private final EncryptionService encryptionService;

    /**
     * 提示词变量匹配模式：{{variableName}}
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    /** 任务取消标记缓存，用于支持异步任务取消 */
    private final Map<String, Boolean> cancellationFlags = new ConcurrentHashMap<>();

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
     * 使用 evaluationTaskExecutor 线程池异步执行评估。
     * 执行流程：
     * <ol>
     *   <li>验证任务、提示词模板、模型配置、数据集存在</li>
     *   <li>更新任务状态为 RUNNING</li>
     *   <li>遍历数据集项，渲染提示词并调用模型</li>
     *   <li>收集响应并保存评估结果</li>
     *   <li>更新任务统计（成功/失败计数、延迟、Token）</li>
     *   <li>完成后更新状态为 COMPLETED</li>
     * </ol>
     * </p>
     * <p>
     * 支持通过 cancelJob() 方法取消正在运行的任务。
     * </p>
     *
     * @param id 任务唯一标识
     * @return 异步执行结果（CompletableFuture）
     * @throws EntityNotFoundException   任务或关联实体不存在时抛出
     * @throws IllegalStateException      任务已在运行时抛出
     */
    @Override
    @Async("evaluationTaskExecutor")
    public CompletableFuture<EvaluationJobResponse> runJob(String id) {
        EvaluationJob job = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));

        if (job.getStatus() == EvaluationJob.JobStatus.RUNNING) {
            throw new IllegalStateException("Job is already running");
        }

        PromptTemplate promptTemplate = promptTemplateRepository.findById(job.getPromptTemplateId())
                .orElseThrow(() -> new EntityNotFoundException("Prompt template not found"));

        // 记录使用的提示词模板版本号（便于复现）
        job.setPromptTemplateVersion(promptTemplate.getVersion());

        ModelConfig modelConfig = modelConfigRepository.findById(job.getModelConfigId())
                .orElseThrow(() -> new EntityNotFoundException("Model config not found"));

        Dataset dataset = datasetRepository.findById(job.getDatasetId())
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found"));

        List<DatasetItem> items = datasetItemRepository.findByDatasetIdAndVersionAndStatusNot(
                job.getDatasetId(), dataset.getVersion(), DatasetItem.ItemStatus.DELETED);

        job.setStatus(EvaluationJob.JobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        job.setTotalItems(items.size());
        job.setCompletedItems(0);
        job.setSuccessCount(0);
        job.setFailedCount(0);
        job.setTotalLatencyMs(0L);
        job.setTotalInputTokens(0L);
        job.setTotalOutputTokens(0L);
        job.setErrorMessage(null);
        evaluationJobRepository.save(job);

        cancellationFlags.put(id, false);

        try {
            for (DatasetItem item : items) {
                if (Boolean.TRUE.equals(cancellationFlags.get(id))) {
                    log.info("Job {} was cancelled", id);
                    job.setStatus(EvaluationJob.JobStatus.CANCELLED);
                    break;
                }

                evaluateItem(job, item, promptTemplate, modelConfig);
            }

            if (job.getStatus() == EvaluationJob.JobStatus.RUNNING) {
                job.setStatus(EvaluationJob.JobStatus.COMPLETED);
            }
            job.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error running evaluation job", e);
            job.setStatus(EvaluationJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
        } finally {
            cancellationFlags.remove(id);
        }

        EvaluationJob saved = evaluationJobRepository.save(job);
        return CompletableFuture.completedFuture(toJobResponseWithNames(saved));
    }

    /**
     * 评估单个数据集项
     * <p>
     * 渲染提示词模板并调用模型获取响应。
     * 记录响应、延迟、Token 使用等指标到评估结果。
     * 更新任务的统计计数器。
     * </p>
     *
     * @param job             评估任务实体
     * @param item            数据集项实体
     * @param promptTemplate  提示词模板实体
     * @param modelConfig     模型配置实体
     */
    private void evaluateItem(EvaluationJob job, DatasetItem item, PromptTemplate promptTemplate, ModelConfig modelConfig) {
        long startTime = System.currentTimeMillis();
        String renderedPrompt = renderPrompt(promptTemplate.getContent(), item.getInput());
        
        EvaluationResult result = EvaluationResult.builder()
                .jobId(job.getId())
                .datasetItemId(item.getId())
                .input(item.getInput())
                .expectedOutput(item.getOutput())
                .renderedPrompt(renderedPrompt)  // 保存渲染后的提示词便于复现
                .status(EvaluationResult.ResultStatus.PENDING)
                .build();
        
        try {
            OpenAiChatClient chatClient = buildChatClient(modelConfig);
            Prompt prompt = new Prompt(new UserMessage(renderedPrompt));
            
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            String output = response.getResult().getOutput().getContent();
            long latency = System.currentTimeMillis() - startTime;
            
            result.setActualOutput(output);
            result.setLatencyMs((int) latency);
            result.setStatus(EvaluationResult.ResultStatus.SUCCESS);
            
            // Extract token usage if available
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                Usage usage = response.getMetadata().getUsage();
                result.setInputTokens(usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : 0);
                result.setOutputTokens(usage.getGenerationTokens() != null ? usage.getGenerationTokens().intValue() : 0);
            }
            
            job.incrementSuccess();
            job.addLatency(latency);
            if (result.getInputTokens() != null) {
                job.addInputTokens(result.getInputTokens());
            }
            if (result.getOutputTokens() != null) {
                job.addOutputTokens(result.getOutputTokens());
            }
        } catch (Exception e) {
            log.error("Error evaluating item {} for job {}", item.getId(), job.getId(), e);
            result.setActualOutput("");
            result.setStatus(EvaluationResult.ResultStatus.FAILED);
            result.setErrorMessage(e.getMessage());
            result.setLatencyMs((int) (System.currentTimeMillis() - startTime));
            job.incrementFailed();
        }
        
        evaluationResultRepository.save(result);
        job.incrementCompleted();
        evaluationJobRepository.save(job);
    }

    /**
     * 构建 OpenAI ChatClient
     * <p>
     * 根据模型配置创建 OpenAiApi 和 OpenAiChatClient 实例。
     * 配置参数包括：baseUrl、apiKey、modelName、temperature、maxTokens、topP。
     * </p>
     *
     * @param config 模型配置实体
     * @return 配置好的 OpenAiChatClient 实例
     */
    private OpenAiChatClient buildChatClient(ModelConfig config) {
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : 
            config.getProvider().getDefaultBaseUrl();

        OpenAiApi api = new OpenAiApi(baseUrl, apiKey);

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .withModel(config.getModelName());

        if (config.getTemperature() != null) {
            optionsBuilder.withTemperature(config.getTemperature().floatValue());
        }
        if (config.getMaxTokens() != null) {
            optionsBuilder.withMaxTokens(config.getMaxTokens());
        }
        if (config.getTopP() != null) {
            optionsBuilder.withTopP(config.getTopP().floatValue());
        }

        return new OpenAiChatClient(api, optionsBuilder.build());
    }

    /**
     * 渲染提示词模板
     * <p>
     * 支持两种输入格式：
     * <ul>
     *   <li>简单文本：直接替换 {{input}} 变量</li>
     *   <li>JSON 格式：解析变量映射并替换所有模板变量</li>
     * </ul>
     * </p>
     *
     * @param template  提示词模板内容
     * @param inputData 输入数据（文本或 JSON）
     * @return 渲染后的提示词
     */
    private String renderPrompt(String template, String inputData) {
        if (template == null || template.isEmpty()) {
            return inputData;
        }
        
        // Simple variable substitution for {{input}}
        String result = template.replace("{{input}}", inputData != null ? inputData : "");
        
        // Handle other variables from JSON input
        if (inputData != null && inputData.startsWith("{")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> variables = mapper.readValue(inputData, Map.class);
                Matcher matcher = VARIABLE_PATTERN.matcher(result);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String varName = matcher.group(1).trim();
                    Object value = variables.get(varName);
                    matcher.appendReplacement(sb, value != null ? value.toString() : "");
                }
                matcher.appendTail(sb);
                result = sb.toString();
            } catch (Exception e) {
                log.warn("Failed to parse input data as JSON: {}", inputData);
            }
        }
        
        return result;
    }

    /**
     * 取消正在运行的评估任务
     * <p>
     * 设置取消标记，任务循环会在下一次迭代时检查并停止。
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
        
        cancellationFlags.put(id, true);
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
        return evaluationMapper.toMetricsResponse(job);
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
     * 检查必要参数：提示词模板 ID、模型配置 ID、数据集 ID。
     * </p>
     *
     * @param request 创建任务请求
     * @throws IllegalArgumentException 必要参数缺失时抛出
     */
    private void validateJobRequest(EvaluationJobCreateRequest request) {
        if (request.getPromptTemplateId() == null || request.getPromptTemplateId().isEmpty()) {
            throw new IllegalArgumentException("Prompt template ID is required");
        }
        if (request.getModelConfigId() == null || request.getModelConfigId().isEmpty()) {
            throw new IllegalArgumentException("Model config ID is required");
        }
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
        
        // Fetch names for related entities
        promptTemplateRepository.findById(job.getPromptTemplateId())
                .ifPresent(pt -> response.setPromptTemplateName(pt.getName()));
        
        modelConfigRepository.findById(job.getModelConfigId())
                .ifPresent(mc -> response.setModelConfigName(mc.getName()));
        
        datasetRepository.findById(job.getDatasetId())
                .ifPresent(ds -> response.setDatasetName(ds.getName()));
        
        // Set computed metrics
        response.setSuccessRate(job.getSuccessRate());
        response.setAverageLatencyMs(job.getAverageLatencyMs());
        
        return response;
    }
}
