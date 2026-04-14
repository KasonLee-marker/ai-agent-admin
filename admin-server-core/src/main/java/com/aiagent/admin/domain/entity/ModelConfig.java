package com.aiagent.admin.domain.entity;

import com.aiagent.admin.domain.enums.ModelProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "model_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelConfig {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModelProvider provider;

    @Column(nullable = false, length = 100)
    private String modelName;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "top_p")
    private Double topP;

    @Column(name = "extra_params", length = 2000)
    private String extraParams;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_default_embedding")
    private Boolean isDefaultEmbedding = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "health_status", length = 20)
    @Enumerated(EnumType.STRING)
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    /**
     * Embedding 向量维度（仅 EMBEDDING 类型模型有效）
     * <p>
     * 健康检查时从 API 响应获取并记录。
     * 不同维度对应不同的向量存储表。
     * </p>
     */
    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    /**
     * 关联的向量表名（仅 EMBEDDING 类型模型有效）
     * <p>
     * 格式：document_embeddings_{dimension}，如 document_embeddings_1536
     * 健康检查成功后自动创建并关联。
     * </p>
     */
    @Column(name = "embedding_table_name", length = 50)
    private String embeddingTableName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    public enum HealthStatus {
        HEALTHY, UNHEALTHY, UNKNOWN
    }
}
