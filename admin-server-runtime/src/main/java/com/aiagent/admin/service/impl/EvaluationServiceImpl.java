package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.*;
import com.aiagent.admin.domain.repository.*;
import com.aiagent.admin.service.*;
import com.aiagent.admin.service.mapper.EvaluationMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
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
    private final ModelConfigService modelConfigService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EvaluationMapper evaluationMapper;
    private final EncryptionService encryptionService;
    private final EmbeddingService embeddingService;
    private final DocumentService documentService;

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

        // Prompt模板是可选的，如果未指定则使用默认系统提示词
        PromptTemplate promptTemplate = null;
        if (job.getPromptTemplateId() != null && !job.getPromptTemplateId().isEmpty()) {
            promptTemplate = promptTemplateRepository.findById(job.getPromptTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("Prompt template not found"));
            // 记录使用的提示词模板版本号（便于复现）
            job.setPromptTemplateVersion(promptTemplate.getVersion());
        }

        // 对话模型是可选的，如果未指定则使用系统默认对话模型
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

        // 如果启用了 RAG 但没有指定 embedding 模型，从知识库获取默认配置
        if (Boolean.TRUE.equals(job.getEnableRag()) && job.getKnowledgeBaseId() != null
                && (job.getEmbeddingModelId() == null || job.getEmbeddingModelId().isEmpty())) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(job.getKnowledgeBaseId())
                    .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found"));
            if (kb.getDefaultEmbeddingModelId() != null && !kb.getDefaultEmbeddingModelId().isEmpty()) {
                job.setEmbeddingModelId(kb.getDefaultEmbeddingModelId());
                log.info("Using knowledge base default embedding model {} for RAG evaluation", kb.getDefaultEmbeddingModelId());
            }
        }

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
     * 根据是否启用RAG选择评估模式：
     * <ul>
     *   <li>基础模式：渲染提示词并调用模型，计算AI评分</li>
     *   <li>RAG模式：检索文档、计算检索指标、构建RAG提示词、计算忠实度</li>
     * </ul>
     * 统一计算语义相似度（如有期望输出）。
     * </p>
     *
     * @param job             评估任务实体
     * @param item            数据集项实体
     * @param promptTemplate  提示词模板实体
     * @param modelConfig     模型配置实体
     */
    private void evaluateItem(EvaluationJob job, DatasetItem item, PromptTemplate promptTemplate, ModelConfig modelConfig) {
        long startTime = System.currentTimeMillis();

        EvaluationResult result = EvaluationResult.builder()
                .jobId(job.getId())
                .datasetItemId(item.getId())
                .input(item.getInput())
                .expectedOutput(item.getOutput())
                .status(EvaluationResult.ResultStatus.PENDING)
                .build();

        try {
            OpenAiChatClient chatClient = buildChatClient(modelConfig);

            // 根据是否启用RAG选择评估流程
            String renderedPrompt;
            List<VectorSearchResult> retrievedDocs = null;

            // 获取基础提示词模板（如果有）
            String baseTemplate = promptTemplate != null ? promptTemplate.getContent() : null;

            if (Boolean.TRUE.equals(job.getEnableRag()) && job.getKnowledgeBaseId() != null) {
                // RAG评估流程 - 使用评估任务指定的 embedding 模型进行检索
                retrievedDocs = retrieveDocuments(job.getKnowledgeBaseId(), job.getEmbeddingModelId(), item.getInput(), 5);
                result.setRetrievedDocIds(serializeDocIds(retrievedDocs));

                // 计算检索评估指标（如有期望文档ID）
                if (item.getExpectedDocIds() != null && !item.getExpectedDocIds().isEmpty()) {
                    result.setRetrievalScore(calculateRetrievalMetrics(item.getExpectedDocIds(), retrievedDocs));
                }

                // 构建包含检索上下文的提示词
                renderedPrompt = buildRagPrompt(baseTemplate, item.getInput(), retrievedDocs);
            } else {
                // 基础评估流程
                renderedPrompt = renderPrompt(baseTemplate, item.getInput());
            }

            result.setRenderedPrompt(renderedPrompt);

            // 调用模型生成回答
            Prompt prompt = new Prompt(new UserMessage(renderedPrompt));
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            String output = response.getResult().getOutput().getContent();
            long latency = System.currentTimeMillis() - startTime;

            result.setActualOutput(output);
            result.setLatencyMs((int) latency);
            result.setStatus(EvaluationResult.ResultStatus.SUCCESS);

            // 提取Token使用量
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                Usage usage = response.getMetadata().getUsage();
                result.setInputTokens(usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : 0);
                result.setOutputTokens(usage.getGenerationTokens() != null ? usage.getGenerationTokens().intValue() : 0);
            }

            // 计算语义相似度（如有期望输出）
            if (item.getOutput() != null && !item.getOutput().isEmpty() && output != null && !output.isEmpty()) {
                try {
                    // 使用指定的 embedding 模型或默认模型
                    Float similarity;
                    if (job.getEmbeddingModelId() != null && !job.getEmbeddingModelId().isEmpty()) {
                        ModelConfig embeddingConfig = modelConfigRepository.findById(job.getEmbeddingModelId())
                                .orElse(null);
                        if (embeddingConfig != null) {
                            similarity = embeddingService.semanticSimilarityWithModel(item.getOutput(), output, embeddingConfig);
                        } else {
                            log.warn("Specified embedding model {} not found, using default", job.getEmbeddingModelId());
                            similarity = embeddingService.semanticSimilarity(item.getOutput(), output);
                        }
                    } else {
                        similarity = embeddingService.semanticSimilarity(item.getOutput(), output);
                    }
                    result.setSemanticSimilarity(similarity);
                } catch (Exception e) {
                    log.warn("Failed to calculate semantic similarity for item {}: {}", item.getId(), e.getMessage());
                }
            }

            // 计算事实忠实度（RAG模式且有上下文）
            if (Boolean.TRUE.equals(job.getEnableRag()) && retrievedDocs != null && !retrievedDocs.isEmpty() && output != null) {
                try {
                    result.setFaithfulness(calculateFaithfulness(retrievedDocs, output, chatClient));
                } catch (Exception e) {
                    log.warn("Failed to calculate faithfulness for item {}: {}", item.getId(), e.getMessage());
                }
            }

            // AI评分（如果有期望输出）
            if (item.getOutput() != null && !item.getOutput().isEmpty() && output != null && !output.isEmpty()) {
                try {
                    EvaluationScore score = evaluateScore(item.getInput(), item.getOutput(), output, chatClient);
                    result.setScore(score.getScore());
                    result.setScoreReason(score.getReason());
                } catch (Exception e) {
                    log.warn("Failed to evaluate score for item {}: {}", item.getId(), e.getMessage());
                }
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
     * 检索相关文档
     *
     * @param knowledgeBaseId 知识库ID
     * @param embeddingModelId Embedding模型ID（可选，用于指定检索时使用的向量表）
     * @param query      查询文本
     * @param topK       返回数量
     * @return 检索结果列表
     */
    private List<VectorSearchResult> retrieveDocuments(String knowledgeBaseId, String embeddingModelId, String query, int topK) {
        VectorSearchRequest request = new VectorSearchRequest();
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setEmbeddingModelId(embeddingModelId);
        request.setQuery(query);
        request.setTopK(topK);
        request.setThreshold(0.3); // 降低阈值以获取更多相关结果
        return documentService.searchSimilar(request);
    }

    /**
     * 构建RAG提示词（包含检索上下文）
     *
     * @param template      提示词模板（可选）
     * @param input         用户输入
     * @param retrievedDocs 检索到的文档
     * @return 渲染后的提示词
     */
    private String buildRagPrompt(String template, String input, List<VectorSearchResult> retrievedDocs) {
        // 构建上下文文本
        String context = retrievedDocs.stream()
                .map(VectorSearchResult::getContent)
                .reduce((a, b) -> a + "\n\n---\n\n" + b)
                .orElse("");

        // 如果没有提供模板，使用默认的RAG提示词模板
        if (template == null || template.isEmpty()) {
            template = "请根据以下参考信息回答问题。如果参考信息中没有相关内容，请说明。\n\n" +
                    "参考信息：\n{context}\n\n问题：{{input}}";
        }

        // 渲染模板（支持 {context} 和 {input} 变量）
        String result = template.replace("{context}", context);
        result = result.replace("{{input}}", input != null ? input : "");

        return result;
    }

    /**
     * 序列化检索到的文档ID列表
     */
    private String serializeDocIds(List<VectorSearchResult> docs) {
        if (docs == null || docs.isEmpty()) return null;
        List<String> ids = docs.stream().map(VectorSearchResult::getChunkId).toList();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(ids);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算检索评估指标（Recall@K）
     *
     * @param expectedDocIds 期望文档ID列表（JSON格式）
     * @param retrievedDocs  实际检索到的文档
     * @return Recall得分（0-1）
     */
    private Float calculateRetrievalMetrics(String expectedDocIds, List<VectorSearchResult> retrievedDocs) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<String> expected = mapper.readValue(expectedDocIds, List.class);
            List<String> retrieved = retrievedDocs.stream()
                    .map(VectorSearchResult::getChunkId)
                    .toList();

            // 计算Recall: 期望文档被检索到的比例
            int hitCount = 0;
            for (String expectedId : expected) {
                if (retrieved.contains(expectedId)) {
                    hitCount++;
                }
            }

            return expected.isEmpty() ? 0f : (float) hitCount / expected.size();
        } catch (Exception e) {
            log.warn("Failed to parse expected doc IDs: {}", expectedDocIds);
            return null;
        }
    }

    /**
     * 计算事实忠实度
     * <p>
     * 使用LLM评估答案是否忠实于检索到的上下文。
     * </p>
     *
     * @param context    检索到的文档上下文
     * @param answer     模型生成的答案
     * @param chatClient 模型客户端
     * @return 忠实度得分（0-1）
     */
    private Float calculateFaithfulness(List<VectorSearchResult> context, String answer, OpenAiChatClient chatClient) {
        String contextText = context.stream()
                .map(VectorSearchResult::getContent)
                .reduce((a, b) -> a + "\n\n---\n\n" + b)
                .orElse("");

        String evalPrompt = """
                评估以下答案是否忠实于给定的上下文内容。
                上下文：%s
                答案：%s
                
                请判断答案中的信息是否都来自上下文，没有捏造或超出上下文的内容。
                返回JSON格式（不要包含其他内容）：
                {"faithfulness": <0-1之间的数值>, "reason": "<简要说明>"}
                """.formatted(contextText.substring(0, Math.min(2000, contextText.length())), answer);

        try {
            Prompt prompt = new Prompt(new UserMessage(evalPrompt));
            String response = chatClient.call(prompt).getResult().getOutput().getContent();

            // 解析JSON响应
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String json = response.substring(start, end + 1);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> map = mapper.readValue(json, Map.class);
                return ((Number) map.get("faithfulness")).floatValue();
            }
        } catch (Exception e) {
            log.warn("Failed to calculate faithfulness: {}", e.getMessage());
        }

        return null;
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
     * 使用 AI 评估响应质量
     * <p>
     * 构建评估提示词，让 AI 对比期望输出和实际输出，给出分数和理由。
     * 使用独立的新对话，避免上下文污染。
     * </p>
     *
     * @param input      原始输入
     * @param expected   期望输出
     * @param actual     实际输出
     * @param chatClient 模型客户端
     * @return 评估分数和理由
     */
    private EvaluationScore evaluateScore(String input, String expected, String actual, OpenAiChatClient chatClient) {
        String evalPrompt = buildEvaluationPrompt(input, expected, actual);
        Prompt prompt = new Prompt(new UserMessage(evalPrompt));

        org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
        String content = response.getResult().getOutput().getContent();

        return parseScoreResponse(content);
    }

    /**
     * 构建评估提示词
     *
     * @param input    原始输入
     * @param expected 期望输出
     * @param actual   实际输出
     * @return 评估提示词
     */
    private String buildEvaluationPrompt(String input, String expected, String actual) {
        return """
                你是一个评估助手。请评估以下 AI 响应的质量。
                
                输入问题：%s
                期望输出：%s
                实际输出：%s
                
                请给出 0-100 分的质量评分，并简要说明理由。
                评分标准：
                - 完全符合期望输出：90-100 分
                - 大部分符合但有小问题：70-89 分
                - 相关但有明显偏差：50-69 分
                - 不相关或错误：0-49 分
                
                请以 JSON 格式返回（不要包含其他内容）：
                {"score": <分数>, "reason": "<理由>"}
                """.formatted(input, expected, actual);
    }

    /**
     * 解析 AI 评估响应
     *
     * @param content AI 返回的内容
     * @return 评估分数和理由
     */
    private EvaluationScore parseScoreResponse(String content) {
        try {
            // 提取 JSON 部分（可能有额外文本）
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String json = content.substring(start, end + 1);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> map = mapper.readValue(json, Map.class);

                Float score = ((Number) map.get("score")).floatValue();
                String reason = (String) map.get("reason");

                return new EvaluationScore(score, reason);
            }
        } catch (Exception e) {
            log.warn("Failed to parse score response: {}", content);
        }

        // 解析失败时返回默认分数
        return new EvaluationScore(50.0f, "无法解析评估结果");
    }

    /**
     * 评估分数内部类
     */
    @Data
    @AllArgsConstructor
    private static class EvaluationScore {
        private Float score;
        private String reason;
    }

    /**
     * 重新运行评估任务
     * <p>
     * 删除之前的评估结果，重置任务状态和计数器，然后重新执行评估。
     * 支持对已完成、失败或取消的任务进行重新评估。
     * </p>
     *
     * @param id 任务唯一标识
     * @return 异步执行结果（CompletableFuture）
     * @throws EntityNotFoundException 任务不存在时抛出
     * @throws IllegalStateException   任务正在运行时抛出
     */
    @Override
    @Transactional
    public CompletableFuture<EvaluationJobResponse> rerunJob(String id) {
        EvaluationJob job = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));

        if (job.getStatus() == EvaluationJob.JobStatus.RUNNING) {
            throw new IllegalStateException("Job is already running, cannot rerun");
        }

        // 删除之前的评估结果
        evaluationResultRepository.deleteByJobId(id);

        // 重置任务状态和计数器
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
        evaluationJobRepository.save(job);

        // 运行评估任务
        return runJob(id);
    }
}
