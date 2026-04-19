package com.aiagent.admin.api.dto;

import lombok.Data;

/**
 * 评估指标统计响应 DTO
 * <p>
 * 返回评估任务的综合统计指标，包括成功率、延迟、Token 使用量等。
 * </p>
 */
@Data
public class EvaluationMetricsResponse {

    /**
     * 评估任务 ID
     */
    private String jobId;

    /** 任务名称 */
    private String jobName;

    /** 总数据项数 */
    private Integer totalItems;

    /** 已完成数据项数 */
    private Integer completedItems;

    /** 成功数量 */
    private Integer successCount;

    /** 失败数量 */
    private Integer failedCount;

    /** 成功率（百分比） */
    private Double successRate;

    /** 平均响应延迟（毫秒） */
    private Double averageLatencyMs;

    /** 总响应延迟（毫秒） */
    private Long totalLatencyMs;

    /** 总输入 Token 数 */
    private Long totalInputTokens;

    /** 总输出 Token 数 */
    private Long totalOutputTokens;

    /** 平均输入 Token 数 */
    private Double averageInputTokens;

    /** 平均输出 Token 数 */
    private Double averageOutputTokens;

    /**
     * 平均 AI 得分（0-100）
     */
    private Double averageScore;

    /**
     * 平均语义相似度（0-1）
     */
    private Double averageSemanticSimilarity;

    /**
     * 平均忠实度（0-1）
     */
    private Double averageFaithfulness;
}
