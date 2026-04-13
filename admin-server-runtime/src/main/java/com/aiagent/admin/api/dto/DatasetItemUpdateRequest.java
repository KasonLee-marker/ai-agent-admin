package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DatasetItemUpdateRequest {

    @Size(max = 10000, message = "Input must be less than 10000 characters")
    private String input;

    @Size(max = 10000, message = "Output must be less than 10000 characters")
    private String output;

    /**
     * 期望检索到的文档ID列表（用于RAG评估）
     */
    @Size(max = 500, message = "Expected doc IDs must be less than 500 characters")
    private String expectedDocIds;

    /**
     * 参考上下文（用于RAG评估）
     */
    @Size(max = 5000, message = "Context must be less than 5000 characters")
    private String context;

    @Size(max = 10000, message = "Metadata must be less than 10000 characters")
    private String metadata;
}
