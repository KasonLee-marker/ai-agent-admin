package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent 执行请求 DTO
 * <p>
 * 用于 Agent 单次执行或对话请求。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecuteRequest {

    /**
     * 用户消息内容
     */
    @NotBlank(message = "Message is required")
    private String message;

    /**
     * 会话 ID（可选）
     * <p>
     * 用于多轮对话。如果为空，将创建新会话。
     * </p>
     */
    private String sessionId;

    /**
     * 执行上下文（可选）
     * <p>
     * 传递给工具执行器的额外信息。
     * </p>
     */
    private Map<String, Object> context;
}