package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据集响应 DTO
 * <p>
 * 返回数据集的详细信息，包括名称、描述、版本、数据项数量等。
 * </p>
 */
@Data
public class DatasetResponse {

    /**
     * 数据集 ID
     */
    private String id;

    /** 数据集名称 */
    private String name;

    /** 数据集描述 */
    private String description;

    /** 数据集分类 */
    private String category;

    /** 数据集标签（逗号分隔） */
    private String tags;

    /** 当前版本号 */
    private Integer version;

    /** 数据集状态 */
    private String status;

    /** 数据项数量 */
    private Integer itemCount;

    /** 数据来源类型 */
    private String sourceType;

    /** 数据来源路径 */
    private String sourcePath;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 创建者 */
    private String createdBy;
}
