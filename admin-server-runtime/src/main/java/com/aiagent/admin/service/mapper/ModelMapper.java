package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.CreateModelRequest;
import com.aiagent.admin.api.dto.ModelResponse;
import com.aiagent.admin.api.dto.UpdateModelRequest;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.service.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;

import java.util.Map;

/**
 * 模型配置实体与 DTO 之间的映射器
 * <p>
 * 使用 MapStruct 实现模型配置（ModelConfig）与其相关 DTO 之间的自动转换。
 * 处理特殊字段：API Key 解密、额外参数 JSON 序列化/反序列化。
 * </p>
 *
 * @see ModelConfig
 * @see ModelResponse
 * @see CreateModelRequest
 * @see UpdateModelRequest
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ModelMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将创建请求 DTO 转换为实体
     * <p>
     * 忽略 id、isDefault、healthStatus、lastHealthCheck 字段，
     * 额外参数从 Map 序列化为 JSON 字符串。
     * </p>
     *
     * @param request 创建请求 DTO
     * @return 模型配置实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "healthStatus", ignore = true)
    @Mapping(target = "lastHealthCheck", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "extraParams", expression = "java(mapExtraParamsToString(request.getExtraParams()))")
    ModelConfig toEntity(CreateModelRequest request);

    /**
     * 使用更新请求 DTO 更新实体
     * <p>
     * 仅更新请求中包含的字段，忽略 id、isDefault、healthStatus、时间戳字段，
     * API Key 需单独处理（不在更新请求中）。
     * </p>
     *
     * @param request 更新请求 DTO
     * @param entity  待更新的实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "healthStatus", ignore = true)
    @Mapping(target = "lastHealthCheck", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "apiKey", ignore = true)
    @Mapping(target = "extraParams", expression = "java(mapExtraParamsToString(request.getExtraParams()))")
    void updateEntityFromRequest(UpdateModelRequest request, @MappingTarget ModelConfig entity);

    /**
     * 将实体转换为响应 DTO
     * <p>
     * 处理特殊字段：额外参数从 JSON 字符串反序列化为 Map，
     * API Key 解密，模型类型从枚举转换为字符串。
     * </p>
     *
     * @param entity            模型配置实体
     * @param encryptionService 加密服务（用于解密 API Key）
     * @return 响应 DTO
     */
    @Mapping(target = "extraParams", expression = "java(mapExtraParamsFromString(entity.getExtraParams()))")
    @Mapping(target = "modelType", expression = "java(entity.getProvider().getModelType().name())")
    @Mapping(target = "apiKey", expression = "java(decryptApiKey(entity.getApiKey(), encryptionService))")
    ModelResponse toResponse(ModelConfig entity, @Context EncryptionService encryptionService);

    /**
     * 解密 API Key
     *
     * @param apiKey            加密的 API Key
     * @param encryptionService 加密服务
     * @return 解密后的 API Key，如果为空则返回 null
     */
    default String decryptApiKey(String apiKey, EncryptionService encryptionService) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        return encryptionService.decrypt(apiKey);
    }

    /**
     * 将额外参数 Map 序列化为 JSON 字符串
     *
     * @param extraParams 额外参数 Map
     * @return JSON 字符串，如果为空则返回 null
     * @throws RuntimeException 序列化失败时抛出
     */
    default String mapExtraParamsToString(Map<String, Object> extraParams) {
        if (extraParams == null || extraParams.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(extraParams);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize extra params", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为额外参数 Map
     *
     * @param extraParams JSON 字符串
     * @return 额外参数 Map，如果为空则返回 null
     * @throws RuntimeException 反序列化失败时抛出
     */
    default Map<String, Object> mapExtraParamsFromString(String extraParams) {
        if (extraParams == null || extraParams.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(extraParams, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize extra params", e);
        }
    }
}
