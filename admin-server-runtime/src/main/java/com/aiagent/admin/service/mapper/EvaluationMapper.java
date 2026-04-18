package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.EvaluationJob;
import com.aiagent.admin.domain.entity.EvaluationResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 评估任务与结果实体与 DTO 之间的映射器
 * <p>
 * 使用 MapStruct 实现评估任务（EvaluationJob）、评估结果（EvaluationResult）
 * 与相关 DTO 之间的自动转换，以及评估指标和对比分析的计算。
 * </p>
 *
 * @see EvaluationJob
 * @see EvaluationResult
 * @see EvaluationJobResponse
 * @see EvaluationResultResponse
 * @see EvaluationMetricsResponse
 * @see EvaluationCompareResponse
 */
@Mapper(componentModel = "spring")
public interface EvaluationMapper {

    /**
     * 将评估任务实体转换为响应 DTO
     *
     * @param job 评估任务实体
     * @return 响应 DTO
     */
    EvaluationJobResponse toJobResponse(EvaluationJob job);

    /**
     * 将评估任务实体列表转换为响应 DTO 列表
     *
     * @param jobs 评估任务实体列表
     * @return 响应 DTO 列表
     */
    List<EvaluationJobResponse> toJobResponseList(List<EvaluationJob> jobs);

    /**
     * 将评估结果实体转换为响应 DTO
     *
     * @param result 评估结果实体
     * @return 响应 DTO
     */
    EvaluationResultResponse toResultResponse(EvaluationResult result);

    /**
     * 将评估结果实体列表转换为响应 DTO 列表
     *
     * @param results 评估结果实体列表
     * @return 响应 DTO 列表
     */
    List<EvaluationResultResponse> toResultResponseList(List<EvaluationResult> results);

    /**
     * 将创建请求 DTO 转换为评估任务实体
     * <p>
     * 忽略 id 字段，状态默认设置为 PENDING，计数器默认设置为 0。
     * </p>
     *
     * @param request 创建请求 DTO
     * @return 评估任务实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "totalItems", constant = "0")
    @Mapping(target = "completedItems", constant = "0")
    @Mapping(target = "successCount", constant = "0")
    @Mapping(target = "failedCount", constant = "0")
    @Mapping(target = "totalLatencyMs", ignore = true)
    @Mapping(target = "totalInputTokens", ignore = true)
    @Mapping(target = "totalOutputTokens", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "promptTemplateVersion", ignore = true)  // 运行时设置
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EvaluationJob toJobEntity(EvaluationJobCreateRequest request);

    /**
     * 使用更新请求 DTO 更新评估任务实体
     * <p>
     * 仅更新基本信息字段，忽略状态、计数器、时间戳等运行时字段。
     * </p>
     *
     * @param job     待更新的实体
     * @param request 更新请求 DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalItems", ignore = true)
    @Mapping(target = "completedItems", ignore = true)
    @Mapping(target = "successCount", ignore = true)
    @Mapping(target = "failedCount", ignore = true)
    @Mapping(target = "totalLatencyMs", ignore = true)
    @Mapping(target = "totalInputTokens", ignore = true)
    @Mapping(target = "totalOutputTokens", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "promptTemplateVersion", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateJobEntity(@MappingTarget EvaluationJob job, EvaluationJobUpdateRequest request);

    /**
     * 将评估结果响应 DTO 转换为实体
     * <p>
     * 用于导入评估结果，状态默认设置为 PENDING。
     * </p>
     *
     * @param response 评估结果响应 DTO
     * @return 评估结果实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "latencyMs", ignore = true)
    @Mapping(target = "inputTokens", ignore = true)
    @Mapping(target = "outputTokens", ignore = true)
    @Mapping(target = "actualOutput", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    EvaluationResult toResultEntity(EvaluationResultResponse response);

    /**
     * 将评估任务转换为指标响应 DTO
     * <p>
     * 提取评估任务的核心指标数据，包括完成数、成功率、延迟、Token 消耗等，
     * 并计算平均输入/输出 Token 数。
     * </p>
     *
     * @param job 评估任务实体
     * @return 指标响应 DTO
     */
    default EvaluationMetricsResponse toMetricsResponse(EvaluationJob job) {
        if (job == null) {
            return null;
        }
        EvaluationMetricsResponse metrics = new EvaluationMetricsResponse();
        metrics.setJobId(job.getId());
        metrics.setJobName(job.getName());
        metrics.setTotalItems(job.getTotalItems());
        metrics.setCompletedItems(job.getCompletedItems());
        metrics.setSuccessCount(job.getSuccessCount());
        metrics.setFailedCount(job.getFailedCount());
        metrics.setSuccessRate(job.getSuccessRate());
        metrics.setAverageLatencyMs(job.getAverageLatencyMs());
        metrics.setTotalLatencyMs(job.getTotalLatencyMs());
        metrics.setTotalInputTokens(job.getTotalInputTokens());
        metrics.setTotalOutputTokens(job.getTotalOutputTokens());

        if (job.getCompletedItems() > 0) {
            metrics.setAverageInputTokens(job.getTotalInputTokens() != null ?
                (double) job.getTotalInputTokens() / job.getCompletedItems() : null);
            metrics.setAverageOutputTokens(job.getTotalOutputTokens() != null ?
                (double) job.getTotalOutputTokens() / job.getCompletedItems() : null);
        }

        return metrics;
    }

    /**
     * 计算两个评估任务的对比指标
     * <p>
     * 比较两个评估任务的延迟、成功率、Token 消耗等指标，
     * 计算差异值和差异百分比，并判断哪个任务表现更好。
     * </p>
     *
     * @param job1 第一个评估任务
     * @param job2 第二个评估任务
     * @return 对比指标 DTO
     */
    default EvaluationCompareResponse.ComparisonMetrics toComparisonMetrics(EvaluationJob job1, EvaluationJob job2) {
        EvaluationCompareResponse.ComparisonMetrics metrics = new EvaluationCompareResponse.ComparisonMetrics();

        double avgLatency1 = job1.getAverageLatencyMs() != null ? job1.getAverageLatencyMs() : 0;
        double avgLatency2 = job2.getAverageLatencyMs() != null ? job2.getAverageLatencyMs() : 0;
        metrics.setLatencyDiffMs(avgLatency1 - avgLatency2);
        metrics.setLatencyDiffPercent(avgLatency2 > 0 ? ((avgLatency1 - avgLatency2) / avgLatency2) * 100 : 0);

        double successRate1 = job1.getSuccessRate() != null ? job1.getSuccessRate() : 0;
        double successRate2 = job2.getSuccessRate() != null ? job2.getSuccessRate() : 0;
        metrics.setSuccessRateDiff(successRate1 - successRate2);

        long inputTokens1 = job1.getTotalInputTokens() != null ? job1.getTotalInputTokens() : 0;
        long inputTokens2 = job2.getTotalInputTokens() != null ? job2.getTotalInputTokens() : 0;
        metrics.setInputTokensDiff(inputTokens1 - inputTokens2);

        long outputTokens1 = job1.getTotalOutputTokens() != null ? job1.getTotalOutputTokens() : 0;
        long outputTokens2 = job2.getTotalOutputTokens() != null ? job2.getTotalOutputTokens() : 0;
        metrics.setOutputTokensDiff(outputTokens1 - outputTokens2);

        metrics.setBetterLatencyJob(avgLatency1 <= avgLatency2 ? job1.getName() : job2.getName());
        metrics.setBetterSuccessRateJob(successRate1 >= successRate2 ? job1.getName() : job2.getName());

        return metrics;
    }
}
