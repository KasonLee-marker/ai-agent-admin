package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Agent;
import com.aiagent.admin.domain.entity.AgentTool;
import com.aiagent.admin.domain.entity.Tool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;

/**
 * Agent 实体与 DTO 之间的映射器
 * <p>
 * 使用 MapStruct 实现 Agent 实体与其相关 DTO 之间的自动转换。
 * 处理特殊字段：config JSON 序列化/反序列化。
 * </p>
 *
 * @see Agent
 * @see AgentResponse
 * @see CreateAgentRequest
 * @see UpdateAgentRequest
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AgentMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将创建请求 DTO 转换为实体
     *
     * @param request 创建请求 DTO
     * @return Agent 实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "config", expression = "java(mapConfigToString(request.getConfig()))")
    Agent toEntity(CreateAgentRequest request);

    /**
     * 使用更新请求 DTO 更新实体
     *
     * @param request 更新请求 DTO
     * @param entity  待更新的实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "config", expression = "java(mapConfigToString(request.getConfig()))")
    void updateEntityFromRequest(UpdateAgentRequest request, @MappingTarget Agent entity);

    /**
     * 将实体转换为响应 DTO（不含工具绑定）
     *
     * @param entity Agent 实体
     * @return Agent 响应 DTO
     */
    @Mapping(target = "modelName", ignore = true)
    @Mapping(target = "tools", ignore = true)
    @Mapping(target = "config", expression = "java(mapConfigFromString(entity.getConfig()))")
    AgentResponse toResponse(Agent entity);

    /**
     * 将实体转换为响应 DTO（含工具绑定）
     *
     * @param entity    Agent 实体
     * @param tools     工具绑定响应列表
     * @param modelName 模型名称
     * @return Agent 响应 DTO
     */
    @Mapping(target = "tools", source = "tools")
    @Mapping(target = "modelName", source = "modelName")
    @Mapping(target = "config", expression = "java(mapConfigFromString(entity.getConfig()))")
    AgentResponse toResponseWithTools(Agent entity, List<ToolBindingResponse> tools, String modelName);

    /**
     * 将 AgentTool 实体转换为工具绑定响应 DTO
     *
     * @param agentTool AgentTool 实体
     * @param tool      工具实体
     * @return 工具绑定响应 DTO
     */
    @Mapping(target = "id", source = "agentTool.id")
    @Mapping(target = "toolId", source = "agentTool.toolId")
    @Mapping(target = "toolName", source = "tool.name")
    @Mapping(target = "toolDescription", source = "tool.description")
    @Mapping(target = "enabled", source = "agentTool.enabled")
    @Mapping(target = "config", expression = "java(mapConfigToMap(agentTool.getConfig()))")
    ToolBindingResponse toToolBindingResponse(AgentTool agentTool, Tool tool);

    /**
     * 将 AgentConfigRequest 序列化为 JSON 字符串
     *
     * @param config 配置请求 DTO
     * @return JSON 字符串
     */
    default String mapConfigToString(AgentConfigRequest config) {
        if (config == null) {
            return "{\"temperature\": 0.7, \"maxTokens\": 4096}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize agent config", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 AgentConfigResponse
     *
     * @param config JSON 字符串
     * @return Agent 配置响应 DTO
     */
    default AgentConfigResponse mapConfigFromString(String config) {
        if (config == null || config.isEmpty()) {
            return AgentConfigResponse.builder()
                    .temperature(0.7)
                    .maxTokens(4096)
                    .build();
        }
        try {
            return OBJECT_MAPPER.readValue(config, AgentConfigResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize agent config", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Map
     *
     * @param config JSON 字符串
     * @return Map
     */
    default Map<String, Object> mapConfigToMap(String config) {
        if (config == null || config.isEmpty()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(config, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize config to map", e);
        }
    }
}