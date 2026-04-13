package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DatasetItemResponse {
    private String id;
    private String datasetId;
    private Integer version;
    private Integer sequence;
    private String input;
    private String output;
    /**
     * 期望检索到的文档ID列表（用于RAG评估）
     */
    private String expectedDocIds;
    /**
     * 参考上下文（用于RAG评估）
     */
    private String context;
    private String metadata;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
