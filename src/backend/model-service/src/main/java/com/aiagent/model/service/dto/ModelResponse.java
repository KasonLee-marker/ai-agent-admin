package com.aiagent.model.service.dto;

import com.aiagent.model.domain.entity.ModelConfig;
import com.aiagent.model.domain.entity.ModelProvider;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModelResponse {

    private String id;
    private String name;
    private ModelProvider provider;
    private String modelName;
    private String baseUrl;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Object extraParams;
    private Boolean isDefault;
    private Boolean isActive;
    private ModelConfig.HealthStatus healthStatus;
    private LocalDateTime lastHealthCheck;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
