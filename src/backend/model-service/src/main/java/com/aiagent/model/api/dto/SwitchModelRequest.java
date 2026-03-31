package com.aiagent.model.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SwitchModelRequest {

    @NotBlank(message = "Model ID is required")
    private String modelId;
}
