package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent 执行响应 DTO
 * <p>
 * 包含 AI 响应内容和工具调用记录。
 * </p>
 *
 * @see ToolCallRecord
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecuteResponse {

    /**
     * AI 响应内容
     */
    private String response;

    /**
     * 会话 ID
     * <p>
     * 用于后续对话。
     * </p>
     */
    private String sessionId;

    /**
     * 工具调用记录列表
     */
    private List<ToolCallRecord> toolCalls;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 执行日志 ID
     */
    private String executionLogId;
}