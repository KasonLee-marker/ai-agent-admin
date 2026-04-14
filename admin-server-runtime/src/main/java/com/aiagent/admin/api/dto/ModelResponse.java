package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModelResponse {

    private String id;
    private String name;
    private ModelProvider provider;
    private String modelName;
    private String baseUrl;
    /**
     * 脱敏的 API Key（仅显示后4位，前面用星号替代）
     * <p>
     * 格式：****xxxx（如 ****1234）
     * </p>
     */
    private String apiKey;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Object extraParams;
    private Boolean isDefault;
    private Boolean isDefaultEmbedding;
    private Boolean isActive;
    private ModelConfig.HealthStatus healthStatus;
    private LocalDateTime lastHealthCheck;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 模型类型（从 Provider 的 modelType 字段获取）
     */
    private String modelType;

    /**
     * 向量维度（仅 EMBEDDING 类型有效）
     */
    private Integer embeddingDimension;

    /**
     * 向量表名（仅 EMBEDDING 类型有效）
     */
    private String embeddingTableName;
}
