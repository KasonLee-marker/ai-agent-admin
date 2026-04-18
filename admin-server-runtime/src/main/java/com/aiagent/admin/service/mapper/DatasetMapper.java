package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.DatasetCreateRequest;
import com.aiagent.admin.api.dto.DatasetResponse;
import com.aiagent.admin.api.dto.DatasetUpdateRequest;
import com.aiagent.admin.domain.entity.Dataset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 数据集实体与 DTO 之间的映射器
 * <p>
 * 使用 MapStruct 实现数据集（Dataset）与其相关 DTO 之间的自动转换。
 * </p>
 *
 * @see Dataset
 * @see DatasetResponse
 * @see DatasetCreateRequest
 * @see DatasetUpdateRequest
 */
@Mapper(componentModel = "spring")
public interface DatasetMapper {

    /**
     * 将实体转换为响应 DTO
     *
     * @param dataset 数据集实体
     * @return 响应 DTO
     */
    DatasetResponse toResponse(Dataset dataset);

    /**
     * 将实体列表转换为响应 DTO 列表
     *
     * @param datasets 数据集实体列表
     * @return 响应 DTO 列表
     */
    List<DatasetResponse> toResponseList(List<Dataset> datasets);

    /**
     * 将创建请求 DTO 转换为实体
     * <p>
     * 忽略 id 字段，版本默认设置为 1，状态默认设置为 DRAFT，数据项数量默认设置为 0。
     * </p>
     *
     * @param request 创建请求 DTO
     * @return 数据集实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", constant = "1")
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "itemCount", constant = "0")
    @Mapping(target = "sourcePath", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Dataset toEntity(DatasetCreateRequest request);

    /**
     * 使用更新请求 DTO 更新实体
     * <p>
     * 仅更新请求中包含的字段，忽略 id、version、status、itemCount、sourceType、sourcePath、时间戳、创建人字段。
     * </p>
     *
     * @param dataset 待更新的实体
     * @param request 更新请求 DTO
     */
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
