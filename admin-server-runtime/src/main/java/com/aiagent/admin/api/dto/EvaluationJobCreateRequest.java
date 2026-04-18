package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评估任务创建请求 DTO
 * <p>
 * 用于创建新的评估任务，指定数据集、模型配置、RAG 配置等。
 * </p>
 */
@Data
public class EvaluationJobCreateRequest {

    /**
     * 任务名称（可选，最大 200 字符）
     */
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    /** 任务描述（可选，最大 1000 字符） */
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    /**
     * Prompt模板ID（可选）
     * <p>
     * 如果不指定，评估时将使用默认的系统提示词。
     * </p>
     */
    private String promptTemplateId;

    /**
     * 对话模型配置ID（可选）
     * <p>
     * 如果不指定，将使用系统默认的对话模型。
     * </p>
     */
    private String modelConfigId;

    /** 关联数据集 ID（必填） */
    @NotBlank(message = "Dataset ID is required")
    private String datasetId;

    /**
     * 关联的知识库ID（用于RAG评估，可选）
     */
    private String knowledgeBaseId;

    /**
     * 是否启用RAG评估模式（默认false）
     */
    private Boolean enableRag;

    /**
     * Embedding 模型配置ID（用于计算语义相似度，可选）
     * <p>
     * 如果不指定，将使用系统默认的 Embedding 模型。
     * </p>
     */
    private String embeddingModelId;

    /** 创建者（可选，最大 100 字符） */
    @Size(max = 100, message = "CreatedBy must be less than 100 characters")
    private String createdBy;
}
