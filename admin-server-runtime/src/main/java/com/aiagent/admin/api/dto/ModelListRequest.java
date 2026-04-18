package com.aiagent.admin.api.dto;

import lombok.Data;

/**
 * 模型列表查询请求 DTO
 * <p>
 * 用于筛选查询模型配置列表，支持按供应商、状态、关键词筛选。
 * </p>
 */
@Data
public class ModelListRequest {

    /**
     * 供应商筛选（可选，如 OPENAI、DASHSCOPE）
     */
    private String provider;

    /** 是否激活筛选（可选） */
    private Boolean isActive;

    /** 关键词搜索（可选，匹配名称或模型名） */
    private String keyword;
}
