package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.ModelProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateModelRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Provider is required")
    private ModelProvider provider;

    @NotBlank(message = "Model name is required")
    private String modelName;

    private String apiKey;

    private String baseUrl;

    private Double temperature = 0.7;

    private Integer maxTokens = 2048;

    private Double topP = 1.0;

    private Map<String, Object> extraParams;

    private Boolean isActive = true;
}
