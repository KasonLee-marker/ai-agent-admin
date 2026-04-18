package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Prompt 模板服务接口
 * <p>
 * 提供 Prompt 模板的管理功能：
 * <ul>
 *   <li>模板的创建、查询、更新、删除</li>
 *   <li>模板版本管理和回滚</li>
 *   <li>模板变量提取</li>
 *   <li>模板分类和标签管理</li>
 * </ul>
 * </p>
 * <p>
 * 支持变量占位符语法：{{variable_name}}，在调用时动态替换。
 * </p>
 *
 * @see PromptTemplateResponse
 * @see PromptVersionResponse
 */
public interface PromptService {

    /**
     * 创建 Prompt 模板
     *
     * @param request 创建请求，包含名称、内容、分类等
     * @return 创建成功的模板响应 DTO
     */
    PromptTemplateResponse createPrompt(PromptTemplateCreateRequest request);

    /**
     * 更新 Prompt 模板
     * <p>
     * 更新内容会创建新版本，保留历史版本。
     * </p>
     *
     * @param id      模板唯一标识
     * @param request 更新请求
     * @return 更新后的模板响应 DTO
     */
    PromptTemplateResponse updatePrompt(String id, PromptTemplateUpdateRequest request);

    /**
     * 删除 Prompt 模板
     * <p>
     * 同时删除模板的所有版本。
     * </p>
     *
     * @param id 模板唯一标识
     */
    void deletePrompt(String id);

    /**
     * 获取 Prompt 模板详情
     *
     * @param id 模板唯一标识
     * @return 模板响应 DTO
     */
    PromptTemplateResponse getPrompt(String id);

    /**
     * 分页查询 Prompt 模板列表
     *
     * @param category 分类过滤（可选）
     * @param tag      标签过滤（可选）
     * @param keyword  搜索关键词（可选）
     * @param pageable 分页参数
     * @return 模板分页响应
     */
    PageResponse<PromptTemplateResponse> listPrompts(String category, String tag, String keyword, Pageable pageable);

    /**
     * 获取模板的所有版本列表
     *
     * @param promptId 模板 ID
     * @return 版本响应列表
     */
    List<PromptVersionResponse> getPromptVersions(String promptId);

    /**
     * 回滚模板到指定版本
     *
     * @param promptId 模板 ID
     * @param request  回滚请求，包含目标版本号
     * @return 回滚后的模板响应 DTO
     */
    PromptTemplateResponse rollbackPrompt(String promptId, RollbackRequest request);

    /**
     * 从模板内容中提取变量名列表
     * <p>
     * 解析 {{variable_name}} 格式的占位符，返回变量名列表。
     * </p>
     *
     * @param content 模板内容
     * @return 变量名列表
     */
    List<String> extractVariables(String content);
}
