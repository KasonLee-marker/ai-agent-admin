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
 * 数据集实体
 * <p>
 * 数据集用于存储评估测试数据，包含多条数据项（DatasetItem）。
 * 主要功能：
 * <ul>
 *   <li>组织评估测试输入和期望输出</li>
 *   <li>支持版本管理</li>
 *   <li>支持分类和标签</li>
 *   <li>记录来源信息（导入/手动创建）</li>
 * </ul>
 * </p>
 *
 * @see DatasetItem
 * @see EvaluationJob
 */
@Entity
@Table(name = "datasets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dataset {

    /**
     * 数据集唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 数据集名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 数据集描述
     */
    @Column(length = 1000)
    private String description;

    /**
     * 数据集分类
     */
    @Column(length = 100)
    private String category;

    /**
     * 标签（逗号分隔）
     */
    @Column(length = 500)
    private String tags;

    /**
     * 数据集版本号
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * 数据集状态
     */
    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private DatasetStatus status = DatasetStatus.DRAFT;

    /**
     * 数据项数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer itemCount = 0;

    /**
     * 来源类型（IMPORT/MANUAL）
     */
    @Column(length = 50)
    private String sourceType;

    /**
     * 来源路径（导入文件的路径）
     */
    @Column(length = 500)
    private String sourcePath;

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
     * 数据集状态枚举
     */
    public enum DatasetStatus {
        DRAFT,      // 草稿
        ACTIVE,     // 可用
        ARCHIVED,   // 已归档
        DELETED     // 已删除
    }
}
