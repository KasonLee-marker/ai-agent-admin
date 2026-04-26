package com.aiagent.admin.domain.enums;

/**
 * 工具类型枚举
 * <p>
 * 定义工具的来源类型：
 * <ul>
 *   <li>BUILTIN - 内置工具，平台预定义的执行器</li>
 *   <li>CUSTOM - 自定义工具，用户配置的 HTTP API</li>
 *   <li>MCP - MCP 工具，从外部 MCP Server 发现的工具</li>
 * </ul>
 * </p>
 *
 * @see com.aiagent.admin.domain.entity.Tool
 */
public enum ToolType {

    /**
     * 内置工具
     * <p>
     * 平台预定义的工具，如 calculator、datetime、knowledge_retrieval。
     * 使用本地执行器执行。
     * </p>
     */
    BUILTIN,

    /**
     * 自定义工具
     * <p>
     * 用户配置的 HTTP API 工具，平台代理调用外部 API。
     * </p>
     */
    CUSTOM,

    /**
     * MCP 工具
     * <p>
     * 从外部 MCP Server 发现的工具，通过 MCP Client 代理调用。
     * Phase 2 功能。
     * </p>
     */
    MCP
}