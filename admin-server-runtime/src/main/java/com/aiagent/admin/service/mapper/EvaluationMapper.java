package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.EvaluationJob;
import com.aiagent.admin.domain.entity.EvaluationResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EvaluationMapper {

    EvaluationJobResponse toJobResponse(EvaluationJob job);

    List<EvaluationJobResponse> toJobResponseList(List<EvaluationJob> jobs);

    EvaluationResultResponse toResultResponse(EvaluationResult result);

    List<EvaluationResultResponse> toResultResponseList(List<EvaluationResult> results);

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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "promptTemplateId", ignore = true)
    @Mapping(target = "modelConfigId", ignore = true)
    @Mapping(target = "datasetId", ignore = true)
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
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateJobEntity(@MappingTarget EvaluationJob job, EvaluationJobUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "latencyMs", ignore = true)
    @Mapping(target = "inputTokens", ignore = true)
    @Mapping(target = "outputTokens", ignore = true)
    @Mapping(target = "actualOutput", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    EvaluationResult toResultEntity(EvaluationResultResponse response);

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
