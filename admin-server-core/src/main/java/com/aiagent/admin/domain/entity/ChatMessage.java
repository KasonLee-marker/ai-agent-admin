package com.aiagent.admin.domain.entity;

import com.aiagent.admin.domain.enums.MessageRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(length = 100)
    private String modelName;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "is_error")
    @Builder.Default
    private Boolean isError = false;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (isError == null) {
            isError = false;
        }
    }
}
