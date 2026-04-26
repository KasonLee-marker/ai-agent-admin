package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.CreateMcpServerRequest;
import com.aiagent.admin.api.dto.McpServerJsonRequest;
import com.aiagent.admin.api.dto.McpServerResponse;
import com.aiagent.admin.domain.entity.McpServer;
import com.aiagent.admin.service.mcp.McpTool;

import java.util.List;
import java.util.Optional;

/**
 * MCP Server 服务接口
 * <p>
 * 提供 MCP Server 配置的管理功能：
 * <ul>
 *   <li>MCP Server CRUD</li>
 *   <li>工具列表刷新</li>
 *   <li>工具注册</li>
 * </ul>
 * </p>
 *
 * @see McpServer
 * @see McpTool
 */
public interface McpServerService {

    /**
     * 从 JSON 配置创建 MCP Server
     * <p>
     * 解析标准 MCP 配置格式，自动识别 SSE 或 Stdio transport。
     * 支持批量添加多个 Server。
     * </p>
     *
     * @param request JSON 配置请求
     * @return MCP Server 响应列表
     */
    List<McpServerResponse> createFromJson(McpServerJsonRequest request);

    /**
     * 从 JSON 配置更新 MCP Server
     * <p>
     * 解析配置并更新指定 Server。
     * </p>
     *
     * @param id      MCP Server ID
     * @param request JSON 配置请求
     * @return MCP Server 响应
     */
    McpServerResponse updateFromJson(String id, McpServerJsonRequest request);

    /**
     * 创建 MCP Server 配置
     *
     * @param request 创建请求
     * @return MCP Server 响应
     */
    McpServerResponse create(CreateMcpServerRequest request);

    /**
     * 获取 MCP Server 列表
     *
     * @return MCP Server 响应列表
     */
    List<McpServerResponse> listAll();

    /**
     * 获取 MCP Server 详情
     *
     * @param id MCP Server ID
     * @return MCP Server 响应 Optional
     */
    Optional<McpServerResponse> getById(String id);

    /**
     * 删除 MCP Server 配置
     *
     * @param id MCP Server ID
     */
    void delete(String id);

    /**
     * 更新 MCP Server 配置
     *
     * @param id      MCP Server ID
     * @param request 更新请求
     * @return MCP Server 响应
     */
    McpServerResponse update(String id, CreateMcpServerRequest request);

    /**
     * 刷新 MCP Server 工具列表
     * <p>
     * 连接 MCP Server，获取工具列表，注册到 Tool 表。
     * </p>
     *
     * @param id MCP Server ID
     * @return 工具列表
     */
    List<McpTool> refreshTools(String id);

    /**
     * 查找 MCP Server 实体
     *
     * @param id MCP Server ID
     * @return MCP Server 实体 Optional
     */
    Optional<McpServer> findEntityById(String id);
}