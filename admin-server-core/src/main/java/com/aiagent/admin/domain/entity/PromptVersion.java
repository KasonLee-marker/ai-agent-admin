package com.aiagent.admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 36)
    private String promptId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(length = 1000)
    private String changeLog;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
