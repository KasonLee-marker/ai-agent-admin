package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.concurrent.CompletableFuture;

public interface EvaluationService {

    EvaluationJobResponse createJob(EvaluationJobCreateRequest request);

    EvaluationJobResponse updateJob(String id, EvaluationJobUpdateRequest request);

    void deleteJob(String id);

    EvaluationJobResponse getJob(String id);

    PageResponse<EvaluationJobResponse> listJobs(String status, String keyword, Pageable pageable);

    CompletableFuture<EvaluationJobResponse> runJob(String id);

    void cancelJob(String id);

    PageResponse<EvaluationResultResponse> listResults(String jobId, Pageable pageable);

    EvaluationResultResponse getResult(String resultId);

    EvaluationMetricsResponse getMetrics(String jobId);

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
    CompletableFuture<EvaluationJobResponse> rerunJob(String id);
}
