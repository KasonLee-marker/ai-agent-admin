package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Dataset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DatasetMapper {

    DatasetResponse toResponse(Dataset dataset);

    List<DatasetResponse> toResponseList(List<Dataset> datasets);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", constant = "1")
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "itemCount", constant = "0")
    @Mapping(target = "sourcePath", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Dataset toEntity(DatasetCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "itemCount", ignore = true)
    @Mapping(target = "sourceType", ignore = true)
    @Mapping(target = "sourcePath", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(@MappingTarget Dataset dataset, DatasetUpdateRequest request);
}
