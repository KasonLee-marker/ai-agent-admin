package com.aiagent.admin.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Agent-Tool 绑定实体类
 * <p>
 * 用于管理 Agent 与工具的绑定关系，支持：
 * <ul>
 *   <li>一个 Agent 可绑定多个工具</li>
 *   <li>每个绑定可配置独立的参数（如知识库 ID）</li>
 *   <li>绑定可启用/禁用</li>
 * </ul>
 * </p>
 *
 * @see Agent
 * @see Tool
 */
@Entity
@Table(name = "agent_tools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTool {

    /**
     * 绑定关系唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 绑定的 Agent ID
     */
    @Column(name = "agent_id", nullable = false, length = 64)
    private String agentId;

    /**
     * 绑定的工具 ID
     */
    @Column(name = "tool_id", nullable = false, length = 64)
    private String toolId;

    /**
     * 是否启用
     * <p>
     * 禁用的工具在 Agent 执行时不会被调用。
     * </p>
     */
    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 工具配置
     * <p>
     * JSON 格式，Agent 级别的工具配置覆盖：
     * <ul>
     *   <li>knowledge_retrieval: kbId、topK、threshold</li>
     *   <li>shell_command: 允许的命令列表覆盖</li>
     * </ul>
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config")
    @Builder.Default
    private String config = "{}";

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 持久化前初始化默认值
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (enabled == null) {
            enabled = true;
        }
        if (config == null || config.isEmpty()) {
            config = "{}";
        }
    }
}