package com.aiagent.admin.prompt.service.mapper;

import com.aiagent.admin.prompt.api.dto.*;
import com.aiagent.admin.prompt.domain.entity.PromptTemplate;
import com.aiagent.admin.prompt.domain.entity.PromptVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PromptMapper {

    @Mapping(target = "tags", source = "tags", qualifiedByName = "stringToList")
    @Mapping(target = "variables", source = "variables", qualifiedByName = "stringToList")
    PromptTemplateResponse toResponse(PromptTemplate entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", constant = "1")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "listToString")
    @Mapping(target = "variables", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PromptTemplate toEntity(PromptTemplateCreateRequest request);

    PromptVersionResponse toVersionResponse(PromptVersion entity);

    List<PromptVersionResponse> toVersionResponseList(List<PromptVersion> entities);

    @Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(","));
    }

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
