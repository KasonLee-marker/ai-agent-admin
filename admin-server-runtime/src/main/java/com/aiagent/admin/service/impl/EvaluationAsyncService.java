package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.*;
import com.aiagent.admin.domain.repository.*;
import com.aiagent.admin.service.DocumentService;
import com.aiagent.admin.service.EmbeddingService;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 评估任务异步执行服务
 * <p>
 * 提供评估任务的异步执行功能，避免 Service 自调用 @Async 不生效的问题。
 * 主要功能：
 * <ul>
 *   <li>异步执行评估任务（遍历数据集项、调用模型、收集结果）</li>
 *   <li>任务取消支持</li>
 *   <li>进度实时更新</li>
 * </ul>
 * </p>
 *
 * @see EvaluationServiceImpl
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationAsyncService {

    private final EvaluationJobRepository evaluationJobRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final ModelConfigService modelConfigService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EmbeddingService embeddingService;
    private final DocumentService documentService;
    private final EncryptionService encryptionService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 提示词变量匹配模式：{{variableName}}
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    /**
     * 任务取消标记缓存
     */
    private final Map<String, Boolean> cancellationFlags = new ConcurrentHashMap<>();

    /**
     * 异步执行评估任务
     * <p>
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
     *
     * @param id        任务唯一标识
     * @param items     数据集项列表（预先加载，避免懒加载问题）
     * @param promptTemplate 提示词模板（可选）
     * @param modelConfig    模型配置
     * @param totalItems     总数据项数量（由 runJob 设置，避免事务可见性问题）
     */
    @Async("evaluationTaskExecutor")
    public void executeEvaluation(String id, List<DatasetItem> items,
                                   PromptTemplate promptTemplate, ModelConfig modelConfig,
                                   String embeddingModelId, String knowledgeBaseId, Boolean enableRag,
                                   int totalItems) {
        // 初始化任务计数器（独立事务，立即提交）
        // 注意：totalItems 从参数获取（由 runJob 设置），避免事务可见性问题
        transactionTemplate.executeWithoutResult(status -> {
            EvaluationJob job = evaluationJobRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("Job not found: " + id));
            job.setTotalItems(totalItems);  // 设置总数据项数量
            job.setCompletedItems(0);
            job.setSuccessCount(0);
            job.setFailedCount(0);
            job.setTotalLatencyMs(0L);
            job.setTotalInputTokens(0L);
            job.setTotalOutputTokens(0L);
            job.setErrorMessage(null);
            evaluationJobRepository.save(job);
        });

        cancellationFlags.put(id, false);

        // 使用 AtomicReference 包装可变状态，以便在 lambda 中访问
        AtomicReference<EvaluationJob.JobStatus> finalStatus = new AtomicReference<>(EvaluationJob.JobStatus.RUNNING);
        AtomicReference<String> errorMessage = new AtomicReference<>(null);

        try {
            for (DatasetItem item : items) {
                if (Boolean.TRUE.equals(cancellationFlags.get(id))) {
                    log.info("Job {} was cancelled", id);
                    finalStatus.set(EvaluationJob.JobStatus.CANCELLED);
                    break;
                }

                // 每个项的评估和保存都在独立事务中，立即提交，前端可见进度
                transactionTemplate.executeWithoutResult(status -> {
                    EvaluationJob job = evaluationJobRepository.findById(id)
                            .orElseThrow(() -> new IllegalStateException("Job not found: " + id));
                    evaluateItem(job, item, promptTemplate, modelConfig, embeddingModelId, knowledgeBaseId, enableRag);
                });
            }

            if (finalStatus.get() == EvaluationJob.JobStatus.RUNNING) {
                finalStatus.set(EvaluationJob.JobStatus.COMPLETED);
            }
        } catch (Exception e) {
            log.error("Error running evaluation job", e);
            finalStatus.set(EvaluationJob.JobStatus.FAILED);
            errorMessage.set(e.getMessage());
        } finally {
            cancellationFlags.remove(id);
        }

        // 最终状态更新（独立事务）
        transactionTemplate.executeWithoutResult(status -> {
            EvaluationJob job = evaluationJobRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("Job not found: " + id));
            job.setStatus(finalStatus.get());
            job.setCompletedAt(LocalDateTime.now());
            if (errorMessage.get() != null) {
                job.setErrorMessage(errorMessage.get());
            }
            evaluationJobRepository.save(job);
            log.info("Evaluation job {} completed with status {}", id, job.getStatus());
        });
    }

    /**
     * 设置取消标记
     *
     * @param jobId 任务 ID
     */
    public void requestCancellation(String jobId) {
        cancellationFlags.put(jobId, true);
    }

    /**
     * 在事务提交后异步启动评估任务
     * <p>
     * 此方法由 EvaluationServiceImpl.rerunJob 在事务提交后调用，
     * 通过 Spring 代理确保 @Transactional 生效。
     * </p>
     *
     * @param evaluationService 评估服务（通过代理注入）
     * @param jobId 任务 ID
     */
    @Async("evaluationTaskExecutor")
    public void triggerRerunAfterCommit(EvaluationServiceImpl evaluationService, String jobId) {
        try {
            evaluationService.runJob(jobId);
        } catch (Exception e) {
            log.error("Failed to trigger rerun for job {}", jobId, e);
        }
    }

    /**
     * 评估单个数据集项
     * <p>
     * 执行流程：
     * <ol>
     *   <li>创建评估结果实体</li>
     *   <li>渲染提示词（支持 RAG 上下文注入）</li>
     *   <li>调用 AI 模型获取响应</li>
     *   <li>计算语义相似度（使用 Embedding API）</li>
     *   <li>计算忠实度（检查响应是否基于检索内容）</li>
     *   <li>AI 评分（0-100 分质量评估）</li>
     *   <li>更新任务统计并保存结果</li>
     * </ol>
     * </p>
     *
     * @param job              评估任务实体
     * @param item             数据集项
     * @param promptTemplate   提示词模板（可选）
     * @param modelConfig      模型配置
     * @param embeddingModelId Embedding 模型 ID（用于相似度计算）
     * @param knowledgeBaseId  知识库 ID（用于 RAG 检索）
     * @param enableRag        是否启用 RAG
     */
    private void evaluateItem(EvaluationJob job, DatasetItem item,
                              PromptTemplate promptTemplate, ModelConfig modelConfig,
                              String embeddingModelId, String knowledgeBaseId, Boolean enableRag) {
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

            String baseTemplate = promptTemplate != null ? promptTemplate.getContent() : null;
            String renderedPrompt;
            List<VectorSearchResult> retrievedDocs = null;

            if (Boolean.TRUE.equals(enableRag) && knowledgeBaseId != null) {
                // RAG评估流程
                retrievedDocs = retrieveDocuments(knowledgeBaseId, embeddingModelId, item.getInput(), 5);
                result.setRetrievedDocIds(serializeDocIds(retrievedDocs));

                renderedPrompt = buildRagPrompt(baseTemplate, item.getInput(), retrievedDocs);
            } else {
                renderedPrompt = renderPrompt(baseTemplate, item.getInput());
            }

            result.setRenderedPrompt(renderedPrompt);

            Prompt prompt = new Prompt(new UserMessage(renderedPrompt));
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            String output = response.getResult().getOutput().getContent();
            long latency = System.currentTimeMillis() - startTime;

            result.setActualOutput(output);
            result.setLatencyMs((int) latency);
            result.setStatus(EvaluationResult.ResultStatus.SUCCESS);

            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                Usage usage = response.getMetadata().getUsage();
                result.setInputTokens(usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : 0);
                result.setOutputTokens(usage.getGenerationTokens() != null ? usage.getGenerationTokens().intValue() : 0);
            }

            // 计算语义相似度
            if (item.getOutput() != null && !item.getOutput().isEmpty() && output != null && !output.isEmpty()) {
                try {
                    Float similarity;
                    if (embeddingModelId != null && !embeddingModelId.isEmpty()) {
                        ModelConfig embeddingConfig = modelConfigRepository.findById(embeddingModelId).orElse(null);
                        if (embeddingConfig != null) {
                            similarity = embeddingService.semanticSimilarityWithModel(item.getOutput(), output, embeddingConfig);
                        } else {
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

            // 计算忠实度
            if (Boolean.TRUE.equals(enableRag) && retrievedDocs != null && !retrievedDocs.isEmpty() && output != null) {
                try {
                    result.setFaithfulness(calculateFaithfulness(retrievedDocs, output, chatClient));
                } catch (Exception e) {
                    log.warn("Failed to calculate faithfulness for item {}: {}", item.getId(), e.getMessage());
                }
            }

            // AI评分
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

    private List<VectorSearchResult> retrieveDocuments(String knowledgeBaseId, String embeddingModelId, String query, int topK) {
        VectorSearchRequest request = new VectorSearchRequest();
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setEmbeddingModelId(embeddingModelId);
        request.setQuery(query);
        request.setTopK(topK);
        request.setThreshold(0.3);
        return documentService.searchSimilar(request);
    }

    private String buildRagPrompt(String template, String input, List<VectorSearchResult> retrievedDocs) {
        String context = retrievedDocs.stream()
                .map(VectorSearchResult::getContent)
                .reduce((a, b) -> a + "\n\n---\n\n" + b)
                .orElse("");

        if (template == null || template.isEmpty()) {
            template = """
                    请根据以下参考信息回答问题。如果参考信息中没有相关内容，请说明。
                    
                    参考信息：
                    {context}
                    
                    问题：{{input}}""";
        }

        String result = template.replace("{context}", context);
        result = result.replace("{{input}}", input != null ? input : "");
        return result;
    }

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

    private OpenAiChatClient buildChatClient(ModelConfig config) {
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : config.getProvider().getDefaultBaseUrl();

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

    private String renderPrompt(String template, String inputData) {
        if (template == null || template.isEmpty()) {
            return inputData;
        }

        String result = template.replace("{{input}}", inputData != null ? inputData : "");

        if (inputData != null && inputData.startsWith("{")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> variables = mapper.readValue(inputData, Map.class);
                Matcher matcher = VARIABLE_PATTERN.matcher(result);
                StringBuilder sb = new StringBuilder();
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

    private EvaluationScore evaluateScore(String input, String expected, String actual, OpenAiChatClient chatClient) {
        String evalPrompt = """
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

        Prompt prompt = new Prompt(new UserMessage(evalPrompt));
        String content = chatClient.call(prompt).getResult().getOutput().getContent();
        return parseScoreResponse(content);
    }

    private EvaluationScore parseScoreResponse(String content) {
        try {
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
        return new EvaluationScore(50.0f, "无法解析评估结果");
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class EvaluationScore {
        private Float score;
        private String reason;
    }
}