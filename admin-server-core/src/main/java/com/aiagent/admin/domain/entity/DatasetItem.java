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
 * 数据集项实体
 * <p>
 * 存储数据集中的单条测试数据，用于评估：
 * <ul>
 *   <li>输入数据 - 测试问题或提示词</li>
 *   <li>输出数据 - 期望的回答</li>
 * </ul>
 * </p>
 *
 * @see Dataset
 * @see EvaluationResult
 */
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

    /**
     * 数据项唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 所属数据集 ID
     */
    @Column(nullable = false, length = 100)
    private String datasetId;

    /**
     * 数据项版本号
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * 序号（排序用）
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    /**
     * 输入数据（测试问题）
     */
    @Column(name = "input_data", nullable = false, length = 10000)
    private String input;

    /**
     * 输出数据（期望回答）
     */
    @Column(name = "output_data", length = 10000)
    private String output;


    /**
     * 元数据（JSON 格式）
     */
    @Column(length = 10000)
    private String metadata;

    /**
     * 数据项状态
     */
    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.ACTIVE;

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
     * 数据项状态枚举
     */
    public enum ItemStatus {
        ACTIVE,     // 可用
        DISABLED,   // 禁用
        DELETED     // 已删除
    }
}
