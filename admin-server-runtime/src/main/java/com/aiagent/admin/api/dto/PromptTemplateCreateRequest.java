package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提示词模板创建请求 DTO
 * <p>
 * 用于创建新的提示词模板，包含名称、内容、描述、分类、标签等。
 * </p>
 */
@Data
public class PromptTemplateCreateRequest {

    /**
     * 模板名称（必填，最大 200 字符）
     */
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /** 模板内容（必填，最大 5000 字符，支持变量占位符如 {{variable}}） */
    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    /** 模板描述（可选，最大 1000 字符） */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /** 模板分类（可选，最大 100 字符） */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /** 模板标签（可选） */
    private List<String> tags;

    /** 创建者（可选，最大 100 字符） */
    @Size(max = 100, message = "CreatedBy must not exceed 100 characters")
    private String createdBy;
}
