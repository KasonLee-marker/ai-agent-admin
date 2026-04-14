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
    private Integer promptTemplateVersion;  // 使用的提示词模板版本号
    private String modelConfigId;
    private String modelConfigName;
    private String datasetId;
    private String datasetName;
    /**
     * 关联的知识库ID（用于RAG评估）
     */
    private String documentId;
    /**
     * 知识库名称
     */
    private String documentName;
    /**
     * 是否启用RAG评估模式
     */
    private Boolean enableRag;
    /**
     * Embedding 模型配置ID（用于计算语义相似度）
     */
    private String embeddingModelId;
    /**
     * Embedding 模型名称
     */
    private String embeddingModelName;
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
