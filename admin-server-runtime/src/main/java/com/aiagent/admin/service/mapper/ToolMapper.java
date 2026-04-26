package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.CreateToolRequest;
import com.aiagent.admin.api.dto.ToolResponse;
import com.aiagent.admin.api.dto.UpdateToolRequest;
import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.ToolCategory;
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
 * Tool 实体与 DTO 之间的映射器
 * <p>
 * 使用 MapStruct 实现 Tool 实体与其相关 DTO 之间的自动转换。
 * 处理特殊字段：schema 和 config JSON 序列化/反序列化。
 * </p>
 *
 * @see Tool
 * @see ToolResponse
 * @see CreateToolRequest
 * @see UpdateToolRequest
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ToolMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将创建请求 DTO 转换为实体
     * <p>
     * 仅用于 CUSTOM 类型工具创建。
     * </p>
     *
     * @param request 创建请求 DTO
     * @return Tool 实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", constant = "CUSTOM")
    @Mapping(target = "executor", expression = "java(request.getEndpoint())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "schema", expression = "java(mapToJson(request.getSchema()))")
    @Mapping(target = "config", expression = "java(mapCustomToolConfig(request))")
    @Mapping(target = "category", expression = "java(parseCategory(request.getCategory()))")
    Tool toEntity(CreateToolRequest request);

    /**
     * 使用更新请求 DTO 更新实体
     *
     * @param request 更新请求 DTO
     * @param entity  待更新的实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "executor", expression = "java(request.getEndpoint())")
    @Mapping(target = "schema", expression = "java(mapToJson(request.getSchema()))")
    @Mapping(target = "config", expression = "java(mapCustomToolConfig(request))")
    @Mapping(target = "category", expression = "java(parseCategory(request.getCategory()))")
    void updateEntityFromRequest(UpdateToolRequest request, @MappingTarget Tool entity);

    /**
     * 将实体转换为响应 DTO
     *
     * @param entity Tool 实体
     * @return Tool 响应 DTO
     */
    @Mapping(target = "schema", expression = "java(mapToMap(entity.getSchema()))")
    @Mapping(target = "config", expression = "java(mapToMap(entity.getConfig()))")
    ToolResponse toResponse(Tool entity);

    /**
     * 将实体列表转换为响应 DTO 列表
     *
     * @param entities Tool 实体列表
     * @return Tool 响应 DTO 列表
     */
    List<ToolResponse> toResponseList(List<Tool> entities);

    /**
     * 解析工具类别
     *
     * @param category 类别字符串
     * @return ToolCategory 枚举
     */
    default ToolCategory parseCategory(String category) {
        if (category == null || category.isEmpty()) {
            return ToolCategory.HTTP;
        }
        try {
            return ToolCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ToolCategory.HTTP;
        }
    }

    /**
     * 将 Map 序列化为 JSON 字符串
     *
     * @param map Map 对象
     * @return JSON 字符串
     */
    default String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Map
     *
     * @param json JSON 字符串
     * @return Map 对象
     */
    default Map<String, Object> mapToMap(String json) {
        if (json == null || json.isEmpty()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    /**
     * 构建 CUSTOM 工具配置
     *
     * @param request 创建请求
     * @return 配置 JSON 字符串
     */
    default String mapCustomToolConfig(CreateToolRequest request) {
        Map<String, Object> config = new java.util.HashMap<>();
        if (request.getAuthType() != null) {
            config.put("authType", request.getAuthType());
        }
        if (request.getAuthConfig() != null) {
            config.put("authConfig", request.getAuthConfig());
        }
        if (request.getRequestTemplate() != null) {
            config.put("requestTemplate", request.getRequestTemplate());
        }
        if (request.getResponseMapping() != null) {
            config.put("responseMapping", request.getResponseMapping());
        }
        return mapToJson(config);
    }

    /**
     * 构建 CUSTOM 工具配置（更新请求）
     *
     * @param request 更新请求
     * @return 配置 JSON 字符串
     */
    default String mapCustomToolConfig(UpdateToolRequest request) {
        Map<String, Object> config = new java.util.HashMap<>();
        if (request.getAuthType() != null) {
            config.put("authType", request.getAuthType());
        }
        if (request.getAuthConfig() != null) {
            config.put("authConfig", request.getAuthConfig());
        }
        if (request.getRequestTemplate() != null) {
            config.put("requestTemplate", request.getRequestTemplate());
        }
        if (request.getResponseMapping() != null) {
            config.put("responseMapping", request.getResponseMapping());
        }
        return mapToJson(config);
    }
}