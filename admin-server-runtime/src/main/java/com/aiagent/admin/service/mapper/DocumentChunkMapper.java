package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.DocumentChunkResponse;
import com.aiagent.admin.domain.entity.DocumentChunk;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentChunkMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "documentId", source = "documentId")
    @Mapping(target = "chunkIndex", source = "chunkIndex")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "createdAt", source = "createdAt")
    DocumentChunkResponse toResponse(DocumentChunk chunk);
}