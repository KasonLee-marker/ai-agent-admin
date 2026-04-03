package com.aiagent.admin.api.dto;

import lombok.Data;

@Data
public class EvaluationCompareResponse {
    private EvaluationJobResponse job1;
    private EvaluationJobResponse job2;
    private ComparisonMetrics metrics;

    @Data
    public static class ComparisonMetrics {
        private double latencyDiffMs;
        private double latencyDiffPercent;
        private double successRateDiff;
        private long inputTokensDiff;
        private long outputTokensDiff;
        private String betterLatencyJob;
        private String betterSuccessRateJob;
    }
}
