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

    @Column(precision = 3, scale = 2)
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "top_p", precision = 3, scale = 2)
    private Double topP;

    @Column(name = "extra_params", length = 2000)
    private String extraParams;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "health_status", length = 20)
    @Enumerated(EnumType.STRING)
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

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
