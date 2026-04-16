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
 * 知识库实体
 * <p>
 * 知识库用于组织和分组管理文档，每个知识库可以：
 * <ul>
 *   <li>设置默认的 Embedding 模型</li>
 *   <li>统计文档数量和分块数量</li>
 *   <li>作为 RAG 检索的筛选条件</li>
 * </ul>
 * </p>
 *
 * @see Document
 * @see ModelConfig
 */
@Entity
@Table(name = "knowledge_bases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase {

    /**
     * 知识库唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 知识库名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 知识库描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 默认 Embedding 模型 ID
     * <p>
     * 上传文档到该知识库时，如果没有指定 Embedding 模型，
     * 将使用此默认模型进行向量索引。
     * </p>
     */
    @Column(name = "default_embedding_model_id", length = 64)
    private String defaultEmbeddingModelId;

    /**
     * 文档数量
     */
    @Column(name = "document_count")
    @Builder.Default
    private Integer documentCount = 0;

    /**
     * 分块总数
     */
    @Column(name = "chunk_count")
    @Builder.Default
    private Integer chunkCount = 0;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * 重索引状态
     * <p>
     * 当切换 Embedding 模型时，需要重新计算所有分块的向量。
     * 状态流转：NONE → IN_PROGRESS → COMPLETED/FAILED
     * </p>
     */
    @Column(name = "reindex_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ReindexStatus reindexStatus = ReindexStatus.NONE;

    /**
     * 重索引进度：已处理分块数
     */
    @Column(name = "reindex_progress_current")
    private Integer reindexProgressCurrent;

    /**
     * 重索引进度：总分块数
     */
    @Column(name = "reindex_progress_total")
    private Integer reindexProgressTotal;

    /**
     * 重索引开始时间
     */
    @Column(name = "reindex_started_at")
    private LocalDateTime reindexStartedAt;

    /**
     * 重索引完成时间
     */
    @Column(name = "reindex_completed_at")
    private LocalDateTime reindexCompletedAt;

    /**
     * 重索引错误信息
     */
    @Column(name = "reindex_error_message", length = 500)
    private String reindexErrorMessage;

    /**
     * 重索引状态枚举
     */
    public enum ReindexStatus {
        NONE,        // 无重索引任务
        IN_PROGRESS, // 正在重索引
        COMPLETED,   // 已完成
        FAILED       // 失败
    }
}