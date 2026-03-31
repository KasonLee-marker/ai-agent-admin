package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SwitchModelRequest {

    @NotBlank(message = "Model ID is required")
    private String modelId;
}
