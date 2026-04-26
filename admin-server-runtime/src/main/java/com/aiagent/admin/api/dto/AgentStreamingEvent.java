package com.aiagent.admin.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Agent 流式事件
 * <p>
 * 包装 Agent 流式执行过程中的各类事件数据，用于 SSE 输出。
 * 每个事件包含类型、内容和可选的工具调用信息。
 * </p>
 *
 * @see StreamingEventType
 * @see ToolCallRecord
 */
@Data
@Builder
public class AgentStreamingEvent {

    /**
     * 事件类型
     */
    private StreamingEventType type;

    /**
     * 事件内容（文本或 JSON）
     */
    private String content;

    /**
     * 工具调用信息（仅 TOOL_START 和 TOOL_END 事件）
     */
    private ToolCallRecord toolCall;

    /**
     * 事件序号（用于前端排序）
     */
    private int sequence;

    /**
     * 时间戳（ISO 格式）
     */
    private String timestamp;

    /**
     * 创建模型输出事件
     *
     * @param content  输出内容
     * @param sequence 事件序号
     * @return 事件对象
     */
    public static AgentStreamingEvent modelOutput(String content, int sequence) {
        return AgentStreamingEvent.builder()
                .type(StreamingEventType.MODEL_OUTPUT)
                .content(content)
                .sequence(sequence)
                .timestamp(java.time.Instant.now().toString())
                .build();
    }

    /**
     * 创建工具开始事件
     *
     * @param toolCall 工具调用信息
     * @param sequence 事件序号
     * @return 事件对象
     */
    public static AgentStreamingEvent toolStart(ToolCallRecord toolCall, int sequence) {
        return AgentStreamingEvent.builder()
                .type(StreamingEventType.TOOL_START)
                .content("正在调用工具: " + toolCall.getToolName())
                .toolCall(toolCall)
                .sequence(sequence)
                .timestamp(java.time.Instant.now().toString())
                .build();
    }

    /**
     * 创建工具完成事件
     *
     * @param toolCall 工具调用信息（含结果）
     * @param sequence 事件序号
     * @return 事件对象
     */
    public static AgentStreamingEvent toolEnd(ToolCallRecord toolCall, int sequence) {
        return AgentStreamingEvent.builder()
                .type(StreamingEventType.TOOL_END)
                .content("工具调用完成: " + toolCall.getToolName())
                .toolCall(toolCall)
                .sequence(sequence)
                .timestamp(java.time.Instant.now().toString())
                .build();
    }

    /**
     * 创建错误事件
     *
     * @param errorMessage 错误消息
     * @param sequence     事件序号
     * @return 事件对象
     */
    public static AgentStreamingEvent error(String errorMessage, int sequence) {
        return AgentStreamingEvent.builder()
                .type(StreamingEventType.ERROR)
                .content(errorMessage)
                .sequence(sequence)
                .timestamp(java.time.Instant.now().toString())
                .build();
    }

    /**
     * 创建完成事件
     *
     * @param sequence 事件序号
     * @return 事件对象
     */
    public static AgentStreamingEvent done(int sequence) {
        return AgentStreamingEvent.builder()
                .type(StreamingEventType.DONE)
                .content("执行完成")
                .sequence(sequence)
                .timestamp(java.time.Instant.now().toString())
                .build();
    }
}