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
