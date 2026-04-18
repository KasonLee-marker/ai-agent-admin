package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建数据集请求 DTO
 * <p>
 * 用于创建新的评估数据集，包含名称、描述、分类、标签等基本信息。
 * </p>
 */
@Data
public class DatasetCreateRequest {

    /**
     * 数据集名称（必填，最大 200 字符）
     */
    @NotBlank(message = "Dataset name is required")
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    /** 数据集描述（可选，最大 1000 字符） */
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    /** 数据集分类（可选，最大 100 字符） */
    @Size(max = 100, message = "Category must be less than 100 characters")
    private String category;

    /** 数据集标签（可选，逗号分隔，最大 500 字符） */
    @Size(max = 500, message = "Tags must be less than 500 characters")
    private String tags;

    /** 数据来源类型（可选，最大 50 字符） */
    @Size(max = 50, message = "Source type must be less than 50 characters")
    private String sourceType;
}
