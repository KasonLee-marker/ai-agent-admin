package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.domain.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "contentType", source = "contentType")
    @Mapping(target = "fileSize", source = "fileSize")
    @Mapping(target = "totalChunks", source = "totalChunks")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "createdBy", source = "createdBy")
    DocumentResponse toResponse(Document document);
}