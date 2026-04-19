package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据集项响应 DTO
 * <p>
 * 返回单个数据集项的详细信息，包含输入、输出。
 * </p>
 */
@Data
public class DatasetItemResponse {

    /**
     * 数据项 ID
     */
    private String id;

    /** 所属数据集 ID */
    private String datasetId;

    /** 版本号 */
    private Integer version;

    /** 序号（数据集中的顺序） */
    private Integer sequence;

    /** 输入内容 */
    private String input;

    /** 期望输出 */
    private String output;

    /** 元数据 */
    private String metadata;

    /** 数据项状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
