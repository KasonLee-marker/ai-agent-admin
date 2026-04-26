package com.aiagent.admin.service.mcp;

/**
 * MCP 连接异常
 * <p>
 * MCP Client 连接或通信过程中发生的异常。
 * </p>
 */
public class McpConnectionException extends RuntimeException {

    public McpConnectionException(String message) {
        super(message);
    }

    public McpConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}