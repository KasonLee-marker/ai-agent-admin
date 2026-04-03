package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@Tag(name = "Evaluation Management", description = "APIs for managing evaluation jobs and results")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    @Operation(summary = "Create a new evaluation job")
    public ApiResponse<EvaluationJobResponse> createJob(
            @Valid @RequestBody EvaluationJobCreateRequest request) {
        return ApiResponse.success(evaluationService.createJob(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get evaluation job by ID")
    public ApiResponse<EvaluationJobResponse> getJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        return ApiResponse.success(evaluationService.getJob(id));
    }

    @GetMapping
    @Operation(summary = "List evaluation jobs with pagination and filters")
    public ApiResponse<PageResponse<EvaluationJobResponse>> listJobs(
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(evaluationService.listJobs(status, keyword, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an evaluation job")
    public ApiResponse<EvaluationJobResponse> updateJob(
            @Parameter(description = "Job ID") @PathVariable String id,
            @Valid @RequestBody EvaluationJobUpdateRequest request) {
        return ApiResponse.success(evaluationService.updateJob(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an evaluation job")
    public ApiResponse<Void> deleteJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        evaluationService.deleteJob(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "Run an evaluation job")
    public ApiResponse<EvaluationJobResponse> runJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        evaluationService.runJob(id);
        return ApiResponse.success(evaluationService.getJob(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a running evaluation job")
    public ApiResponse<Void> cancelJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        evaluationService.cancelJob(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get evaluation results for a job")
    public ApiResponse<PageResponse<EvaluationResultResponse>> listResults(
            @Parameter(description = "Job ID") @PathVariable String id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ApiResponse.success(evaluationService.listResults(id, pageable));
    }

    @GetMapping("/results/{resultId}")
    @Operation(summary = "Get a single evaluation result by ID")
    public ApiResponse<EvaluationResultResponse> getResult(
            @Parameter(description = "Result ID") @PathVariable String resultId) {
        return ApiResponse.success(evaluationService.getResult(resultId));
    }

    @GetMapping("/{id}/metrics")
    @Operation(summary = "Get evaluation metrics for a job")
    public ApiResponse<EvaluationMetricsResponse> getMetrics(
            @Parameter(description = "Job ID") @PathVariable String id) {
        return ApiResponse.success(evaluationService.getMetrics(id));
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare two evaluation jobs")
    public ApiResponse<EvaluationCompareResponse> compareJobs(
            @Valid @RequestBody EvaluationCompareRequest request) {
        return ApiResponse.success(evaluationService.compareJobs(request));
    }
}
