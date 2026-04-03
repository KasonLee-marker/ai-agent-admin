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

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private final Map<String, Boolean> cancellationFlags = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public EvaluationJobResponse createJob(EvaluationJobCreateRequest request) {
        validateJobRequest(request);

        EvaluationJob entity = evaluationMapper.toJobEntity(request);
        EvaluationJob saved = evaluationJobRepository.save(entity);
        return toJobResponseWithNames(saved);
    }

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

    @Override
    @Transactional(readOnly = true)
    public EvaluationJobResponse getJob(String id) {
        EvaluationJob entity = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));
        return toJobResponseWithNames(entity);
    }

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

    private void evaluateItem(EvaluationJob job, DatasetItem item, PromptTemplate promptTemplate, ModelConfig modelConfig) {
        long startTime = System.currentTimeMillis();
        String renderedPrompt = renderPrompt(promptTemplate.getContent(), item.getInput());
        
        EvaluationResult result = EvaluationResult.builder()
                .jobId(job.getId())
                .datasetItemId(item.getId())
                .input(item.getInput())
                .expectedOutput(item.getOutput())
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

    @Override
    public void cancelJob(String id) {
        EvaluationJob job = evaluationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + id));
        
        if (job.getStatus() != EvaluationJob.JobStatus.RUNNING) {
            throw new IllegalStateException("Job is not running");
        }
        
        cancellationFlags.put(id, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EvaluationResultResponse> listResults(String jobId, Pageable pageable) {
        Page<EvaluationResult> page = evaluationResultRepository.findByJobIdOrderByCreatedAtAsc(jobId, pageable);
        Page<EvaluationResultResponse> responsePage = page.map(evaluationMapper::toResultResponse);
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationResultResponse getResult(String resultId) {
        EvaluationResult result = evaluationResultRepository.findById(resultId)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation result not found with id: " + resultId));
        return evaluationMapper.toResultResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationMetricsResponse getMetrics(String jobId) {
        EvaluationJob job = evaluationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation job not found with id: " + jobId));
        return evaluationMapper.toMetricsResponse(job);
    }

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
