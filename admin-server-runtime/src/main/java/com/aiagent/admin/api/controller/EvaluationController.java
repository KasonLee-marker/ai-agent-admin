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

/**
 * 模型评估管理 REST 控制器
 * <p>
 * 提供模型评估任务的管理 API：
 * <ul>
 *   <li>评估任务创建、查询、更新、删除</li>
 *   <li>异步执行评估任务</li>
 *   <li>任务取消支持</li>
 *   <li>评估结果查询</li>
 *   <li>评估指标统计</li>
 *   <li>评估对比（比较两个任务）</li>
 * </ul>
 * </p>
 * <p>
 * 评估任务使用指定的提示词模板、模型配置和数据集，
 * 批量调用模型并收集性能指标。
 * </p>
 *
 * @see EvaluationService
 */
@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@Tag(name = "Evaluation Management", description = "APIs for managing evaluation jobs and results")
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * 创建新的评估任务
     *
     * @param request 评估任务创建请求，包含提示词、模型、数据集配置
     * @return 创建成功的评估任务信息
     */
    @PostMapping
    @Operation(summary = "Create a new evaluation job")
    public ApiResponse<EvaluationJobResponse> createJob(
            @Valid @RequestBody EvaluationJobCreateRequest request) {
        return ApiResponse.success(evaluationService.createJob(request));
    }

    /**
     * 根据ID获取评估任务详情
     *
     * @param id 评估任务ID
     * @return 评估任务详情信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get evaluation job by ID")
    public ApiResponse<EvaluationJobResponse> getJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        return ApiResponse.success(evaluationService.getJob(id));
    }

    /**
     * 分页查询评估任务列表
     * <p>
     * 支持按状态、关键词筛选，结果按创建时间倒序排列。
     * </p>
     *
     * @param status  状态筛选（可选，如 PENDING、RUNNING、COMPLETED）
     * @param keyword 搜索关键词（可选，匹配任务名称）
     * @param page    页码（从0开始）
     * @param size    每页数量
     * @return 分页的评估任务列表
     */
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

    /**
     * 更新评估任务配置
     * <p>
     * 仅允许更新未开始执行的任务。
     * </p>
     *
     * @param id      评估任务ID
     * @param request 更新请求，包含新的配置信息
     * @return 更新后的评估任务信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an evaluation job")
    public ApiResponse<EvaluationJobResponse> updateJob(
            @Parameter(description = "Job ID") @PathVariable String id,
            @Valid @RequestBody EvaluationJobUpdateRequest request) {
        return ApiResponse.success(evaluationService.updateJob(id, request));
    }

    /**
     * 删除评估任务
     * <p>
     * 同时删除任务的所有评估结果。
     * </p>
     *
     * @param id 评估任务ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an evaluation job")
    public ApiResponse<Void> deleteJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        evaluationService.deleteJob(id);
        return ApiResponse.success();
    }

    /**
     * 执行评估任务
     * <p>
     * 异步执行评估，使用指定提示词和模型批量处理数据集，
     * 收集性能指标（响应质量、语义相似度等）。
     * </p>
     *
     * @param id 评估任务ID
     * @return 开始执行后的评估任务信息（状态变为 RUNNING）
     */
    @PostMapping("/{id}/run")
    @Operation(summary = "Run an evaluation job")
    public ApiResponse<EvaluationJobResponse> runJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        return ApiResponse.success(evaluationService.runJob(id));
    }

    /**
     * 取消正在执行的评估任务
     * <p>
     * 仅当任务状态为 RUNNING 时可取消。
     * </p>
     *
     * @param id 评估任务ID
     * @return 成功响应（无数据）
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a running evaluation job")
    public ApiResponse<Void> cancelJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        evaluationService.cancelJob(id);
        return ApiResponse.success();
    }

    /**
     * 重新运行评估任务
     * <p>
     * 删除之前的所有评估结果，重新执行评估。
     * </p>
     *
     * @param id 评估任务ID
     * @return 开始重新执行后的评估任务信息
     */
    @PostMapping("/{id}/rerun")
    @Operation(summary = "Rerun an evaluation job (delete previous results and run again)")
    public ApiResponse<EvaluationJobResponse> rerunJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        return ApiResponse.success(evaluationService.rerunJob(id));
    }

    /**
     * 分页查询评估任务的评估结果
     * <p>
     * 结果按创建时间升序排列（按数据项顺序）。
     * </p>
     *
     * @param id   评估任务ID
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 分页的评估结果列表
     */
    @GetMapping("/{id}/results")
    @Operation(summary = "Get evaluation results for a job")
    public ApiResponse<PageResponse<EvaluationResultResponse>> listResults(
            @Parameter(description = "Job ID") @PathVariable String id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ApiResponse.success(evaluationService.listResults(id, pageable));
    }

    /**
     * 根据ID获取单个评估结果详情
     *
     * @param resultId 评估结果ID
     * @return 评估结果详情信息
     */
    @GetMapping("/results/{resultId}")
    @Operation(summary = "Get a single evaluation result by ID")
    public ApiResponse<EvaluationResultResponse> getResult(
            @Parameter(description = "Result ID") @PathVariable String resultId) {
        return ApiResponse.success(evaluationService.getResult(resultId));
    }

    /**
     * 获取评估任务的指标统计
     * <p>
     * 返回汇总指标：平均响应质量、平均语义相似度、平均检索得分等。
     * </p>
     *
     * @param id 评估任务ID
     * @return 评估指标统计信息
     */
    @GetMapping("/{id}/metrics")
    @Operation(summary = "Get evaluation metrics for a job")
    public ApiResponse<EvaluationMetricsResponse> getMetrics(
            @Parameter(description = "Job ID") @PathVariable String id) {
        return ApiResponse.success(evaluationService.getMetrics(id));
    }

    /**
     * 对比两个评估任务
     * <p>
     * 比较两个任务在相同数据集上的表现差异，
     * 用于评估不同提示词或模型的效果。
     * </p>
     *
     * @param request 对比请求，包含两个任务的ID
     * @return 评估对比结果
     */
    @PostMapping("/compare")
    @Operation(summary = "Compare two evaluation jobs")
    public ApiResponse<EvaluationCompareResponse> compareJobs(
            @Valid @RequestBody EvaluationCompareRequest request) {
        return ApiResponse.success(evaluationService.compareJobs(request));
    }
}
