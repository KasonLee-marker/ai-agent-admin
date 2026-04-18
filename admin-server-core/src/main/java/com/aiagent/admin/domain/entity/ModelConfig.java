package com.aiagent.admin.domain.entity;

import com.aiagent.admin.domain.enums.ModelProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 模型配置实体
 * <p>
 * 用于管理 AI 模型的配置信息，支持：
 * <ul>
 *   <li>Chat 模型 - 用于对话和文本生成</li>
 *   <li>Embedding 模型 - 用于文本向量计算</li>
 *   <li>Rerank 模型 - 用于检索结果重排序</li>
 * </ul>
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>配置模型供应商、API Key、Base URL</li>
 *   <li>设置模型参数（temperature、maxTokens 等）</li>
 *   <li>健康检查状态追踪</li>
 *   <li>设置默认模型</li>
 * </ul>
 * </p>
 *
 * @see ModelProvider
 */
@Entity
@Table(name = "model_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelConfig {

    /**
     * 配置唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 配置名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 模型供应商
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModelProvider provider;

    /**
     * 模型名称（如 gpt-4、claude-3-opus）
     */
    @Column(nullable = false, length = 100)
    private String modelName;

    /**
     * API Key
     */
    @Column(name = "api_key", length = 500)
    private String apiKey;

    /**
     * API Base URL
     */
    @Column(name = "base_url", length = 500)
    private String baseUrl;

    /**
     * Temperature 参数（控制输出随机性）
     */
    private Double temperature;

    /**
     * 最大输出 Token 数
     */
    @Column(name = "max_tokens")
    private Integer maxTokens;

    /**
     * Top P 参数（控制输出多样性）
     */
    @Column(name = "top_p")
    private Double topP;

    /**
     * 其他参数（JSON 格式）
     */
    @Column(name = "extra_params", length = 2000)
    private String extraParams;

    /**
     * 是否为默认 Chat 模型
     */
    @Column(name = "is_default")
    private Boolean isDefault = false;

    /**
     * 是否为默认 Embedding 模型
     */
    @Column(name = "is_default_embedding")
    private Boolean isDefaultEmbedding = false;

    /**
     * 是否启用
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * 健康检查状态
     */
    @Column(name = "health_status", length = 20)
    @Enumerated(EnumType.STRING)
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    /**
     * 最后健康检查时间
     */
    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    /**
     * Embedding 向量维度（仅 Embedding 类型模型有效）
     * <p>
     * 健康检查时从 API 响应获取并记录。
     * 不同维度对应不同的向量存储表。
     * </p>
     */
    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    /**
     * 关联的向量表名（仅 Embedding 类型模型有效）
     * <p>
     * 格式：document_embeddings_{dimension}，如 document_embeddings_1536
     * 健康检查成功后自动创建并关联。
     * </p>
     */
    @Column(name = "embedding_table_name", length = 50)
    private String embeddingTableName;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 持久化前初始化默认值
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (isDefault == null) {
            isDefault = false;
        }
        if (isDefaultEmbedding == null) {
            isDefaultEmbedding = false;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (healthStatus == null) {
            healthStatus = HealthStatus.UNKNOWN;
        }
    }

    /**
     * 健康检查状态枚举
     */
    public enum HealthStatus {
        HEALTHY, UNHEALTHY, UNKNOWN
    }
}
