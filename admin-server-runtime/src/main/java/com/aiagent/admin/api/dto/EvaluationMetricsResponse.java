package com.aiagent.admin.api.dto;

import lombok.Data;

@Data
public class EvaluationMetricsResponse {
    private String jobId;
    private String jobName;
    private Integer totalItems;
    private Integer completedItems;
    private Integer successCount;
    private Integer failedCount;
    private Double successRate;
    private Double averageLatencyMs;
    private Long totalLatencyMs;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private Double averageInputTokens;
    private Double averageOutputTokens;
}
