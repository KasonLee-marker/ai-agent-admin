package com.aiagent.admin.api.dto;

import lombok.Data;

/**
 * 提示词模板列表查询请求 DTO
 * <p>
 * 用于筛选查询提示词模板列表，支持按分类、标签、关键词筛选和分页。
 * </p>
 */
@Data
public class PromptListRequest {

    /**
     * 分类筛选（可选）
     */
    private String category;

    /** 标签筛选（可选） */
    private String tag;

    /** 关键词搜索（可选） */
    private String keyword;

    /** 当前页码（默认 0） */
    private Integer page = 0;

    /** 每页大小（默认 20） */
    private Integer size = 20;
}
