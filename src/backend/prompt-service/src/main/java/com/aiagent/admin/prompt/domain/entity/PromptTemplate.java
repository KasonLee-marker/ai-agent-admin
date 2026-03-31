package com.aiagent.admin.prompt.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(length = 1000)
    private String variables;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String createdBy;
}
