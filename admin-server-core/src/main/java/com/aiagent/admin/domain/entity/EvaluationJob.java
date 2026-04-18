package com.aiagent.admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 评估任务实体
 * <p>
 * 用于管理评估任务的执行过程，支持：
 * <ul>
 *   <li>关联数据集、模型配置、提示词模板</li>
 *   <li>RAG 评估模式（关联知识库）</li>
 *   <li>进度追踪（完成数、成功数、失败数）</li>
 *   <li>性能统计（延迟、Token 消耗）</li>
 * </ul>
 * </p>
 *
 * @see Dataset
 * @see EvaluationResult
 * @see KnowledgeBase
 */
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

    /**
     * 任务唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 任务名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 任务描述
     */
    @Column(length = 1000)
    private String description;

    /**
     * Prompt 模板 ID（可选）
     * <p>
     * 如果不指定，评估时将使用默认的系统提示词。
     * </p>
     */
    @Column(length = 100)
    private String promptTemplateId;

    /**
     * 使用的提示词模板版本号（记录评估时的版本，便于复现）
     */
    @Column(name = "prompt_template_version")
    private Integer promptTemplateVersion;

    /**
     * 对话模型配置 ID（可选）
     * <p>
     * 如果不指定，将使用系统默认的对话模型。
     * </p>
     */
    @Column(length = 100)
    private String modelConfigId;

    /**
     * 关联数据集 ID
     */
    @Column(nullable = false, length = 100)
    private String datasetId;

    /**
     * 关联知识库 ID（用于 RAG 评估）
     * <p>
     * 设置后，评估时会从该知识库检索相关文档作为上下文。
     * </p>
     */
    @Column(name = "knowledge_base_id", length = 100)
    private String knowledgeBaseId;

    /**
     * 使用的 Embedding 模型配置 ID（用于计算语义相似度）
     * <p>
     * 用于计算期望输出与实际输出的语义相似度。
     * 如果不指定，将使用系统默认的 Embedding 模型。
     * </p>
     */
    @Column(name = "embedding_model_id", length = 100)
    private String embeddingModelId;

    /**
     * 是否启用 RAG 评估模式
     * <p>
     * 启用后，评估流程会：
     * <ol>
     *   <li>检索相关文档片段</li>
     *   <li>计算检索评估指标（Recall、Precision 等）</li>
     *   <li>构建包含上下文的提示词</li>
     *   <li>评估答案忠实度</li>
     * </ol>
     * </p>
     */
    @Column(name = "enable_rag")
    @Builder.Default
    private Boolean enableRag = false;

    /**
     * 任务状态
     */
    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    /**
     * 总数据项数
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer totalItems = 0;

    /**
     * 已完成数据项数
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer completedItems = 0;

    /**
     * 成功数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer successCount = 0;

    /**
     * 失败数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    /**
     * 总延迟（毫秒）
     */
    @Column(name = "total_latency_ms")
    private Long totalLatencyMs;

    /**
     * 总输入 Token 数
     */
    @Column(name = "total_input_tokens")
    private Long totalInputTokens;

    /**
     * 总输出 Token 数
     */
    @Column(name = "total_output_tokens")
    private Long totalOutputTokens;

    /**
     * 开始时间
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 错误信息
     */
    @Column(length = 2000)
    private String errorMessage;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    @Column(length = 100)
    private String createdBy;

    /**
     * 任务状态枚举
     */
    public enum JobStatus {
        PENDING,    // 待运行
        RUNNING,    // 运行中
        COMPLETED,  // 已完成
        FAILED,     // 失败
        CANCELLED   // 已取消
    }

    /**
     * 增加已完成数量
     */
    public void incrementCompleted() {
        this.completedItems++;
    }

    /**
     * 增加成功数量
     */
    public void incrementSuccess() {
        this.successCount++;
    }

    /**
     * 增加失败数量
     */
    public void incrementFailed() {
        this.failedCount++;
    }

    /**
     * 累加延迟时间
     *
     * @param latencyMs 单次延迟（毫秒）
     */
    public void addLatency(long latencyMs) {
        if (this.totalLatencyMs == null) {
            this.totalLatencyMs = 0L;
        }
        this.totalLatencyMs += latencyMs;
    }

    /**
     * 累加输入 Token 数
     *
     * @param tokens 单次输入 Token 数
     */
    public void addInputTokens(long tokens) {
        if (this.totalInputTokens == null) {
            this.totalInputTokens = 0L;
        }
        this.totalInputTokens += tokens;
    }

    /**
     * 累加输出 Token 数
     *
     * @param tokens 单次输出 Token 数
     */
    public void addOutputTokens(long tokens) {
        if (this.totalOutputTokens == null) {
            this.totalOutputTokens = 0L;
        }
        this.totalOutputTokens += tokens;
    }

    /**
     * 计算平均延迟
     *
     * @return 平均延迟（毫秒），若无数据返回 null
     */
    public Double getAverageLatencyMs() {
        if (totalLatencyMs == null || completedItems == 0) {
            return null;
        }
        return (double) totalLatencyMs / completedItems;
    }

    /**
     * 计算成功率
     *
     * @return 成功率（百分比），若无数据返回 null
     */
    public Double getSuccessRate() {
        if (completedItems == 0) {
            return null;
        }
        return (double) successCount / completedItems * 100;
    }
}
