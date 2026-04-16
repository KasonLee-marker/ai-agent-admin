package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EvaluationJobUpdateRequest {

    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

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
