package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 数据集更新请求 DTO
 * <p>
 * 用于更新数据集的基本信息，如名称、描述、分类等。
 * </p>
 */
@Data
public class DatasetUpdateRequest {

    /**
     * 新名称（可选，最大 200 字符）
     */
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    /** 新描述（可选，最大 1000 字符） */
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    /** 新分类（可选，最大 100 字符） */
    @Size(max = 100, message = "Category must be less than 100 characters")
    private String category;

    /** 新标签（可选，最大 500 字符） */
    @Size(max = 500, message = "Tags must be less than 500 characters")
    private String tags;

    /** 新来源类型（可选，最大 50 字符） */
    @Size(max = 50, message = "Source type must be less than 50 characters")
    private String sourceType;
}
