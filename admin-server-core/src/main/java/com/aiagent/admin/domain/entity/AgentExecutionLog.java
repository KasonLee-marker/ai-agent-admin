package com.aiagent.admin.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Agent 执行日志实体
 * <p>
 * 记录 Agent 每次执行的详细信息，包括输入、输出、工具调用记录和耗时。
 * 用于调试、审计和性能分析。
 * </p>
 *
 * @see com.aiagent.admin.domain.entity.Agent
 */
@Entity
@Table(name = "agent_execution_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentExecutionLog {

    /**
     * 执行日志唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 执行的 Agent ID
     */
    @Column(name = "agent_id", nullable = false, length = 64)
    private String agentId;

    /**
     * 会话 ID（可选）
     * <p>
     * 用于关联多轮对话的执行记录。
     * </p>
     */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /**
     * 用户输入
     */
    @Column(name = "input", columnDefinition = "TEXT")
    private String input;

    /**
     * AI 输出响应
     */
    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    /**
     * 工具调用记录
     * <p>
     * JSON 格式，包含工具调用详情：
     * <ul>
     *   <li>toolName - 工具名称</li>
     *   <li>args - 调用参数</li>
     *   <li>result - 执行结果</li>
     *   <li>durationMs - 执行耗时</li>
     *   <li>success - 是否成功</li>
     * </ul>
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_calls", columnDefinition = "JSONB")
    private String toolCalls;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * 是否成功
     */
    @Column(name = "success")
    @Builder.Default
    private Boolean success = true;

    /**
     * 错误信息（失败时）
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}