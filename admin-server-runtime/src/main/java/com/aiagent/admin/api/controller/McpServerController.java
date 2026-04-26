package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.repository.ToolRepository;
import com.aiagent.admin.service.McpServerService;
import com.aiagent.admin.service.mapper.ToolMapper;
import com.aiagent.admin.service.mcp.McpTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP Server 管理 REST 控制器
 * <p>
 * 提供 MCP Server 配置的管理 API：
 * <ul>
 *   <li>MCP Server CRUD 操作</li>
 *   <li>JSON 配置解析（自动识别 SSE/Stdio）</li>
 *   <li>工具列表刷新</li>
 * </ul>
 * </p>
 *
 * @see McpServerService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mcp-servers")
@RequiredArgsConstructor
@Tag(name = "MCP Server Management", description = "APIs for managing MCP server configurations")
public class McpServerController {

    private final McpServerService mcpServerService;
    private final ToolRepository toolRepository;
    private final ToolMapper toolMapper;

    /**
     * 从 JSON 配置创建 MCP Server
     * <p>
     * 解析标准 MCP 配置格式，自动识别 SSE 或 Stdio transport。
     * 格式示例：
     * <pre>
     * {
     *   "mcpServers": {
     *     "server-name": {
     *       "url": "https://..."
     *     },
     *     "memory": {
     *       "command": "npx",
     *       "args": ["-y", "@modelcontextprotocol/server-memory"]
     *     }
     *   }
     * }
     * </pre>
     * </p>
     *
     * @param request JSON 配置请求
     * @return MCP Server 响应列表
     */
    @PostMapping("/from-json")
    @Operation(summary = "Create MCP servers from JSON config",
            description = "Parse MCP config JSON, auto-detect SSE/Stdio transport")
    public ApiResponse<List<McpServerResponse>> createFromJson(
            @Valid @RequestBody McpServerJsonRequest request) {
        return ApiResponse.success(mcpServerService.createFromJson(request));
    }

    /**
     * 从 JSON 配置更新 MCP Server
     *
     * @param id      MCP Server ID
     * @param request JSON 配置请求
     * @return MCP Server 响应
     */
    @PutMapping("/{id}/from-json")
    @Operation(summary = "Update MCP server from JSON config")
    public ApiResponse<McpServerResponse> updateFromJson(
            @Parameter(description = "MCP Server ID") @PathVariable String id,
            @Valid @RequestBody McpServerJsonRequest request) {
        return ApiResponse.success(mcpServerService.updateFromJson(id, request));
    }

    /**
     * 获取 MCP Server 列表
     *
     * @return MCP Server 响应列表
     */
    @GetMapping
    @Operation(summary = "List all MCP servers")
    public ApiResponse<List<McpServerResponse>> listMcpServers() {
        return ApiResponse.success(mcpServerService.listAll());
    }

    /**
     * 获取 MCP Server 详情
     *
     * @param id MCP Server ID
     * @return MCP Server 响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get MCP server by ID")
    public ApiResponse<McpServerResponse> getMcpServer(
            @Parameter(description = "MCP Server ID") @PathVariable String id) {
        return mcpServerService.getById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "MCP Server not found"));
    }

    /**
     * 删除 MCP Server 配置
     *
     * @param id MCP Server ID
     * @return 成功响应
     */
    /**
     * 获取引用指定 MCP Server 的 Agent 列表
     * <p>
     * 返回绑定了该 MCP Server 下任何工具的 Agent 列表。
     * </p>
     *
     * @param id MCP Server ID
     * @return Agent 信息列表
     */
    @GetMapping("/{id}/referencing-agents")
    @Operation(summary = "Get agents referencing this MCP Server",
            description = "Returns list of agents that have bound tools from this MCP Server")
    public ApiResponse<List<AgentInfoDTO>> getReferencingAgents(
            @Parameter(description = "MCP Server ID") @PathVariable String id) {
        return ApiResponse.success(mcpServerService.getReferencingAgents(id));
    }
    @Operation(summary = "Delete MCP server")
    public ApiResponse<Void> deleteMcpServer(
            @Parameter(description = "MCP Server ID") @PathVariable String id) {
        mcpServerService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 更新 MCP Server 基本信息
     * <p>
     * 更新描述等基本信息，不修改连接配置。
     * </p>
     *
     * @param id      MCP Server ID
     * @param request 更新请求
     * @return MCP Server 响应
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update MCP server basic info",
            description = "Update description and other basic info, not connection config")
    public ApiResponse<McpServerResponse> updateMcpServer(
            @Parameter(description = "MCP Server ID") @PathVariable String id,
            @RequestBody CreateMcpServerRequest request) {
        return ApiResponse.success(mcpServerService.update(id, request));
    }

    /**
     * 刷新 MCP Server 工具列表
     * <p>
     * 连接 MCP Server，获取工具列表，注册到 Tool 表。
     * </p>
     *
     * @param id MCP Server ID
     * @return 工具列表
     */
    @PostMapping("/{id}/refresh-tools")
    @Operation(summary = "Refresh tools from MCP server")
    public ApiResponse<List<McpTool>> refreshTools(
            @Parameter(description = "MCP Server ID") @PathVariable String id) {
        return ApiResponse.success(mcpServerService.refreshTools(id));
    }

    /**
     * 获取 MCP Server 已注册的工具列表
     * <p>
     * 从 Tool 表查询该 MCP Server 已保存的工具。
     * </p>
     *
     * @param id MCP Server ID
     * @return 工具列表
     */
    @GetMapping("/{id}/tools")
    @Operation(summary = "Get tools registered for MCP server",
            description = "Query saved tools from Tool table by MCP Server ID")
    public ApiResponse<List<ToolResponse>> getMcpServerTools(
            @Parameter(description = "MCP Server ID") @PathVariable String id) {
        List<com.aiagent.admin.domain.entity.Tool> tools = toolRepository.findByMcpServerId(id);
        return ApiResponse.success(toolMapper.toResponseList(tools));
    }
}