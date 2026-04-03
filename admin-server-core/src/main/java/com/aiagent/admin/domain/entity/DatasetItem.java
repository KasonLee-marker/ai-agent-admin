package com.aiagent.admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_items", indexes = {
    @Index(name = "idx_dataset_id", columnList = "datasetId"),
    @Index(name = "idx_dataset_version", columnList = "datasetId, version")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String datasetId;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "input_data", nullable = false, length = 10000)
    private String input;

    @Column(name = "output_data", length = 10000)
    private String output;

    @Column(length = 10000)
    private String metadata;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum ItemStatus {
        ACTIVE,     // 可用
        DISABLED,   // 禁用
        DELETED     // 已删除
    }
}
