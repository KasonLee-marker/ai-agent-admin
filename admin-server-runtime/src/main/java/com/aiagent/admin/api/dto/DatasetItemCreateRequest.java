package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 数据集项创建请求 DTO
 * <p>
 * 用于向数据集添加新的数据项，包含输入、期望输出。
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

    /** 元数据（JSON 格式，最大 10000 字符） */
    @Size(max = 10000, message = "Metadata must be less than 10000 characters")
    private String metadata;
}
