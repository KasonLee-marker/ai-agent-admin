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
 * 聊天消息实体
 * <p>
 * 存储对话会话中的单条消息，包括：
 * <ul>
 *   <li>用户消息 - 用户输入的问题或请求</li>
 *   <li>助手消息 - AI 生成的回答或响应</li>
 *   <li>系统消息 - 预设的系统提示词</li>
 * </ul>
 * </p>
 *
 * @see ChatSession
 * @see MessageRole
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_session_id", columnList = "sessionId"),
    @Index(name = "idx_session_created", columnList = "sessionId, createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 所属会话 ID
     */
    @Column(nullable = false, length = 64)
    private String sessionId;

    /**
     * 消息角色（USER/ASSISTANT/SYSTEM）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    /**
     * 消息内容
     */
    @Column(nullable = false, length = 10000)
    private String content;

    /**
     * 使用的模型名称（仅助手消息）
     */
    @Column(length = 100)
    private String modelName;

    /**
     * Token 消耗数量
     */
    @Column(name = "token_count")
    private Integer tokenCount;

    /**
     * 响应延迟（毫秒）
     */
    @Column(name = "latency_ms")
    private Long latencyMs;

    /**
     * 是否为错误消息
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
     * RAG 检索来源 JSON
     * <p>
     * 仅助手消息有此字段，存储检索来源的 JSON 格式数据，
     * 包含 chunkId、documentId、documentName、score 等信息。
     * </p>
     */
    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    /**
     * 工具调用记录 JSON
     * <p>
     * 仅Agent对话的助手消息有此字段，存储工具调用记录的 JSON 格式数据，
     * 包含 toolId、toolName、args、result、success、durationMs 等信息。
     * </p>
     */
    @Column(name = "tool_calls", columnDefinition = "TEXT")
    private String toolCalls;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 持久化前初始化默认值
     */
    @PrePersist
    public void prePersist() {
        if (isError == null) {
            isError = false;
        }
    }
}
