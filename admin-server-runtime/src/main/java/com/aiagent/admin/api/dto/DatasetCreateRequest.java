package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DatasetCreateRequest {

    @NotBlank(message = "Dataset name is required")
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @Size(max = 100, message = "Category must be less than 100 characters")
    private String category;

    @Size(max = 500, message = "Tags must be less than 500 characters")
    private String tags;

    @Size(max = 50, message = "Source type must be less than 50 characters")
    private String sourceType;
}
