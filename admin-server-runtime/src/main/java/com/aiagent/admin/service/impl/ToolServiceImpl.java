package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.CreateToolRequest;
import com.aiagent.admin.api.dto.ToolResponse;
import com.aiagent.admin.api.dto.UpdateToolRequest;
import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import com.aiagent.admin.domain.repository.AgentToolRepository;
import com.aiagent.admin.domain.repository.ToolRepository;
import com.aiagent.admin.service.ToolIdGenerator;
import com.aiagent.admin.service.ToolService;
import com.aiagent.admin.service.mapper.ToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tool 服务实现类
 * <p>
 * 提供工具的管理功能实现：
 * <ul>
 *   <li>工具 CRUD 操作（仅 CUSTOM 类型）</li>
 *   <li>内置工具查询</li>
 *   <li>工具筛选查询</li>
 * </ul>
 * </p>
 *
 * @see ToolService
 * @see Tool
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolServiceImpl implements ToolService {

    private final ToolRepository toolRepository;
    private final AgentToolRepository agentToolRepository;
    private final ToolIdGenerator toolIdGenerator;
    private final ToolMapper toolMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ToolResponse> findAll() {
        return toolRepository.findAll().stream()
                .map(toolMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ToolResponse> findById(String id) {
        return toolRepository.findById(id)
                .map(toolMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ToolResponse> findByName(String name) {
        return toolRepository.findByName(name)
                .map(toolMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tool> findEntityById(String id) {
        return toolRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ToolResponse> findByFilters(ToolType type, ToolCategory category, String keyword) {
        return toolRepository.findByFilters(type, category, keyword).stream()
                .map(toolMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ToolResponse> findBuiltinTools() {
        return toolRepository.findByType(ToolType.BUILTIN).stream()
                .map(toolMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ToolResponse create(CreateToolRequest request) {
        // 检查名称唯一性
        if (toolRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Tool name already exists: " + request.getName());
        }

        // 创建实体
        Tool entity = toolMapper.toEntity(request);
        entity.setId(toolIdGenerator.generateId());

        Tool saved = toolRepository.save(entity);
        log.info("Created custom tool: {}", saved.getId());

        return toolMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ToolResponse update(String id, UpdateToolRequest request) {
        Tool existing = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + id));

        // 仅允许更新 CUSTOM 类型工具
        if (existing.getType() != ToolType.CUSTOM) {
            throw new IllegalArgumentException("Only CUSTOM type tools can be updated");
        }

        // 检查名称唯一性（如果修改了名称）
        if (!existing.getName().equals(request.getName()) &&
                toolRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Tool name already exists: " + request.getName());
        }

        // 更新实体
        toolMapper.updateEntityFromRequest(request, existing);

        Tool saved = toolRepository.save(existing);
        log.info("Updated tool: {}", saved.getId());

        return toolMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + id));

        // 仅允许删除 CUSTOM 类型工具
        if (tool.getType() != ToolType.CUSTOM) {
            throw new IllegalArgumentException("Only CUSTOM type tools can be deleted");
        }

        // 检查是否有 Agent 绑定了此工具
        if (agentToolRepository.findByToolId(id).size() > 0) {
            throw new IllegalArgumentException("Tool is bound to agents, cannot delete");
        }

        toolRepository.deleteById(id);
        log.info("Deleted tool: {}", id);
    }
}