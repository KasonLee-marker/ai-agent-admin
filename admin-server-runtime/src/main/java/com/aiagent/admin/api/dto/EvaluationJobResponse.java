package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EvaluationJobResponse {
    private String id;
    private String name;
    private String description;
    private String promptTemplateId;
    private String promptTemplateName;
    private String modelConfigId;
    private String modelConfigName;
    private String datasetId;
    private String datasetName;
    private String status;
    private Integer totalItems;
    private Integer completedItems;
    private Integer successCount;
    private Integer failedCount;
    private Double successRate;
    private Double averageLatencyMs;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
