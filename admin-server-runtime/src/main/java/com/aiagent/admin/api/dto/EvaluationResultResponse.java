package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EvaluationResultResponse {
    private String id;
    private String jobId;
    private String datasetItemId;
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private Integer latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private Float score;
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
    private String renderedPrompt;  // 渲染后的提示词内容
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
