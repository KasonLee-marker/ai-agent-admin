package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.DocumentChunkResponse;
import com.aiagent.admin.domain.entity.DocumentChunk;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 文档分块实体与 DTO 之间的映射器
 * <p>
 * 使用 MapStruct 实现文档分块（DocumentChunk）与响应 DTO 之间的自动转换。
 * </p>
 *
 * @see DocumentChunk
 * @see DocumentChunkResponse
 */
@Mapper(componentModel = "spring")
public interface DocumentChunkMapper {

    /**
     * 将实体转换为响应 DTO
     *
     * @param chunk 文档分块实体
     * @return 响应 DTO
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "documentId", source = "documentId")
    @Mapping(target = "chunkIndex", source = "chunkIndex")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "createdAt", source = "createdAt")
    DocumentChunkResponse toResponse(DocumentChunk chunk);
}