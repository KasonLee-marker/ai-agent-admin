package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Agent;
import com.aiagent.admin.domain.enums.AgentStatus;

import java.util.List;
import java.util.Optional;

/**
 * Agent 服务接口
 * <p>
 * 提供 Agent 的管理功能：
 * <ul>
 *   <li>Agent CRUD 操作</li>
 *   <li>Agent-Tool 绑定管理</li>
 *   <li>Agent 状态管理</li>
 *   <li>Agent 筛选查询</li>
 * </ul>
 * </p>
 *
 * @see Agent
 * @see AgentStatus
 */
public interface AgentService {

    /**
     * 查询所有 Agent
     *
     * @return Agent 响应 DTO 列表
     */
    List<AgentResponse> findAll();

    /**
     * 根据 ID 查询 Agent
     *
     * @param id Agent ID
     * @return Agent 响应 DTO（Optional）
     */
    Optional<AgentResponse> findById(String id);

    /**
     * 根据 ID 查询 Agent 实体（用于内部调用）
     *
     * @param id Agent ID
     * @return Agent 实体（Optional）
     */
    Optional<Agent> findEntityById(String id);

    /**
     * 根据筛选条件查询 Agent 列表
     *
     * @param status  Agent 状态（可选）
     * @param modelId 模型 ID（可选）
     * @param keyword 搜索关键词（可选）
     * @return Agent 响应 DTO 列表
     */
    List<AgentResponse> findByFilters(AgentStatus status, String modelId, String keyword);

    /**
     * 创建 Agent
     *
     * @param request 创建请求
     * @return 创建成功的 Agent 响应 DTO
     */
    AgentResponse create(CreateAgentRequest request);

    /**
     * 更新 Agent
     *
     * @param id      Agent ID
     * @param request 更新请求
     * @return 更新后的 Agent 响应 DTO
     */
    AgentResponse update(String id, UpdateAgentRequest request);

    /**
     * 删除 Agent
     *
     * @param id Agent ID
     */
    void delete(String id);

    /**
     * 更新 Agent 状态
     *
     * @param id     Agent ID
     * @param status 目标状态
     * @return 更新后的 Agent 响应 DTO
     */
    AgentResponse updateStatus(String id, AgentStatus status);

    /**
     * 绑定工具到 Agent
     *
     * @param agentId Agent ID
     * @param request 工具绑定请求
     * @return 绑定关系响应 DTO
     */
    ToolBindingResponse bindTool(String agentId, ToolBindingRequest request);

    /**
     * 解绑 Agent 的工具
     *
     * @param agentId Agent ID
     * @param toolId  工具 ID
     */
    void unbindTool(String agentId, String toolId);

    /**
     * 更新 Agent-Tool 绑定配置
     *
     * @param agentId Agent ID
     * @param toolId  工具 ID
     * @param request 工具绑定请求
     * @return 更新后的绑定关系响应 DTO
     */
    ToolBindingResponse updateToolBinding(String agentId, String toolId, ToolBindingRequest request);

    /**
     * 获取 Agent 绑定的所有工具
     *
     * @param agentId Agent ID
     * @return 工具绑定响应 DTO 列表
     */
    List<ToolBindingResponse> getAgentTools(String agentId);
}