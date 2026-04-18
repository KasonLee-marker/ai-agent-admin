package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型配置响应 DTO
 * <p>
 * 返回模型配置的详细信息，包括供应商、参数、健康状态等。
 * </p>
 */
@Data
public class ModelResponse {

    /**
     * 配置 ID
     */
    private String id;

    /** 配置名称 */
    private String name;

    /** 模型供应商 */
    private ModelProvider provider;

    /** 模型名称 */
    private String modelName;

    /** API 基础 URL */
    private String baseUrl;

    /**
     * 脱敏的 API Key（仅显示后4位，前面用星号替代）
     * <p>
     * 格式：****xxxx（如 ****1234）
     * </p>
     */
    private String apiKey;

    /** 温度参数 */
    private Double temperature;

    /** 最大 Token 数 */
    private Integer maxTokens;

    /** Top-P 参数 */
    private Double topP;

    /** 额外参数 */
    private Object extraParams;

    /** 是否为默认对话模型 */
    private Boolean isDefault;

    /** 是否为默认 Embedding 模型 */
    private Boolean isDefaultEmbedding;

    /** 是否激活 */
    private Boolean isActive;

    /** 健康检查状态 */
    private ModelConfig.HealthStatus healthStatus;

    /** 最后健康检查时间 */
    private LocalDateTime lastHealthCheck;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
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
