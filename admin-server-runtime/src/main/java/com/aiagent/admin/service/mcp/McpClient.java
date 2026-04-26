package com.aiagent.admin.service.mcp;

import java.util.List;
import java.util.Map;

/**
 * MCP Client 接口
 * <p>
 * 定义 MCP (Model Context Protocol) 客户端的核心功能：
 * <ul>
 *   <li>连接 MCP Server</li>
 *   <li>获取可用工具列表</li>
 *   <li>调用 MCP 工具</li>
 *   <li>断开连接</li>
 * </ul>
 * </p>
 *
 * @see McpServerConfig
 * @see McpTool
 * @see McpToolResult
 */
public interface McpClient {

    /**
     * 连接 MCP Server
     * <p>
     * 启动 MCP Server 进程，发送 initialize 请求，
     * 获取 Server 能力信息。
     * </p>
     *
     * @param config MCP Server 配置
     * @throws McpConnectionException 连接失败时抛出
     */
    void connect(McpServerConfig config) throws McpConnectionException;

    /**
     * 检查是否已连接
     *
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 获取可用工具列表
     * <p>
     * 发送 tools/list 请求，获取 MCP Server 提供的所有工具。
     * </p>
     *
     * @return 工具列表
     * @throws McpConnectionException 未连接或请求失败时抛出
     */
    List<McpTool> listTools() throws McpConnectionException;

    /**
     * 调用 MCP 工具
     * <p>
     * 发送 tools/call 请求，执行指定工具。
     * </p>
     *
     * @param toolName 工具名称
     * @param args     工具参数
     * @return 执行结果
     * @throws McpConnectionException    未连接或请求失败时抛出
     * @throws McpToolExecutionException 工具执行失败时抛出
     */
    McpToolResult callTool(String toolName, Map<String, Object> args)
            throws McpConnectionException, McpToolExecutionException;

    /**
     * 断开连接
     * <p>
     * 关闭 MCP Server 进程，释放资源。
     * </p>
     */
    void disconnect();

    /**
     * 获取 Server 名称
     *
     * @return Server 名称
     */
    String getServerName();
}