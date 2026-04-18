package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评估结果响应 DTO
 * <p>
 * 返回单条评估结果的详细信息，包括输入、输出、评分、RAG 评估指标等。
 * </p>
 */
@Data
public class EvaluationResultResponse {

    /**
     * 结果 ID
     */
    private String id;

    /** 所属评估任务 ID */
    private String jobId;

    /** 数据集项 ID */
    private String datasetItemId;

    /** 输入内容 */
    private String input;

    /** 期望输出 */
    private String expectedOutput;

    /** 实际输出 */
    private String actualOutput;

    /** 响应延迟（毫秒） */
    private Integer latencyMs;

    /** 输入 Token 数 */
    private Integer inputTokens;

    /** 输出 Token 数 */
    private Integer outputTokens;

    /** AI 评分（0-100） */
    private Float score;

    /** AI 评分理由 */
    private String scoreReason;

    /**
     * Embedding语义相似度（0-1）
     */
    private Float semanticSimilarity;

    /**
     * 实际检索到的文档ID列表
     */
    private String retrievedDocIds;

    /**
     * 检索评估得分
     */
    private Float retrievalScore;

    /**
     * 事实忠实度
     */
    private Float faithfulness;

    /**
     * 渲染后的提示词内容
     */
    private String renderedPrompt;

    /** 结果状态 */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
