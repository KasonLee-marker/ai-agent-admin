package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DatasetVersionCreateRequest {

    @NotBlank(message = "Dataset ID is required")
    private String datasetId;

    private String description;
}
