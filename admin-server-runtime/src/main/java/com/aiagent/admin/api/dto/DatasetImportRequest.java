package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 数据集导入请求 DTO
 * <p>
 * 用于从 JSON/CSV 文件导入数据集，批量创建数据项。
 * </p>
 */
@Data
public class DatasetImportRequest {

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

    /** 导入的数据项列表（必填） */
    @NotNull(message = "Items are required")
    private List<DatasetItemImportData> items;

    /**
     * 数据项导入数据 DTO
     * <p>
     * 表示单个导入的数据项，包含输入、输出和元数据。
     * </p>
     */
    @Data
    public static class DatasetItemImportData {

        /** 输入内容 */
        private String input;

        /** 期望输出 */
        private String output;

        /** 元数据（JSON 格式） */
        private String metadata;
    }
}
