package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评估任务响应 DTO
 * <p>
 * 返回评估任务的详细信息，包括配置、进度、统计指标等。
 * </p>
 */
@Data
public class EvaluationJobResponse {

    /**
     * 任务 ID
     */
    private String id;

    /** 任务名称 */
    private String name;

    /** 任务描述 */
    private String description;

    /** 使用的提示词模板 ID */
    private String promptTemplateId;

    /** 提示词模板名称 */
    private String promptTemplateName;

    /**
     * 使用的提示词模板版本号
     */
    private Integer promptTemplateVersion;

    /** 使用的模型配置 ID */
    private String modelConfigId;

    /** 模型配置名称 */
    private String modelConfigName;

    /** 关联数据集 ID */
    private String datasetId;

    /** 数据集名称 */
    private String datasetName;

    /**
     * 关联的知识库ID（用于RAG评估）
     */
    private String knowledgeBaseId;

    /**
     * 知识库名称
     */
    private String knowledgeBaseName;

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

    /** 任务状态 */
    private String status;

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

    /** 总输入 Token 数 */
    private Long totalInputTokens;

    /** 总输出 Token 数 */
    private Long totalOutputTokens;

    /** 错误信息 */
    private String errorMessage;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 创建者 */
    private String createdBy;
}
