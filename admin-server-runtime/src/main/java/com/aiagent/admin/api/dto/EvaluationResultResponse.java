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
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
