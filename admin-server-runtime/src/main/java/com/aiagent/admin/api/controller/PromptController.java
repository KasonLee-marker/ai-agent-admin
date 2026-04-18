package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.PromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提示词模板管理 REST 控制器
 * <p>
 * 提供提示词模板的 CRUD 和版本管理 API：
 * <ul>
 *   <li>创建、查询、更新、删除模板</li>
 *   <li>版本历史查询和版本回滚</li>
 *   <li>按分类、标签、关键词筛选</li>
 * </ul>
 * </p>
 * <p>
 * 更新操作会自动创建新版本，支持完整的版本历史追踪。
 * </p>
 *
 * @see PromptService
 */
@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
@Tag(name = "Prompt Management", description = "APIs for managing prompt templates")
public class PromptController {

    private final PromptService promptService;

    /**
     * 创建新的提示词模板
     *
     * @param request 创建请求，包含模板名称、内容、分类、标签等
     * @return 创建成功的模板信息
     */
    @PostMapping
    @Operation(summary = "Create a new prompt template")
    public ApiResponse<PromptTemplateResponse> createPrompt(
            @Valid @RequestBody PromptTemplateCreateRequest request) {
        return ApiResponse.success(promptService.createPrompt(request));
    }

    /**
     * 根据ID获取提示词模板详情
     *
     * @param id 模板ID
     * @return 模板详情信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get prompt template by ID")
    public ApiResponse<PromptTemplateResponse> getPrompt(
            @Parameter(description = "Prompt ID") @PathVariable String id) {
        return ApiResponse.success(promptService.getPrompt(id));
    }

    /**
     * 分页查询提示词模板列表
     * <p>
     * 支持按分类、标签、关键词筛选，结果按更新时间倒序排列。
     * </p>
     *
     * @param category 分类筛选（可选）
     * @param tag      标签筛选（可选）
     * @param keyword  搜索关键词（可选，匹配名称和内容）
     * @param page     页码（从0开始）
     * @param size     每页数量
     * @return 分页的模板列表
     */
    @GetMapping
    @Operation(summary = "List prompt templates with pagination and filters")
    public ApiResponse<PageResponse<PromptTemplateResponse>> listPrompts(
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Filter by tag") @RequestParam(required = false) String tag,
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ApiResponse.success(promptService.listPrompts(category, tag, keyword, pageable));
    }

    /**
     * 更新提示词模板
     * <p>
     * 更新操作会自动创建新版本，保留历史版本记录。
     * </p>
     *
     * @param id      模板ID
     * @param request 更新请求，包含新的内容、分类等
     * @return 更新后的模板信息（新版本）
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a prompt template (auto-creates new version)")
    public ApiResponse<PromptTemplateResponse> updatePrompt(
            @Parameter(description = "Prompt ID") @PathVariable String id,
            @Valid @RequestBody PromptTemplateUpdateRequest request) {
        return ApiResponse.success(promptService.updatePrompt(id, request));
    }

    /**
     * 删除提示词模板
     * <p>
     * 同时删除模板的所有版本历史。
     * </p>
     *
     * @param id 模板ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prompt template")
    public ApiResponse<Void> deletePrompt(
            @Parameter(description = "Prompt ID") @PathVariable String id) {
        promptService.deletePrompt(id);
        return ApiResponse.success();
    }

    /**
     * 获取提示词模板的版本历史
     * <p>
     * 返回所有历史版本，按版本号倒序排列。
     * </p>
     *
     * @param id 模板ID
     * @return 版本历史列表
     */
    @GetMapping("/{id}/versions")
    @Operation(summary = "Get version history of a prompt template")
    public ApiResponse<List<PromptVersionResponse>> getPromptVersions(
            @Parameter(description = "Prompt ID") @PathVariable String id) {
        return ApiResponse.success(promptService.getPromptVersions(id));
    }

    /**
     * 回滚提示词模板到指定版本
     * <p>
     * 将模板内容恢复到历史版本，同时创建新的版本记录。
     * </p>
     *
     * @param id      模板ID
     * @param request 回滚请求，包含目标版本号
     * @return 回滚后的模板信息（新版本）
     */
    @PostMapping("/{id}/rollback")
    @Operation(summary = "Rollback a prompt template to a specific version")
    public ApiResponse<PromptTemplateResponse> rollbackPrompt(
            @Parameter(description = "Prompt ID") @PathVariable String id,
            @Valid @RequestBody RollbackRequest request) {
        return ApiResponse.success(promptService.rollbackPrompt(id, request));
    }
}
