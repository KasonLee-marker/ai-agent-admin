package com.aiagent.admin.service.event;

import lombok.Getter;

/**
 * MCP Server 工具刷新事件
 * <p>
 * 用于异步刷新 MCP Server 工具列表，避免 npx 安装超时。
 * </p>
 */
@Getter
public class McpServerRefreshEvent {
    
    private final String serverId;
    
    public McpServerRefreshEvent(String serverId) {
        this.serverId = serverId;
    }
}
