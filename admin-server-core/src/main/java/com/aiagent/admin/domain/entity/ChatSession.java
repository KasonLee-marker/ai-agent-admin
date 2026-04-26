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
 * 聊天会话实体
 * <p>
 * 用于管理对话会话，支持：
 * <ul>
 *   <li>关联模型配置和提示词模板</li>
 *   <li>设置自定义系统消息</li>
 *   <li>统计消息数量</li>
 * </ul>
 * </p>
 *
 * @see ChatMessage
 * @see ModelConfig
 * @see PromptTemplate
 */
@Entity
@Table(name = "chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    /**
     * 会话唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 会话标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 关联模型配置 ID
     */
    @Column(length = 64)
    private String modelId;

    /**
     * 关联提示词模板 ID
     */
    @Column(length = 64)
    private String promptId;

    /**
     * 自定义系统消息
     * <p>
     * 如果设置了提示词模板，模板内容将作为系统消息；
     * 否则使用此字段作为系统消息。
     * </p>
     */
    @Column(length = 500)
    private String systemMessage;

    /**
     * 消息数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer messageCount = 0;

    /**
     * 是否活跃会话
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
     * 是否启用 RAG 检索增强
     * <p>
     * 启用后，对话时会先从知识库检索相关文档，再生成回答。
     * </p>
     */
    @Column(name = "enable_rag")
    private Boolean enableRag;

    /**
     * 关联知识库 ID（用于 RAG 检索）
     * <p>
     * 如果设置了知识库，检索时默认只在该知识库范围内搜索。
     * </p>
     */
    @Column(name = "knowledge_base_id", length = 64)
    private String knowledgeBaseId;

    /**
     * RAG 检索数量（topK）
     */
    @Column(name = "rag_top_k")
    private Integer ragTopK;

    /**
     * RAG 相似度阈值
     */
    @Column(name = "rag_threshold")
    private Double ragThreshold;

    /**
     * RAG 检索策略（VECTOR/BM25/HYBRID）
     */
    @Column(name = "rag_strategy", length = 20)
    private String ragStrategy;

    /**
     * RAG Embedding 模型 ID
     */
    @Column(name = "rag_embedding_model_id", length = 64)
    private String ragEmbeddingModelId;

    /**
     * 关联 Agent ID（可选）
     * <p>
     * 如果设置了 Agent，对话时会使用 Agent 的工具和系统提示词。
     * Agent 配置会覆盖 modelId 和 systemMessage。
     * </p>
     */
    @Column(name = "agent_id", length = 64)
    private String agentId;

    /**
     * 创建人
     */
    @Column(length = 100)
    private String createdBy;

    /**
     * 持久化前初始化默认值
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (isActive == null) {
            isActive = true;
        }
        if (messageCount == null) {
            messageCount = 0;
        }
    }
}
