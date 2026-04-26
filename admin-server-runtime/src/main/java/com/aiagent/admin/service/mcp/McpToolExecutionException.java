package com.aiagent.admin.service.mcp;

/**
 * MCP 工具执行异常
 * <p>
 * MCP 工具调用执行过程中发生的异常。
 * </p>
 */
public class McpToolExecutionException extends RuntimeException {

    public McpToolExecutionException(String message) {
        super(message);
    }

    public McpToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}