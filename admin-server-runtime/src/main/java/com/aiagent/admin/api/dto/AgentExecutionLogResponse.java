package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 执行日志响应 DTO
 * <p>
 * 返回执行日志的详细信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionLogResponse {

    /**
     * 执行日志 ID
     */
    private String id;

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 用户输入（截断显示）
     */
    private String inputSummary;

    /**
     * AI 输出（截断显示）
     */
    private String outputSummary;

    /**
     * 工具调用次数
     */
    private Integer toolCallCount;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 工具调用详情列表
     */
    private List<ToolCallRecord> toolCalls;
}