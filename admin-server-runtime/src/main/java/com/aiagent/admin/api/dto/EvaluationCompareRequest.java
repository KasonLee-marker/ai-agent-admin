package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EvaluationCompareRequest {

    @NotBlank(message = "First job ID is required")
    private String jobId1;

    @NotBlank(message = "Second job ID is required")
    private String jobId2;
}
