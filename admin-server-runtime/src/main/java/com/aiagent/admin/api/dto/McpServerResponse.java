package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MCP Server 响应 DTO
 * <p>
 * 返回 MCP Server 配置信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerResponse {

    /**
     * MCP Server ID
     */
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * Transport 类型
     */
    private String transportType;

    /**
     * 启动命令（stdio transport）
     */
    private String command;

    /**
     * SSE URL（sse transport）
     */
    private String url;

    /**
     * 命令参数
     */
    private List<String> args;

    /**
     * 环境变量
     */
    private Map<String, String> env;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 工具数量
     */
    private Integer toolCount;
}