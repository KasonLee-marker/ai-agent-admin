package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
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
}
