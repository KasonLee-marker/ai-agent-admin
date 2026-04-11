package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.PromptTemplate;
import com.aiagent.admin.domain.entity.PromptVersion;
import com.aiagent.admin.domain.repository.PromptTemplateRepository;
import com.aiagent.admin.domain.repository.PromptVersionRepository;
import com.aiagent.admin.service.PromptService;
import com.aiagent.admin.service.mapper.PromptMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词模板服务实现类
 * <p>
 * 提供提示词模板的核心管理功能：
 * <ul>
 *   <li>模板创建、更新、删除、查询</li>
 *   <li>版本管理（自动创建新版本、版本回滚）</li>
 *   <li>变量提取（识别 {{variable}} 格式的模板变量）</li>
 * </ul>
 * </p>
 * <p>
 * 更新模板时自动创建新版本，支持版本历史查询和回滚。
 * 模板变量使用双大括号格式：{{variableName}}。
 * </p>
 *
 * @see PromptService
 * @see PromptTemplate
 * @see PromptVersion
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptServiceImpl implements PromptService {

    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptVersionRepository promptVersionRepository;
    private final PromptMapper promptMapper;

    /**
     * 模板变量匹配模式：{{variableName}}
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * 创建新的提示词模板
     * <p>
     * 创建流程：
     * <ol>
     *   <li>检查名称唯一性</li>
     *   <li>创建模板实体并保存</li>
     *   <li>自动提取模板变量</li>
     *   <li>创建初始版本记录（版本号 1）</li>
     * </ol>
     * </p>
     *
     * @param request 创建模板请求，包含名称、内容、描述、分类、标签等
     * @return 创建成功的模板响应 DTO
     * @throws IllegalArgumentException 模板名称已存在时抛出
     */
    @Override
    @Transactional
    public PromptTemplateResponse createPrompt(PromptTemplateCreateRequest request) {
        if (promptTemplateRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Prompt with name '" + request.getName() + "' already exists");
        }

        PromptTemplate entity = promptMapper.toEntity(request);
        
        List<String> variables = extractVariables(request.getContent());
        entity.setVariables(String.join(",", variables));
        
        PromptTemplate saved = promptTemplateRepository.save(entity);
        
        PromptVersion version = PromptVersion.builder()
                .promptId(saved.getId())
                .version(1)
                .content(saved.getContent())
                .changeLog("Initial version")
                .build();
        promptVersionRepository.save(version);
        
        return promptMapper.toResponse(saved);
    }

    /**
     * 更新提示词模板
     * <p>
     * 更新流程：
     * <ol>
     *   <li>验证模板存在</li>
     *   <li>检查新名称唯一性（如果名称变更）</li>
     *   <li>自动创建新版本记录（版本号递增）</li>
     *   <li>更新模板内容和元数据</li>
     *   <li>重新提取模板变量</li>
     * </ol>
     * </p>
     *
     * @param id      模板唯一标识
     * @param request 更新请求，包含新内容、新名称等
     * @return 更新后的模板响应 DTO
     * @throws EntityNotFoundException   模板不存在时抛出
     * @throws IllegalArgumentException  新名称已被占用时抛出
     */
    @Override
    @Transactional
    public PromptTemplateResponse updatePrompt(String id, PromptTemplateUpdateRequest request) {
        PromptTemplate existing = promptTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found with id: " + id));
        
        if (!existing.getName().equals(request.getName()) && promptTemplateRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Prompt with name '" + request.getName() + "' already exists");
        }

        PromptVersion version = PromptVersion.builder()
                .promptId(id)
                .version(existing.getVersion() + 1)
                .content(request.getContent())
                .changeLog(request.getChangeLog() != null ? request.getChangeLog() : "Updated version")
                .build();
        promptVersionRepository.save(version);

        existing.setName(request.getName());
        existing.setContent(request.getContent());
        existing.setDescription(request.getDescription());
        existing.setCategory(request.getCategory());
        existing.setTags(promptMapper.listToString(request.getTags()));
        existing.setVersion(existing.getVersion() + 1);
        
        List<String> variables = extractVariables(request.getContent());
        existing.setVariables(String.join(",", variables));
        
        PromptTemplate updated = promptTemplateRepository.save(existing);
        return promptMapper.toResponse(updated);
    }

    /**
     * 删除提示词模板及其所有版本
     * <p>
     * 同时删除模板实体和所有历史版本记录。
     * </p>
     *
     * @param id 模板唯一标识
     * @throws EntityNotFoundException 模板不存在时抛出
     */
    @Override
    @Transactional
    public void deletePrompt(String id) {
        if (!promptTemplateRepository.existsById(id)) {
            throw new EntityNotFoundException("Prompt not found with id: " + id);
        }
        promptVersionRepository.deleteAll(promptVersionRepository.findByPromptIdOrderByVersionDesc(id));
        promptTemplateRepository.deleteById(id);
    }

    /**
     * 根据ID获取提示词模板
     *
     * @param id 模板唯一标识
     * @return 模板响应 DTO
     * @throws EntityNotFoundException 模板不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public PromptTemplateResponse getPrompt(String id) {
        PromptTemplate entity = promptTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found with id: " + id));
        return promptMapper.toResponse(entity);
    }

    /**
     * 分页查询提示词模板列表
     * <p>
     * 支持按分类、标签、关键词筛选。
     * 按更新时间倒序排列。
     * </p>
     *
     * @param category 分类过滤条件（可选）
     * @param tag      标签过滤条件（可选）
     * @param keyword  搜索关键词（可选）
     * @param pageable 分页参数
     * @return 分页的模板响应 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromptTemplateResponse> listPrompts(String category, String tag, String keyword, Pageable pageable) {
        Page<PromptTemplate> page = promptTemplateRepository.findByFilters(category, tag, keyword, pageable);
        Page<PromptTemplateResponse> responsePage = page.map(promptMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    /**
     * 获取模板的版本历史列表
     * <p>
     * 按版本号倒序排列，返回完整的版本演变历史。
     * </p>
     *
     * @param promptId 模板唯一标识
     * @return 版本响应 DTO 列表
     * @throws EntityNotFoundException 模板不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public List<PromptVersionResponse> getPromptVersions(String promptId) {
        if (!promptTemplateRepository.existsById(promptId)) {
            throw new EntityNotFoundException("Prompt not found with id: " + promptId);
        }
        List<PromptVersion> versions = promptVersionRepository.findByPromptIdOrderByVersionDesc(promptId);
        return promptMapper.toVersionResponseList(versions);
    }

    /**
     * 回滚模板到指定版本
     * <p>
     * 回滚流程：
     * <ol>
     *   <li>验证模板和目标版本存在</li>
     *   <li>创建新版本记录（内容复制自目标版本）</li>
     *   <li>更新模板当前内容为目标版本内容</li>
     *   <li>重新提取模板变量</li>
     * </ol>
     * </p>
     * <p>
     * 回滚操作本身会创建一个新版本，版本号继续递增。
     * </p>
     *
     * @param promptId 模板唯一标识
     * @param request  回滚请求，包含目标版本号和可选的变更日志
     * @return 回滚后的模板响应 DTO
     * @throws EntityNotFoundException 模板或目标版本不存在时抛出
     */
    @Override
    @Transactional
    public PromptTemplateResponse rollbackPrompt(String promptId, RollbackRequest request) {
        PromptTemplate existing = promptTemplateRepository.findById(promptId)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found with id: " + promptId));
        
        PromptVersion targetVersion = promptVersionRepository.findByPromptIdAndVersion(promptId, request.getVersion())
                .orElseThrow(() -> new EntityNotFoundException("Version " + request.getVersion() + " not found for prompt: " + promptId));
        
        int newVersion = existing.getVersion() + 1;
        
        PromptVersion rollbackVersion = PromptVersion.builder()
                .promptId(promptId)
                .version(newVersion)
                .content(targetVersion.getContent())
                .changeLog("Rollback to version " + request.getVersion() + ": " + 
                          (request.getChangeLog() != null ? request.getChangeLog() : "Rolled back"))
                .build();
        promptVersionRepository.save(rollbackVersion);

        existing.setContent(targetVersion.getContent());
        existing.setVersion(newVersion);
        
        List<String> variables = extractVariables(targetVersion.getContent());
        existing.setVariables(String.join(",", variables));
        
        PromptTemplate updated = promptTemplateRepository.save(existing);
        return promptMapper.toResponse(updated);
    }

    /**
     * 从模板内容中提取变量名列表
     * <p>
     * 使用正则表达式匹配 {{variableName}} 格式的模板变量。
     * 自动去除重复变量和空白字符。
     * </p>
     *
     * @param content 模板内容
     * @return 变量名列表（去重后）
     */
    @Override
    public List<String> extractVariables(String content) {
        List<String> variables = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return variables;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String variable = matcher.group(1).trim();
            if (!variable.isEmpty() && !variables.contains(variable)) {
                variables.add(variable);
            }
        }
        return variables;
    }
}