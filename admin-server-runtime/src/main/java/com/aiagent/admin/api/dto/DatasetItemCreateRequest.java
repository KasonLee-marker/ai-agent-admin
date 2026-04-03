package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DatasetItemCreateRequest {

    @NotBlank(message = "Dataset ID is required")
    private String datasetId;

    @NotBlank(message = "Input data is required")
    @Size(max = 10000, message = "Input must be less than 10000 characters")
    private String input;

    @Size(max = 10000, message = "Output must be less than 10000 characters")
    private String output;

    @Size(max = 10000, message = "Metadata must be less than 10000 characters")
    private String metadata;
}
