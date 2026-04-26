package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.ApiResponse;
import com.aiagent.admin.api.dto.CreateToolRequest;
import com.aiagent.admin.api.dto.ToolResponse;
import com.aiagent.admin.api.dto.UpdateToolRequest;
import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import com.aiagent.admin.service.ToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tool 管理 REST 控制器
 * <p>
 * 提供工具的管理 API：
 * <ul>
 *   <li>工具列表查询</li>
 *   <li>工具详情查询</li>
 *   <li>自定义工具 CRUD</li>
 *   <li>工具筛选查询</li>
 * </ul>
 * </p>
 *
 * @see ToolService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
@Tag(name = "Tool Management", description = "APIs for managing tools")
public class ToolController {

    private final ToolService toolService;

    /**
     * 查询工具列表
     * <p>
     * 支持按类型、类别、关键词筛选。
     * </p>
     *
     * @param type     工具类型筛选（可选）
     * @param category 工具类别筛选（可选）
     * @param keyword  搜索关键词（可选）
     * @return 工具响应 DTO 列表
     */
    @GetMapping
    @Operation(summary = "List all tools with optional filters")
    public ApiResponse<List<ToolResponse>> listTools(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        ToolType typeEnum = type != null ? ToolType.valueOf(type.toUpperCase()) : null;
        ToolCategory categoryEnum = category != null ? ToolCategory.valueOf(category.toUpperCase()) : null;
        return ApiResponse.success(toolService.findByFilters(typeEnum, categoryEnum, keyword));
    }

    /**
     * 查询所有内置工具
     *
     * @return 内置工具响应 DTO 列表
     */
    @GetMapping("/builtin")
    @Operation(summary = "List all built-in tools")
    public ApiResponse<List<ToolResponse>> listBuiltinTools() {
        return ApiResponse.success(toolService.findBuiltinTools());
    }

    /**
     * 根据 ID 获取工具详情
     *
     * @param id 工具 ID
     * @return 工具响应 DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get tool by ID")
    public ApiResponse<ToolResponse> getTool(
            @Parameter(description = "Tool ID") @PathVariable String id) {
        return toolService.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Tool not found"));
    }

    /**
     * 创建自定义工具
     *
     * @param request 创建请求
     * @return 创建成功的工具响应 DTO
     */
    @PostMapping
    @Operation(summary = "Create a custom tool")
    public ApiResponse<ToolResponse> createTool(
            @Valid @RequestBody CreateToolRequest request) {
        return ApiResponse.success(toolService.create(request));
    }

    /**
     * 更新自定义工具
     *
     * @param id      工具 ID
     * @param request 更新请求
     * @return 更新后的工具响应 DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update custom tool")
    public ApiResponse<ToolResponse> updateTool(
            @Parameter(description = "Tool ID") @PathVariable String id,
            @Valid @RequestBody UpdateToolRequest request) {
        return ApiResponse.success(toolService.update(id, request));
    }

    /**
     * 删除自定义工具
     * <p>
     * 仅允许删除 CUSTOM 类型工具。
     * </p>
     *
     * @param id 工具 ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete custom tool")
    public ApiResponse<Void> deleteTool(
            @Parameter(description = "Tool ID") @PathVariable String id) {
        toolService.delete(id);
        return ApiResponse.success();
    }
}