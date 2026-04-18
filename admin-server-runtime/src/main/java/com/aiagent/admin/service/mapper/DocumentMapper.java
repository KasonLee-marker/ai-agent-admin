package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.domain.entity.Document;
import org.mapstruct.Mapper;

/**
 * 文档实体与 DTO 之间的映射器
 * <p>
 * 使用 MapStruct 自动映射同名字段，实现文档（Document）与响应 DTO 之间的转换。
 * </p>
 *
 * @see Document
 * @see DocumentResponse
 */
@Mapper(componentModel = "spring")
public interface DocumentMapper {

    /**
     * 将实体转换为响应 DTO
     *
     * @param document 文档实体
     * @return 响应 DTO
     */
    DocumentResponse toResponse(Document document);
}