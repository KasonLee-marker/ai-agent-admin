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
@Table(name = "datasets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dataset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private DatasetStatus status = DatasetStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private Integer itemCount = 0;

    @Column(length = 50)
    private String sourceType;

    @Column(length = 500)
    private String sourcePath;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String createdBy;

    public enum DatasetStatus {
        DRAFT,      // 草稿
        ACTIVE,     // 可用
        ARCHIVED,   // 已归档
        DELETED     // 已删除
    }
}
