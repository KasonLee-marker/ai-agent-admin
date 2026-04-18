package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 数据集项创建请求 DTO
 * <p>
 * 用于向数据集添加新的数据项，包含输入、期望输出、期望检索文档等 RAG 评估字段。
 * </p>
 */
@Data
public class DatasetItemCreateRequest {

    /**
     * 数据集 ID（由 URL path 提供，不需要在请求体中传递）
     */
    private String datasetId;

    /**
     * 输入内容（必填，最大 10000 字符）
     */
    @NotBlank(message = "Input data is required")
    @Size(max = 10000, message = "Input must be less than 10000 characters")
    private String input;

    /** 期望输出（可选，最大 10000 字符） */
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

    /** 元数据（JSON 格式，最大 10000 字符） */
    @Size(max = 10000, message = "Metadata must be less than 10000 characters")
    private String metadata;
}
