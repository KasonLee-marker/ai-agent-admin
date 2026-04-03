package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.DatasetItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DatasetItemMapper {

    DatasetItemResponse toResponse(DatasetItem item);

    List<DatasetItemResponse> toResponseList(List<DatasetItem> items);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sequence", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DatasetItem toEntity(DatasetItemCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "datasetId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sequence", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget DatasetItem item, DatasetItemUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "datasetId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sequence", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DatasetItem importDataToEntity(DatasetImportRequest.DatasetItemImportData data);
}
