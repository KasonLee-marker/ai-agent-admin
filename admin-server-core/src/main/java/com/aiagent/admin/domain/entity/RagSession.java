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
 * RAG 对话会话实体
 * <p>
 * 用于存储 RAG 对话的会话信息，支持多轮对话：
 * <ul>
 *   <li>关联知识库（可选）</li>
 *   <li>关联对话模型和 Embedding 模型</li>
 *   <li>统计消息数量</li>
 * </ul>
 * </p>
 *
 * @see RagMessage
 * @see KnowledgeBase
 */
@Entity
@Table(name = "rag_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSession {

    /**
     * 会话唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 会话标题（可选，默认使用首个问题摘要）
     */
    @Column(length = 200)
    private String title;

    /**
     * 关联知识库 ID（可选）
     * <p>
     * 如果设置了知识库，检索时默认只在该知识库范围内搜索。
     * </p>
     */
    @Column(name = "knowledge_base_id", length = 64)
    private String knowledgeBaseId;

    /**
     * 对话模型 ID（可选）
     */
    @Column(name = "model_id", length = 64)
    private String modelId;

    /**
     * Embedding 模型 ID（可选）
     */
    @Column(name = "embedding_model_id", length = 64)
    private String embeddingModelId;

    /**
     * 消息数量
     */
    @Column(name = "message_count")
    @Builder.Default
    private Integer messageCount = 0;

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

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (messageCount == null) {
            messageCount = 0;
        }
    }
}