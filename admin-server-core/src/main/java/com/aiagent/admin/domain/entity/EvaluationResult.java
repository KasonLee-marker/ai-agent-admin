package com.aiagent.admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 评估结果实体
 * <p>
 * 存储单条数据的评估结果，包括：
 * <ul>
 *   <li>输入输出数据</li>
 *   <li>性能指标（延迟、Token 消耗）</li>
 *   <li>质量指标（AI 评分、语义相似度）</li>
 *   <li>RAG 评估指标（检索得分、忠实度）</li>
 * </ul>
 * </p>
 *
 * @see EvaluationJob
 * @see DatasetItem
 */
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

    /**
     * 结果唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 所属评估任务 ID
     */
    @Column(nullable = false, length = 100)
    private String jobId;

    /**
     * 关联数据项 ID
     */
    @Column(nullable = false, length = 100)
    private String datasetItemId;

    /**
     * 输入数据（测试问题）
     */
    @Column(name = "input_data", nullable = false, length = 10000)
    private String input;

    /**
     * 期望输出
     */
    @Column(name = "expected_output", length = 10000)
    private String expectedOutput;

    /**
     * 实际输出（AI 生成的回答）
     */
    @Column(name = "actual_output", length = 10000)
    private String actualOutput;

    /**
     * 响应延迟（毫秒）
     */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /**
     * 输入 Token 数
     */
    @Column(name = "input_tokens")
    private Integer inputTokens;

    /**
     * 输出 Token 数
     */
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
     * Embedding 语义相似度（0-1）
     * <p>
     * 计算期望输出与实际输出的向量相似度。
     * </p>
     */
    @Column(name = "semantic_similarity")
    private Float semanticSimilarity;

    /**
     * 实际检索到的文档 ID 列表（RAG 评估）
     * <p>
     * JSON 数组格式：["chunkId1", "chunkId2"]
     * </p>
     */
    @Column(name = "retrieved_doc_ids", length = 500)
    private String retrievedDocIds;


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

    /**
     * 结果状态
     */
    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ResultStatus status = ResultStatus.PENDING;

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
     * 结果状态枚举
     */
    public enum ResultStatus {
        PENDING,    // 待执行
        SUCCESS,    // 成功
        FAILED      // 失败
    }
}
