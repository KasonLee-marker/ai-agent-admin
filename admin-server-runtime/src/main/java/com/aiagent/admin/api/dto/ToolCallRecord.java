package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具调用记录 DTO
 * <p>
 * 记录单次工具调用的详细信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallRecord {

    /**
     * 工具 ID
     */
    private String toolId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 调用参数
     */
    private Map<String, Object> args;

    /**
     * 执行结果
     */
    private Object result;

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
}