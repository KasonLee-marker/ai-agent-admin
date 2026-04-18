package com.aiagent.admin.api.dto;

import lombok.Data;

/**
 * 评估任务对比响应 DTO
 * <p>
 * 返回两个评估任务的对比结果，包括性能指标差异分析。
 * </p>
 */
@Data
public class EvaluationCompareResponse {

    /**
     * 第一个评估任务详情
     */
    private EvaluationJobResponse job1;

    /** 第二个评估任务详情 */
    private EvaluationJobResponse job2;

    /** 对比指标 */
    private ComparisonMetrics metrics;

    /**
     * 对比指标 DTO
     * <p>
     * 包含两个任务之间的性能差异分析数据。
     * </p>
     */
    @Data
    public static class ComparisonMetrics {

        /** 响应延迟差异（毫秒） */
        private double latencyDiffMs;

        /** 响应延迟差异百分比 */
        private double latencyDiffPercent;

        /** 成功率差异 */
        private double successRateDiff;

        /** 输入 Token 数差异 */
        private long inputTokensDiff;

        /** 输出 Token 数差异 */
        private long outputTokensDiff;

        /** 响应延迟更优的任务标识 */
        private String betterLatencyJob;

        /** 成功率更优的任务标识 */
        private String betterSuccessRateJob;
    }
}
