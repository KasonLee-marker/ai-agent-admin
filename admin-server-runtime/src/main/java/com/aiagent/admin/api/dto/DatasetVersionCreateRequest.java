package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 数据集版本创建请求 DTO
 * <p>
 * 用于创建数据集的新版本，保存当前状态为历史版本。
 * </p>
 */
@Data
public class DatasetVersionCreateRequest {

    /**
     * 数据集 ID（必填）
     */
    @NotBlank(message = "Dataset ID is required")
    private String datasetId;

    /** 版本描述（可选） */
    private String description;
}
