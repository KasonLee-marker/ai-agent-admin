package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Agent;
import com.aiagent.admin.domain.entity.AgentTool;
import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.AgentStatus;
import com.aiagent.admin.domain.repository.AgentRepository;
import com.aiagent.admin.domain.repository.AgentToolRepository;
import com.aiagent.admin.domain.repository.ToolRepository;
import com.aiagent.admin.service.AgentIdGenerator;
import com.aiagent.admin.service.AgentService;
import com.aiagent.admin.service.AgentToolIdGenerator;
import com.aiagent.admin.service.ModelConfigService;
import com.aiagent.admin.service.mapper.AgentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Agent 服务实现类
 * <p>
 * 提供 Agent 的管理功能实现：
 * <ul>
 *   <li>Agent CRUD 操作</li>
 *   <li>Agent-Tool 绑定管理</li>
 *   <li>Agent 状态管理</li>
 * </ul>
 * </p>
 *
 * @see AgentService
 * @see Agent
 * @see AgentTool
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final AgentToolRepository agentToolRepository;
    private final ToolRepository toolRepository;
    private final AgentIdGenerator agentIdGenerator;
    private final AgentToolIdGenerator agentToolIdGenerator;
    private final AgentMapper agentMapper;
    private final ModelConfigService modelConfigService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AgentResponse> findAll() {
        return agentRepository.findAll().stream()
                .map(this::toResponseWithTools)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AgentResponse> findById(String id) {
        return agentRepository.findById(id)
                .map(this::toResponseWithTools);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agent> findEntityById(String id) {
        return agentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentResponse> findByFilters(AgentStatus status, String modelId, String keyword) {
        return agentRepository.findByFilters(status, modelId, keyword).stream()
                .map(this::toResponseWithTools)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AgentResponse create(CreateAgentRequest request) {
        // 检查名称唯一性
        if (agentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Agent name already exists: " + request.getName());
        }

        // 验证模型存在
        if (!modelConfigService.findEntityById(request.getModelId()).isPresent()) {
            throw new IllegalArgumentException("Model not found: " + request.getModelId());
        }

        // 创建 Agent 实体
        Agent entity = agentMapper.toEntity(request);
        entity.setId(agentIdGenerator.generateId());
        entity.setStatus(AgentStatus.DRAFT);

        Agent saved = agentRepository.save(entity);
        log.info("Created agent: {}", saved.getId());

        // 绑定工具（如果有）
        List<ToolBindingResponse> tools = List.of();
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            tools = bindToolsInternal(saved.getId(), request.getTools());
        }

        return toResponseWithTools(saved, tools);
    }

    @Override
    @Transactional
    public AgentResponse update(String id, UpdateAgentRequest request) {
        Agent existing = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));

        // 检查名称唯一性（如果修改了名称）
        if (!existing.getName().equals(request.getName()) &&
                agentRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new IllegalArgumentException("Agent name already exists: " + request.getName());
        }

        // 验证模型存在
        if (!modelConfigService.findEntityById(request.getModelId()).isPresent()) {
            throw new IllegalArgumentException("Model not found: " + request.getModelId());
        }

        // 更新实体
        agentMapper.updateEntityFromRequest(request, existing);

        Agent saved = agentRepository.save(existing);
        log.info("Updated agent: {}", saved.getId());

        // 获取已绑定的工具
        List<ToolBindingResponse> tools = getAgentTools(saved.getId());

        return toResponseWithTools(saved, tools);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));

        // 删除所有工具绑定
        agentToolRepository.deleteByAgentId(id);

        // 删除 Agent
        agentRepository.deleteById(id);
        log.info("Deleted agent: {}", id);
    }

    @Override
    @Transactional
    public AgentResponse updateStatus(String id, AgentStatus status) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));

        // 状态转换校验
        validateStatusTransition(agent.getStatus(), status);

        agent.setStatus(status);
        Agent saved = agentRepository.save(agent);
        log.info("Updated agent {} status to {}", id, status);

        List<ToolBindingResponse> tools = getAgentTools(saved.getId());
        return toResponseWithTools(saved, tools);
    }

    @Override
    @Transactional
    public ToolBindingResponse bindTool(String agentId, ToolBindingRequest request) {
        // 验证 Agent 存在
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        // 验证工具存在
        Tool tool = toolRepository.findById(request.getToolId())
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + request.getToolId()));

        // 检查是否已绑定
        if (agentToolRepository.existsByAgentIdAndToolId(agentId, request.getToolId())) {
            throw new IllegalArgumentException("Tool already bound to agent: " + request.getToolId());
        }

        // 创建绑定
        AgentTool agentTool = AgentTool.builder()
                .id(agentToolIdGenerator.generateId())
                .agentId(agentId)
                .toolId(request.getToolId())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .config(mapConfigToString(request.getConfig()))
                .build();

        AgentTool saved = agentToolRepository.save(agentTool);
        log.info("Bound tool {} to agent {}", request.getToolId(), agentId);

        return agentMapper.toToolBindingResponse(saved, tool);
    }

    @Override
    @Transactional
    public void unbindTool(String agentId, String toolId) {
        if (!agentToolRepository.existsByAgentIdAndToolId(agentId, toolId)) {
            throw new IllegalArgumentException("Tool binding not found");
        }

        agentToolRepository.deleteByAgentIdAndToolId(agentId, toolId);
        log.info("Unbound tool {} from agent {}", toolId, agentId);
    }

    @Override
    @Transactional
    public ToolBindingResponse updateToolBinding(String agentId, String toolId, ToolBindingRequest request) {
        AgentTool agentTool = agentToolRepository.findByAgentIdAndToolId(agentId, toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool binding not found"));

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));

        agentTool.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        agentTool.setConfig(mapConfigToString(request.getConfig()));

        AgentTool saved = agentToolRepository.save(agentTool);
        log.info("Updated tool binding {} for agent {}", toolId, agentId);

        return agentMapper.toToolBindingResponse(saved, tool);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ToolBindingResponse> getAgentTools(String agentId) {
        List<AgentTool> agentTools = agentToolRepository.findByAgentId(agentId);
        return agentTools.stream()
                .map(at -> {
                    Tool tool = toolRepository.findById(at.getToolId()).orElse(null);
                    if (tool != null) {
                        return agentMapper.toToolBindingResponse(at, tool);
                    }
                    return null;
                })
                .filter(t -> t != null)
                .collect(Collectors.toList());
    }

    /**
     * 内部绑定工具方法（用于创建 Agent 时批量绑定）
     */
    private List<ToolBindingResponse> bindToolsInternal(String agentId, List<ToolBindingRequest> requests) {
        return requests.stream()
                .map(request -> {
                    Tool tool = toolRepository.findById(request.getToolId())
                            .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + request.getToolId()));

                    AgentTool agentTool = AgentTool.builder()
                            .id(agentToolIdGenerator.generateId())
                            .agentId(agentId)
                            .toolId(request.getToolId())
                            .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                            .config(mapConfigToString(request.getConfig()))
                            .build();

                    AgentTool saved = agentToolRepository.save(agentTool);
                    return agentMapper.toToolBindingResponse(saved, tool);
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换为响应 DTO（含工具绑定）
     */
    private AgentResponse toResponseWithTools(Agent agent) {
        List<ToolBindingResponse> tools = getAgentTools(agent.getId());
        return toResponseWithTools(agent, tools);
    }

    /**
     * 转换为响应 DTO（含工具绑定）
     */
    private AgentResponse toResponseWithTools(Agent agent, List<ToolBindingResponse> tools) {
        String modelName = modelConfigService.findById(agent.getModelId())
                .map(ModelResponse::getName)
                .orElse(null);

        return agentMapper.toResponseWithTools(agent, tools, modelName);
    }

    /**
     * 校验状态转换是否合法
     * <p>
     * 允许的状态转换：
     * <ul>
     *   <li>DRAFT → PUBLISHED（发布）</li>
     *   <li>PUBLISHED → ARCHIVED（归档）</li>
     *   <li>ARCHIVED → DRAFT（恢复到草稿）</li>
     * </ul>
     * 禁止的转换：
     * <ul>
     *   <li>ARCHIVED → PUBLISHED（归档后不能直接发布，需先恢复到草稿）</li>
     * </ul>
     * </p>
     */
    private void validateStatusTransition(AgentStatus current, AgentStatus target) {
        // ARCHIVED 状态不能直接转换到 PUBLISHED（需先恢复到 DRAFT）
        if (current == AgentStatus.ARCHIVED && target == AgentStatus.PUBLISHED) {
            throw new IllegalArgumentException("Cannot change status from ARCHIVED to PUBLISHED directly. Restore to DRAFT first.");
        }
        // 允许其他转换（包括 ARCHIVED → DRAFT）
    }

    /**
     * 将配置 Map 序列化为 JSON 字符串
     */
    private String mapConfigToString(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize config", e);
        }
    }
}