package com.aiagent.admin.service.mapper;

import com.aiagent.admin.api.dto.DatasetImportRequest;
import com.aiagent.admin.api.dto.DatasetItemCreateRequest;
import com.aiagent.admin.api.dto.DatasetItemResponse;
import com.aiagent.admin.api.dto.DatasetItemUpdateRequest;
import com.aiagent.admin.domain.entity.DatasetItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 数据集项实体与 DTO 之间的映射器
 * <p>
 * 使用 MapStruct 实现数据集项（DatasetItem）与其相关 DTO 之间的自动转换。
 * </p>
 *
 * @see DatasetItem
 * @see DatasetItemResponse
 * @see DatasetItemCreateRequest
 * @see DatasetItemUpdateRequest
 */
@Mapper(componentModel = "spring")
public interface DatasetItemMapper {

    /**
     * 将实体转换为响应 DTO
     *
     * @param item 数据集项实体
     * @return 响应 DTO
     */
    DatasetItemResponse toResponse(DatasetItem item);

    /**
     * 将实体列表转换为响应 DTO 列表
     *
     * @param items 数据集项实体列表
     * @return 响应 DTO 列表
     */
    List<DatasetItemResponse> toResponseList(List<DatasetItem> items);

    /**
     * 将创建请求 DTO 转换为实体
     * <p>
     * 忽略 id、version、sequence 字段，状态默认设置为 ACTIVE。
     * </p>
     *
     * @param request 创建请求 DTO
     * @return 数据集项实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sequence", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DatasetItem toEntity(DatasetItemCreateRequest request);

    /**
     * 使用更新请求 DTO 更新实体
     * <p>
     * 仅更新请求中包含的字段，忽略 id、datasetId、version、sequence、status、时间戳字段。
     * </p>
     *
     * @param item    待更新的实体
     * @param request 更新请求 DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "datasetId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sequence", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget DatasetItem item, DatasetItemUpdateRequest request);

    /**
     * 将导入数据转换为实体
     * <p>
     * 用于数据集导入功能，状态默认设置为 ACTIVE。
     * </p>
     *
     * @param data 导入数据
     * @return 数据集项实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "datasetId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sequence", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DatasetItem importDataToEntity(DatasetImportRequest.DatasetItemImportData data);
}
