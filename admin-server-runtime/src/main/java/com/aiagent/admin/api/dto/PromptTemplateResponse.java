package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提示词模板响应 DTO
 * <p>
 * 返回提示词模板的详细信息，包括内容、变量、版本等。
 * </p>
 */
@Data
public class PromptTemplateResponse {

    /**
     * 模板 ID
     */
    private String id;

    /** 模板名称 */
    private String name;

    /** 模板内容（支持变量占位符） */
    private String content;

    /** 模板描述 */
    private String description;

    /** 模板分类 */
    private String category;

    /** 模板标签列表 */
    private List<String> tags;

    /** 当前版本号 */
    private Integer version;

    /** 提取的变量列表 */
    private List<String> variables;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 创建者 */
    private String createdBy;
}
