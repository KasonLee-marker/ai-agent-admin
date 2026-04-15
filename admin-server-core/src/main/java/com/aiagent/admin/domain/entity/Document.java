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
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "total_chunks")
    @Builder.Default
    private Integer totalChunks = 0;

    @Column(name = "embedding_model_id", length = 64)
    private String embeddingModelId;  // 使用的 embedding 模型配置 ID

    @Column(name = "embedding_model_name", length = 100)
    private String embeddingModelName;  // 模型名称（用于展示）

    @Column(name = "embedding_dimension")
    @Builder.Default
    private Integer embeddingDimension = 0;  // 向量维度

    @Column(name = "chunk_strategy", length = 20)
    @Builder.Default
    private String chunkStrategy = "FIXED_SIZE";  // FIXED_SIZE 或 PARAGRAPH

    @Column(name = "chunk_size")
    @Builder.Default
    private Integer chunkSize = 500;

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

    @Column(name = "chunks_created")
    @Builder.Default
    private Integer chunksCreated = 0;  // 已创建的分块数

    @Column(name = "chunks_embedded")
    @Builder.Default
    private Integer chunksEmbedded = 0;  // 已embedding的分块数

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PROCESSING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public enum DocumentStatus {
        PROCESSING,          // 正在提取文本
        SEMANTIC_PROCESSING, // 语义切分处理中（调用 Embedding API）
        CHUNKED,             // 已分块，等待embedding
        EMBEDDING,           // 正在计算向量
        COMPLETED,           // 完成（已embedding）
        FAILED,              // 失败
        DELETED              // 已删除
    }
}