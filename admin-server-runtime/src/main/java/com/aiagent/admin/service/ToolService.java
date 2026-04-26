package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.CreateToolRequest;
import com.aiagent.admin.api.dto.ToolResponse;
import com.aiagent.admin.api.dto.UpdateToolRequest;
import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;

import java.util.List;
import java.util.Optional;

/**
 * Tool 服务接口
 * <p>
 * 提供工具的管理功能：
 * <ul>
 *   <li>工具 CRUD 操作（仅 CUSTOM 类型）</li>
 *   <li>内置工具查询</li>
 *   <li>工具筛选查询</li>
 * </ul>
 * </p>
 *
 * @see Tool
 * @see ToolType
 * @see ToolCategory
 */
public interface ToolService {

    /**
     * 查询所有工具
     *
     * @return 工具响应 DTO 列表
     */
    List<ToolResponse> findAll();

    /**
     * 根据 ID 查询工具
     *
     * @param id 工具 ID
     * @return 工具响应 DTO（Optional）
     */
    Optional<ToolResponse> findById(String id);

    /**
     * 根据名称查询工具
     *
     * @param name 工具名称
     * @return 工具响应 DTO（Optional）
     */
    Optional<ToolResponse> findByName(String name);

    /**
     * 根据 ID 查询工具实体（用于内部调用）
     *
     * @param id 工具 ID
     * @return 工具实体（Optional）
     */
    Optional<Tool> findEntityById(String id);

    /**
     * 根据筛选条件查询工具列表
     *
     * @param type     工具类型（可选）
     * @param category 工具类别（可选）
     * @param keyword  搜索关键词（可选）
     * @return 工具响应 DTO 列表
     */
    List<ToolResponse> findByFilters(ToolType type, ToolCategory category, String keyword);

    /**
     * 查询所有内置工具
     *
     * @return 内置工具响应 DTO 列表
     */
    List<ToolResponse> findBuiltinTools();

    /**
     * 创建自定义工具
     *
     * @param request 创建请求
     * @return 创建成功的工具响应 DTO
     */
    ToolResponse create(CreateToolRequest request);

    /**
     * 更新自定义工具
     *
     * @param id      工具 ID
     * @param request 更新请求
     * @return 更新后的工具响应 DTO
     */
    ToolResponse update(String id, UpdateToolRequest request);

    /**
     * 删除自定义工具
     * <p>
     * 仅允许删除 CUSTOM 类型工具，BUILTIN 和 MCP 工具不可删除。
     * </p>
     *
     * @param id 工具 ID
     */
    void delete(String id);
}