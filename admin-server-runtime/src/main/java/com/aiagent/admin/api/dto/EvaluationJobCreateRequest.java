package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EvaluationJobCreateRequest {

    @NotBlank(message = "Job name is required")
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotBlank(message = "Prompt template ID is required")
    private String promptTemplateId;

    @NotBlank(message = "Model config ID is required")
    private String modelConfigId;

    @NotBlank(message = "Dataset ID is required")
    private String datasetId;

    /**
     * 关联的知识库ID（用于RAG评估，可选）
     */
    private String documentId;

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

    @Size(max = 100, message = "CreatedBy must be less than 100 characters")
    private String createdBy;
}
