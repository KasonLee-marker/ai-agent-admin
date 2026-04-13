package com.aiagent.admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_results", indexes = {
    @Index(name = "idx_result_job_id", columnList = "jobId"),
    @Index(name = "idx_result_dataset_item", columnList = "datasetItemId"),
    @Index(name = "idx_result_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String jobId;

    @Column(nullable = false, length = 100)
    private String datasetItemId;

    @Column(name = "input_data", nullable = false, length = 10000)
    private String input;

    @Column(name = "expected_output", length = 10000)
    private String expectedOutput;

    @Column(name = "actual_output", length = 10000)
    private String actualOutput;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    /**
     * AI 评估得分（0-100）
     */
    @Column(name = "score")
    private Float score;

    /**
     * 评分理由
     */
    @Column(name = "score_reason", length = 2000)
    private String scoreReason;

    /**
     * Embedding语义相似度（0-1）
     * <p>
     * 计算期望输出与实际输出的向量相似度。
     * </p>
     */
    @Column(name = "semantic_similarity")
    private Float semanticSimilarity;

    /**
     * 实际检索到的文档ID列表（RAG评估）
     * <p>
     * JSON数组格式：["chunkId1", "chunkId2"]
     * </p>
     */
    @Column(name = "retrieved_doc_ids", length = 500)
    private String retrievedDocIds;

    /**
     * 检索评估得分（Recall@K）
     * <p>
     * 表示期望文档被检索到的比例（0-1）。
     * </p>
     */
    @Column(name = "retrieval_score")
    private Float retrievalScore;

    /**
     * 事实忠实度（0-1）
     * <p>
     * 评估答案是否忠实于检索到的上下文内容。
     * </p>
     */
    @Column(name = "faithfulness")
    private Float faithfulness;

    /**
     * 渲染后的提示词内容（保存便于复现和调试）
     */
    @Column(name = "rendered_prompt", length = 5000)
    private String renderedPrompt;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ResultStatus status = ResultStatus.PENDING;

    @Column(length = 2000)
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ResultStatus {
        PENDING,    // 待执行
        SUCCESS,    // 成功
        FAILED      // 失败
    }
}
