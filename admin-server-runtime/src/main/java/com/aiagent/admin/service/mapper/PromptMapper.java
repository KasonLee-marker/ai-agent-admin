package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.PromptTemplateCreateRequest;
import com.aiagent.admin.api.dto.PromptTemplateResponse;
import com.aiagent.admin.api.dto.PromptVersionResponse;
import com.aiagent.admin.domain.entity.PromptTemplate;
import com.aiagent.admin.domain.entity.PromptVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Prompt 模板实体与 DTO 之间的映射器
 * <p>
 * 使用 MapStruct 实现 Prompt 模板（PromptTemplate）、版本（PromptVersion）
 * 与相关 DTO 之间的自动转换。处理标签和变量的字符串与列表之间的转换。
 * </p>
 *
 * @see PromptTemplate
 * @see PromptVersion
 * @see PromptTemplateResponse
 * @see PromptTemplateCreateRequest
 * @see PromptVersionResponse
 */
@Mapper(componentModel = "spring")
public interface PromptMapper {

    /**
     * 将 Prompt 模板实体转换为响应 DTO
     * <p>
     * 标签和变量字段从逗号分隔的字符串转换为列表。
     * </p>
     *
     * @param entity Prompt 模板实体
     * @return 响应 DTO
     */
    @Mapping(target = "tags", source = "tags", qualifiedByName = "stringToList")
    @Mapping(target = "variables", source = "variables", qualifiedByName = "stringToList")
    PromptTemplateResponse toResponse(PromptTemplate entity);

    /**
     * 将创建请求 DTO 转换为 Prompt 模板实体
     * <p>
     * 忽略 id 字段，版本默认设置为 1，标签从列表转换为逗号分隔的字符串。
     * </p>
     *
     * @param request 创建请求 DTO
     * @return Prompt 模板实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", constant = "1")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "listToString")
    @Mapping(target = "variables", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PromptTemplate toEntity(PromptTemplateCreateRequest request);

    /**
     * 将 Prompt 版本实体转换为响应 DTO
     *
     * @param entity Prompt 版本实体
     * @return 响应 DTO
     */
    PromptVersionResponse toVersionResponse(PromptVersion entity);

    /**
     * 将 Prompt 版本实体列表转换为响应 DTO 列表
     *
     * @param entities Prompt 版本实体列表
     * @return 响应 DTO 列表
     */
    List<PromptVersionResponse> toVersionResponseList(List<PromptVersion> entities);

    /**
     * 将逗号分隔的字符串转换为字符串列表
     *
     * @param value 逗号分隔的字符串
     * @return 字符串列表，如果为空则返回空列表
     */
    @Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(","));
    }

    /**
     * 将字符串列表转换为逗号分隔的字符串
     *
     * @param list 字符串列表
     * @return 逗号分隔的字符串，如果为空则返回空字符串
     */
    @Named("listToString")
    default String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}
