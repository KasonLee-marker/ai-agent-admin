package com.aiagent.admin.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档分块实体
 * <p>
 * 存储文档的分块内容和向量信息：
 * <ul>
 *   <li>分块内容 - 文本片段</li>
 *   <li>向量 - Embedding 计算结果（JSON 格式）</li>
 *   <li>元数据 - 分块位置、来源等信息</li>
 * </ul>
 * </p>
 *
 * @see Document
 */
@Entity
@Table(name = "document_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * 分块唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 所属文档 ID
     */
    @Column(name = "document_id", nullable = false, length = 64)
    private String documentId;

    /**
     * 分块序号（从 0 开始）
     */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /**
     * 分块内容
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Embedding 向量（JSON 数组格式）
     */
    @Column(columnDefinition = "TEXT")
    private String embedding;

    /**
     * 元数据（JSON 格式，包含分块位置、来源等）
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}