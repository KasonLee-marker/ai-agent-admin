package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DatasetItemCreateRequest {

    /**
     * 数据集 ID（由 URL path 提供，不需要在请求体中传递）
     */
    private String datasetId;

    @NotBlank(message = "Input data is required")
    @Size(max = 10000, message = "Input must be less than 10000 characters")
    private String input;

    @Size(max = 10000, message = "Output must be less than 10000 characters")
    private String output;

    /**
     * 期望检索到的文档ID列表（用于RAG评估）
     * <p>
     * JSON数组格式：["docId1", "docId2"]
     * </p>
     */
    @Size(max = 500, message = "Expected doc IDs must be less than 500 characters")
    private String expectedDocIds;

    /**
     * 参考上下文（用于RAG评估的faithfulness指标）
     */
    @Size(max = 5000, message = "Context must be less than 5000 characters")
    private String context;

    @Size(max = 10000, message = "Metadata must be less than 10000 characters")
    private String metadata;
}
