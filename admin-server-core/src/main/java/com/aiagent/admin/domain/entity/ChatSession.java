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
