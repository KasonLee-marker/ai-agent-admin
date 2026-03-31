package com.aiagent.admin.service.mapper;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.api.dto.CreateModelRequest;
import com.aiagent.admin.api.dto.ModelResponse;
import com.aiagent.admin.api.dto.UpdateModelRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ModelMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "healthStatus", ignore = true)
    @Mapping(target = "lastHealthCheck", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "extraParams", expression = "java(mapExtraParamsToString(request.getExtraParams()))")
    ModelConfig toEntity(CreateModelRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "healthStatus", ignore = true)
    @Mapping(target = "lastHealthCheck", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "extraParams", expression = "java(mapExtraParamsToString(request.getExtraParams()))")
    void updateEntityFromRequest(UpdateModelRequest request, @MappingTarget ModelConfig entity);

    @Mapping(target = "extraParams", expression = "java(mapExtraParamsFromString(entity.getExtraParams()))")
    ModelResponse toResponse(ModelConfig entity);

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
