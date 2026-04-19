package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 数据集项更新请求 DTO
 * <p>
 * 用于更新数据集项的输入、输出。
 * </p>
 */
@Data
public class DatasetItemUpdateRequest {

    /**
     * 新的输入内容（可选，最大 10000 字符）
     */
    @Size(max = 10000, message = "Input must be less than 10000 characters")
    private String input;

    /** 新的期望输出（可选，最大 10000 字符） */
    @Size(max = 10000, message = "Output must be less than 10000 characters")
    private String output;

    /** 元数据（可选，最大 10000 字符） */
    @Size(max = 10000, message = "Metadata must be less than 10000 characters")
    private String metadata;
}
