package com.aiagent.admin.service.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具执行结果
 * <p>
 * 包含工具执行的输出内容或错误信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolResult {

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 输出内容
     * <p>
     * 工具执行成功时返回的内容。
     * </p>
     */
    private Object content;

    /**
     * 错误信息
     * <p>
     * 工具执行失败时的错误描述。
     * </p>
     */
    private String errorMessage;

    /**
     * 是否为错误结果
     */
    private boolean isError;

    /**
     * 创建成功结果
     */
    public static McpToolResult success(Object content) {
        return McpToolResult.builder()
                .success(true)
                .content(content)
                .isError(false)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static McpToolResult error(String errorMessage) {
        return McpToolResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .isError(true)
                .build();
    }
}