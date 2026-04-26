package com.aiagent.admin.api.advice;

import com.aiagent.admin.api.dto.ApiResponse;
import com.aiagent.admin.service.mcp.McpConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * MCP Server 全局异常处理器
 * <p>
 * 统一处理 MCP 相关的异常，返回友好的错误信息。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class McpExceptionHandler {

    /**
     * 处理 MCP 连接异常
     *
     * @param ex MCP 连接异常
     * @return API 错误响应
     */
    @ExceptionHandler(McpConnectionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleMcpConnectionException(McpConnectionException ex) {
        log.error("MCP Connection error: {}", ex.getMessage(), ex);

        // 提取最底层的错误原因
        String message = ex.getMessage();

        // 如果是命令不存在，给出更友好的提示
        if (message != null && message.contains("Cannot run program") || message.contains("CreateProcess error=2") || message.contains("系统找不到指定的文件")) {
            message = "MCP Server 启动失败：命令未找到。请确保命令已安装并在系统 PATH 中。详情: " + message;
        } else if (message != null && message.contains("Command not found")) {
            message = "MCP Server 启动失败：命令未找到。请确保已安装并配置 PATH。详情: " + message;
        }

        return ApiResponse.error(message);
    }
}
