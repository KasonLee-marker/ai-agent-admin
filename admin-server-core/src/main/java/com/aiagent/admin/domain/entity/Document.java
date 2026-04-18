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
 * 文档实体
 * <p>
 * 存储上传的文档信息，支持：
 * <ul>
 *   <li>多种分块策略（固定大小、段落、语义切分等）</li>
 *   <li>Embedding 向量索引</li>
 *   <li>进度追踪（分块、向量化）</li>
 *   <li>关联知识库</li>
 * </ul>
 * </p>
 *
 * @see DocumentChunk
 * @see KnowledgeBase
 */
@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 文档名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 文件类型（MIME 类型）
     */
    @Column(name = "content_type", length = 50)
    private String contentType;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 文件存储路径
     */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /**
     * 总分块数
     */
    @Column(name = "total_chunks")
    @Builder.Default
    private Integer totalChunks = 0;

    /**
     * 使用的 Embedding 模型配置 ID
     */
    @Column(name = "embedding_model_id", length = 64)
    private String embeddingModelId;

    /**
     * Embedding 模型名称（用于展示）
     */
    @Column(name = "embedding_model_name", length = 100)
    private String embeddingModelName;

    /**
     * 向量维度
     */
    @Column(name = "embedding_dimension")
    @Builder.Default
    private Integer embeddingDimension = 0;

    /**
     * 所属知识库 ID
     * <p>
     * 文档可以归属于某个知识库，用于组织管理和检索筛选。
     * </p>
     */
    @Column(name = "knowledge_base_id", length = 64)
    private String knowledgeBaseId;

    /**
     * 分块策略（FIXED_SIZE/PARAGRAPH/SENTENCE/RECURSIVE/SEMANTIC）
     */
    @Column(name = "chunk_strategy", length = 20)
    @Builder.Default
    private String chunkStrategy = "FIXED_SIZE";

    /**
     * 分块大小（字符数）
     */
    @Column(name = "chunk_size")
    @Builder.Default
    private Integer chunkSize = 500;

    /**
     * 分块重叠大小（字符数）
     */
    @Column(name = "chunk_overlap")
    @Builder.Default
    private Integer chunkOverlap = 50;

    /**
     * 语义切分进度 - 已处理句子数
     * <p>
     * 仅在 SEMANTIC 分块策略时使用，用于显示处理进度。
     * </p>
     */
    @Column(name = "semantic_progress_current")
    @Builder.Default
    private Integer semanticProgressCurrent = 0;

    /**
     * 语义切分进度 - 总句子数
     * <p>
     * 仅在 SEMANTIC 分块策略时使用，用于显示处理进度。
     * </p>
     */
    @Column(name = "semantic_progress_total")
    @Builder.Default
    private Integer semanticProgressTotal = 0;

    /**
     * 已创建的分块数
     */
    @Column(name = "chunks_created")
    @Builder.Default
    private Integer chunksCreated = 0;

    /**
     * 已 Embedding 的分块数
     */
    @Column(name = "chunks_embedded")
    @Builder.Default
    private Integer chunksEmbedded = 0;

    /**
     * 文档处理状态
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PROCESSING;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
     * 文档处理状态枚举
     */
    public enum DocumentStatus {
        PROCESSING,          // 正在提取文本
        SEMANTIC_PROCESSING, // 语义切分处理中（调用 Embedding API）
        CHUNKED,             // 已分块，等待 Embedding
        EMBEDDING,           // 正在计算向量
        COMPLETED,           // 完成（已 Embedding）
        FAILED,              // 失败
        DELETED              // 已删除
    }
}