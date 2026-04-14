package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.domain.entity.Document;
import org.mapstruct.Mapper;

/**
 * 文档实体到响应 DTO 的映射器
 * <p>
 * 使用 MapStruct 自动映射同名字段。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentResponse toResponse(Document document);
}