package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DatasetImportRequest {

    @NotBlank(message = "Dataset name is required")
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @Size(max = 100, message = "Category must be less than 100 characters")
    private String category;

    @NotNull(message = "Items are required")
    private List<DatasetItemImportData> items;

    @Data
    public static class DatasetItemImportData {
        private String input;
        private String output;
        private String metadata;
    }
}
