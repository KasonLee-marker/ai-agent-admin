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
@Table(name = "evaluation_jobs", indexes = {
    @Index(name = "idx_job_status", columnList = "status"),
    @Index(name = "idx_job_dataset", columnList = "datasetId"),
    @Index(name = "idx_job_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 100)
    private String promptTemplateId;

    /**
     * 使用的提示词模板版本号（记录评估时的版本，便于复现）
     */
    @Column(name = "prompt_template_version")
    private Integer promptTemplateVersion;

    @Column(nullable = false, length = 100)
    private String modelConfigId;

    @Column(nullable = false, length = 100)
    private String datasetId;

    /**
     * 关联的知识库ID（用于RAG评估）
     * <p>
     * 设置后，评估时会从该知识库检索相关文档作为上下文。
     * </p>
     */
    @Column(name = "document_id", length = 100)
    private String documentId;

    /**
     * 使用的 Embedding 模型配置ID（用于计算语义相似度）
     * <p>
     * 用于计算期望输出与实际输出的语义相似度。
     * 如果不指定，将使用系统默认的 Embedding 模型。
     * </p>
     */
    @Column(name = "embedding_model_id", length = 100)
    private String embeddingModelId;

    /**
     * 是否启用RAG评估模式
     * <p>
     * 启用后，评估流程会：
     * <ol>
     *   <li>检索相关文档片段</li>
     *   <li>计算检索评估指标（Recall、Precision等）</li>
     *   <li>构建包含上下文的提示词</li>
     *   <li>评估答案忠实度</li>
     * </ol>
     * </p>
     */
    @Column(name = "enable_rag")
    @Builder.Default
    private Boolean enableRag = false;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalItems = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer completedItems = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer successCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @Column(name = "total_latency_ms")
    private Long totalLatencyMs;

    @Column(name = "total_input_tokens")
    private Long totalInputTokens;

    @Column(name = "total_output_tokens")
    private Long totalOutputTokens;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(length = 2000)
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String createdBy;

    public enum JobStatus {
        PENDING,    // 待运行
        RUNNING,    // 运行中
        COMPLETED,  // 已完成
        FAILED,     // 失败
        CANCELLED   // 已取消
    }

    public void incrementCompleted() {
        this.completedItems++;
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    public void incrementFailed() {
        this.failedCount++;
    }

    public void addLatency(long latencyMs) {
        if (this.totalLatencyMs == null) {
            this.totalLatencyMs = 0L;
        }
        this.totalLatencyMs += latencyMs;
    }

    public void addInputTokens(long tokens) {
        if (this.totalInputTokens == null) {
            this.totalInputTokens = 0L;
        }
        this.totalInputTokens += tokens;
    }

    public void addOutputTokens(long tokens) {
        if (this.totalOutputTokens == null) {
            this.totalOutputTokens = 0L;
        }
        this.totalOutputTokens += tokens;
    }

    public Double getAverageLatencyMs() {
        if (totalLatencyMs == null || completedItems == 0) {
            return null;
        }
        return (double) totalLatencyMs / completedItems;
    }

    public Double getSuccessRate() {
        if (completedItems == 0) {
            return null;
        }
        return (double) successCount / completedItems * 100;
    }
}
