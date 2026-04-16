package com.aiagent.admin.domain.entity;

import com.aiagent.admin.domain.enums.MessageRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * RAG 对话消息实体
 * <p>
 * 存储 RAG 会话中的单条消息（用户问题或 AI 回答），
 * 以及检索来源信息。
 * </p>
 *
 * @see RagSession
 */
@Entity
@Table(name = "rag_messages", indexes = {
        @Index(name = "idx_rag_session_id", columnList = "sessionId"),
        @Index(name = "idx_rag_session_created", columnList = "sessionId, createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagMessage {

    /**
     * 消息唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 所属会话 ID
     */
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    /**
     * 消息角色（USER/ASSISTANT）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    /**
     * 消息内容
     * <p>
     * 用户消息为问题内容，助手消息为回答内容。
     * </p>
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 检索来源 JSON
     * <p>
     * 仅助手消息有此字段，存储检索来源的 JSON 格式数据，
     * 包含 chunkId、documentId、score 等信息。
     * </p>
     */
    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    /**
     * 模型名称（仅助手消息）
     */
    @Column(name = "model_name", length = 100)
    private String modelName;

    /**
     * 响应延迟（毫秒，仅助手消息）
     */
    @Column(name = "latency_ms")
    private Long latencyMs;

    /**
     * 是否错误消息
     */
    @Column(name = "is_error")
    @Builder.Default
    private Boolean isError = false;

    /**
     * 错误信息
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (isError == null) {
            isError = false;
        }
    }
}