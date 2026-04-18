package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评估任务更新请求 DTO
 * <p>
 * 用于更新评估任务的配置，如名称、描述、模型、数据集等。
 * </p>
 */
@Data
public class EvaluationJobUpdateRequest {

    /**
     * 新名称（可选，最大 200 字符）
     */
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    /** 新描述（可选，最大 1000 字符） */
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    /**
     * Prompt模板ID（可选）
     */
    private String promptTemplateId;

    /**
     * 对话模型配置ID
     */
    private String modelConfigId;

    /**
     * 数据集ID
     */
    private String datasetId;

    /**
     * Embedding 模型配置ID（用于计算语义相似度，可选）
     */
    private String embeddingModelId;

    /**
     * 关联的知识库ID（用于RAG评估，可选）
     */
    private String knowledgeBaseId;

    /**
     * 是否启用RAG评估模式
     */
    private Boolean enableRag;
}
