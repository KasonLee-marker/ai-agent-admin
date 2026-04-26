package com.aiagent.admin.api.dto;

/**
 * 流式事件类型枚举
 * <p>
 * 定义 Agent 流式执行过程中不同的事件类型：
 * <ul>
 *   <li>MODEL_OUTPUT - 模型响应文本输出</li>
 *   <li>TOOL_START - 工具调用开始</li>
 *   <li>TOOL_END - 工具调用完成</li>
 *   <li>THINKING - 模型思考过程（可选）</li>
 *   <li>ERROR - 执行错误</li>
 *   <li>DONE - 整体执行完成</li>
 * </ul>
 * </p>
 *
 * @see AgentStreamingEvent
 */
public enum StreamingEventType {
    /**
     * 模型响应文本输出（增量）
     */
    MODEL_OUTPUT,

    /**
     * 工具调用开始
     */
    TOOL_START,

    /**
     * 工具调用完成（含结果）
     */
    TOOL_END,

    /**
     * 模型思考过程
     */
    THINKING,

    /**
     * 执行错误
     */
    ERROR,

    /**
     * 整体执行完成
     */
    DONE
}