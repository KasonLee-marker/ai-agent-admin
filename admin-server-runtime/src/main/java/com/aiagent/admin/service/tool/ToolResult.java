package com.aiagent.admin.service.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果
 * <p>
 * 包含工具执行后的结果数据，用于返回给 LLM 或记录日志。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果数据
     * <p>
     * 返回给 LLM 的结构化数据。
     * </p>
     */
    private Object data;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 创建成功结果
     *
     * @param data 结果数据
     * @return ToolResult
     */
    public static ToolResult success(Object data) {
        return ToolResult.builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 创建成功结果（含耗时）
     *
     * @param data       结果数据
     * @param durationMs 执行耗时
     * @return ToolResult
     */
    public static ToolResult success(Object data, long durationMs) {
        return ToolResult.builder()
                .success(true)
                .data(data)
                .durationMs(durationMs)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param errorMessage 错误信息
     * @return ToolResult
     */
    public static ToolResult failure(String errorMessage) {
        return ToolResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建失败结果（含耗时）
     *
     * @param errorMessage 错误信息
     * @param durationMs   执行耗时
     * @return ToolResult
     */
    public static ToolResult failure(String errorMessage, long durationMs) {
        return ToolResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();
    }
}