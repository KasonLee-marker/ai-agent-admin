package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Dataset;
import com.aiagent.admin.domain.entity.DatasetItem;
import com.aiagent.admin.domain.repository.DatasetItemRepository;
import com.aiagent.admin.domain.repository.DatasetRepository;
import com.aiagent.admin.service.DatasetService;
import com.aiagent.admin.service.mapper.DatasetItemMapper;
import com.aiagent.admin.service.mapper.DatasetMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据集服务实现类
 * <p>
 * 提供数据集和数据集项的核心管理功能：
 * <ul>
 *   <li>数据集创建、更新、删除、查询</li>
 *   <li>数据集项（测试数据）管理</li>
 *   <li>数据集导入导出（JSON/CSV 格式）</li>
 *   <li>版本管理（创建新版本时复制现有数据）</li>
 * </ul>
 * </p>
 * <p>
 * 数据集用于存储模型评估的测试数据，包含输入和预期输出。
 * 支持软删除，删除时标记状态而非物理删除。
 * </p>
 *
 * @see DatasetService
 * @see Dataset
 * @see DatasetItem
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetServiceImpl implements DatasetService {

    private final DatasetRepository datasetRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final DatasetMapper datasetMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建新的数据集
     * <p>
     * 检查名称唯一性后创建数据集实体。
     * 初始版本为 1，状态为活跃，数据项数量为 0。
     * </p>
     *
     * @param request 创建数据集请求，包含名称、描述、分类等
     * @return 创建成功的数据集响应 DTO
     * @throws IllegalArgumentException 数据集名称已存在时抛出
     */
    @Override
    @Transactional
    public DatasetResponse createDataset(DatasetCreateRequest request) {
        if (datasetRepository.existsByNameAndStatusNot(request.getName(), Dataset.DatasetStatus.DELETED)) {
            throw new IllegalArgumentException("Dataset with name '" + request.getName() + "' already exists");
        }

        Dataset entity = datasetMapper.toEntity(request);
        Dataset saved = datasetRepository.save(entity);
        return datasetMapper.toResponse(saved);
    }

    /**
     * 更新数据集元数据
     * <p>
     * 支持更新名称、描述、分类等属性。
     * 更新名称时检查唯一性。
     * </p>
     *
     * @param id      数据集唯一标识
     * @param request 更新请求
     * @return 更新后的数据集响应 DTO
     * @throws EntityNotFoundException  数据集不存在时抛出
     * @throws IllegalArgumentException 新名称已被占用时抛出
     */
    @Override
    @Transactional
    public DatasetResponse updateDataset(String id, DatasetUpdateRequest request) {
        Dataset existing = datasetRepository.findByIdAndStatusNot(id, Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(existing.getName())
                && datasetRepository.existsByNameAndStatusNot(request.getName(), Dataset.DatasetStatus.DELETED)) {
            throw new IllegalArgumentException("Dataset with name '" + request.getName() + "' already exists");
        }

        datasetMapper.updateEntity(existing, request);
        Dataset updated = datasetRepository.save(existing);
        return datasetMapper.toResponse(updated);
    }

    /**
     * 删除数据集（软删除）
     * <p>
     * 将数据集状态标记为 DELETED，同时将所有数据项状态也标记为 DELETED。
     * 数据不会被物理删除，仍可通过状态过滤查询。
     * </p>
     *
     * @param id 数据集唯一标识
     * @throws EntityNotFoundException 数据集不存在时抛出
     */
    @Override
    @Transactional
    public void deleteDataset(String id) {
        Dataset dataset = datasetRepository.findByIdAndStatusNot(id, Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + id));

        dataset.setStatus(Dataset.DatasetStatus.DELETED);
        datasetRepository.save(dataset);

        datasetItemRepository.updateStatusByDatasetId(id, DatasetItem.ItemStatus.DELETED);
    }

    /**
     * 根据ID获取数据集详情
     *
     * @param id 数据集唯一标识
     * @return 数据集响应 DTO
     * @throws EntityNotFoundException 数据集不存在或已删除时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public DatasetResponse getDataset(String id) {
        Dataset entity = datasetRepository.findByIdAndStatusNot(id, Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + id));
        return datasetMapper.toResponse(entity);
    }

    /**
     * 分页查询数据集列表
     * <p>
     * 支持按分类、关键词筛选，按更新时间倒序排列。
     * 自动排除已删除的数据集。
     * </p>
     *
     * @param category 分类过滤条件（可选）
     * @param keyword  搜索关键词（可选）
     * @param pageable 分页参数
     * @return 分页的数据集响应 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DatasetResponse> listDatasets(String category, String keyword, Pageable pageable) {
        Page<Dataset> page;
        if (keyword != null && !keyword.isEmpty()) {
            page = datasetRepository.searchByKeyword(keyword, Dataset.DatasetStatus.DELETED, pageable);
        } else if (category != null && !category.isEmpty()) {
            page = datasetRepository.findByCategoryAndStatusNot(category, Dataset.DatasetStatus.DELETED, pageable);
        } else {
            page = datasetRepository.findByStatusNot(Dataset.DatasetStatus.DELETED, pageable);
        }
        Page<DatasetResponse> responsePage = page.map(datasetMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    /**
     * 创建新的数据集项（测试数据）
     * <p>
     * 在当前版本下创建数据项，自动分配序号。
     * 更新数据集的数据项计数。
     * </p>
     *
     * @param request 创建数据项请求，包含数据集ID、输入、预期输出、元数据
     * @return 创建成功的数据项响应 DTO
     * @throws EntityNotFoundException 数据集不存在时抛出
     */
    @Override
    @Transactional
    public DatasetItemResponse createDatasetItem(DatasetItemCreateRequest request) {
        Dataset dataset = datasetRepository.findByIdAndStatusNot(request.getDatasetId(), Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + request.getDatasetId()));

        Integer currentVersion = dataset.getVersion();
        long itemCount = datasetItemRepository.countByDatasetIdAndVersionAndStatusNot(
                request.getDatasetId(), currentVersion, DatasetItem.ItemStatus.DELETED);

        DatasetItem entity = datasetItemMapper.toEntity(request);
        entity.setDatasetId(request.getDatasetId());
        entity.setVersion(currentVersion);
        entity.setSequence((int) itemCount + 1);

        DatasetItem saved = datasetItemRepository.save(entity);

        dataset.setItemCount((int) (itemCount + 1));
        datasetRepository.save(dataset);

        return datasetItemMapper.toResponse(saved);
    }

    /**
     * 更新数据集项内容
     *
     * @param id      数据项唯一标识
     * @param request 更新请求，包含新的输入、输出、元数据
     * @return 更新后的数据项响应 DTO
     * @throws EntityNotFoundException 数据项不存在时抛出
     */
    @Override
    @Transactional
    public DatasetItemResponse updateDatasetItem(String id, DatasetItemUpdateRequest request) {
        DatasetItem existing = datasetItemRepository.findByIdAndStatusNot(id, DatasetItem.ItemStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset item not found with id: " + id));

        datasetItemMapper.updateEntity(existing, request);
        DatasetItem updated = datasetItemRepository.save(existing);
        return datasetItemMapper.toResponse(updated);
    }

    /**
     * 删除数据集项（软删除）
     * <p>
     * 将数据项状态标记为 DELETED，更新数据集的数据项计数。
     * </p>
     *
     * @param id 数据项唯一标识
     * @throws EntityNotFoundException 数据项或所属数据集不存在时抛出
     */
    @Override
    @Transactional
    public void deleteDatasetItem(String id) {
        DatasetItem item = datasetItemRepository.findByIdAndStatusNot(id, DatasetItem.ItemStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset item not found with id: " + id));

        item.setStatus(DatasetItem.ItemStatus.DELETED);
        datasetItemRepository.save(item);

        Dataset dataset = datasetRepository.findByIdAndStatusNot(item.getDatasetId(), Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + item.getDatasetId()));

        long itemCount = datasetItemRepository.countByDatasetIdAndStatusNot(item.getDatasetId(), DatasetItem.ItemStatus.DELETED);
        dataset.setItemCount((int) itemCount);
        datasetRepository.save(dataset);
    }

    /**
     * 根据ID获取数据集项详情
     *
     * @param id 数据项唯一标识
     * @return 数据项响应 DTO
     * @throws EntityNotFoundException 数据项不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public DatasetItemResponse getDatasetItem(String id) {
        DatasetItem entity = datasetItemRepository.findByIdAndStatusNot(id, DatasetItem.ItemStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset item not found with id: " + id));
        return datasetItemMapper.toResponse(entity);
    }

    /**
     * 分页查询数据集的数据项列表
     * <p>
     * 按序号升序排列，返回当前数据集的所有数据项。
     * </p>
     *
     * @param datasetId 数据集唯一标识
     * @param pageable  分页参数
     * @return 分页的数据项响应 DTO
     * @throws EntityNotFoundException 数据集不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DatasetItemResponse> listDatasetItems(String datasetId, Pageable pageable) {
        if (!datasetRepository.existsById(datasetId)) {
            throw new EntityNotFoundException("Dataset not found with id: " + datasetId);
        }

        Page<DatasetItem> page = datasetItemRepository.findByDatasetIdAndStatusNot(
                datasetId, DatasetItem.ItemStatus.DELETED, pageable);
        Page<DatasetItemResponse> responsePage = page.map(datasetItemMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    /**
     * 查询指定版本的所有数据项
     * <p>
     * 返回指定版本下的所有数据项，用于版本比较或历史查看。
     * 如果未指定版本，使用当前最新版本。
     * </p>
     *
     * @param datasetId 数据集唯一标识
     * @param version   版本号（可选，默认为当前版本）
     * @return 数据项响应 DTO 列表
     * @throws EntityNotFoundException 数据集不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public List<DatasetItemResponse> listDatasetItemsByVersion(String datasetId, Integer version) {
        if (!datasetRepository.existsById(datasetId)) {
            throw new EntityNotFoundException("Dataset not found with id: " + datasetId);
        }

        List<DatasetItem> items = datasetItemRepository.findByDatasetIdAndVersionAndStatusNot(
                datasetId, version, DatasetItem.ItemStatus.DELETED);
        return datasetItemMapper.toResponseList(items);
    }

    /**
     * 导入数据集（包含数据项）
     * <p>
     * 从外部数据导入创建新的数据集，支持批量导入数据项。
     * 导入的数据项自动分配序号，版本初始化为 1。
     * </p>
     *
     * @param request 导入请求，包含数据集名称、描述和数据项列表
     * @return 导入成功的数据集响应 DTO
     * @throws IllegalArgumentException 数据集名称已存在时抛出
     */
    @Override
    @Transactional
    public DatasetResponse importDataset(DatasetImportRequest request) {
        if (datasetRepository.existsByNameAndStatusNot(request.getName(), Dataset.DatasetStatus.DELETED)) {
            throw new IllegalArgumentException("Dataset with name '" + request.getName() + "' already exists");
        }

        Dataset dataset = Dataset.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .version(1)
                .status(Dataset.DatasetStatus.ACTIVE)
                .itemCount(0)
                .sourceType("IMPORT")
                .build();
        Dataset savedDataset = datasetRepository.save(dataset);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<DatasetItem> items = new ArrayList<>();
            int sequence = 1;
            for (DatasetImportRequest.DatasetItemImportData itemData : request.getItems()) {
                DatasetItem item = datasetItemMapper.importDataToEntity(itemData);
                item.setDatasetId(savedDataset.getId());
                item.setVersion(1);
                item.setSequence(sequence++);
                items.add(item);
            }
            datasetItemRepository.saveAll(items);

            savedDataset.setItemCount(items.size());
            savedDataset = datasetRepository.save(savedDataset);
        }

        return datasetMapper.toResponse(savedDataset);
    }

    /**
     * 向现有数据集导入数据项
     * <p>
     * 在当前版本下批量添加数据项，自动分配序号。
     * 更新数据集的数据项计数。
     * </p>
     *
     * @param datasetId 数据集唯一标识
     * @param items     要导入的数据项列表
     * @return 导入成功的数据项响应 DTO 列表
     * @throws EntityNotFoundException 数据集不存在时抛出
     */
    @Override
    @Transactional
    public List<DatasetItemResponse> importItemsToDataset(String datasetId, List<DatasetImportRequest.DatasetItemImportData> items) {
        Dataset dataset = datasetRepository.findByIdAndStatusNot(datasetId, Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + datasetId));

        Integer currentVersion = dataset.getVersion();
        long existingCount = datasetItemRepository.countByDatasetIdAndVersionAndStatusNot(
                datasetId, currentVersion, DatasetItem.ItemStatus.DELETED);

        List<DatasetItem> newItems = new ArrayList<>();
        int sequence = (int) existingCount + 1;
        for (DatasetImportRequest.DatasetItemImportData itemData : items) {
            DatasetItem item = datasetItemMapper.importDataToEntity(itemData);
            item.setDatasetId(datasetId);
            item.setVersion(currentVersion);
            item.setSequence(sequence++);
            newItems.add(item);
        }

        List<DatasetItem> savedItems = datasetItemRepository.saveAll(newItems);

        long totalCount = datasetItemRepository.countByDatasetIdAndStatusNot(datasetId, DatasetItem.ItemStatus.DELETED);
        dataset.setItemCount((int) totalCount);
        datasetRepository.save(dataset);

        return datasetItemMapper.toResponseList(savedItems);
    }

    /**
     * 创建数据集新版本
     * <p>
     * 创建新版本时：
     * <ol>
     *   <li>递增版本号</li>
     *   <li>复制当前版本的所有数据项到新版本</li>
     * </ol>
     * 这允许保留历史版本数据，支持版本对比和回滚。
     * </p>
     *
     * @param datasetId 数据集唯一标识
     * @param request   版本创建请求（可选变更日志）
     * @return 更新后的数据集响应 DTO
     * @throws EntityNotFoundException 数据集不存在时抛出
     */
    @Override
    @Transactional
    public DatasetResponse createNewVersion(String datasetId, DatasetVersionCreateRequest request) {
        Dataset dataset = datasetRepository.findByIdAndStatusNot(datasetId, Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + datasetId));

        Integer newVersion = dataset.getVersion() + 1;
        dataset.setVersion(newVersion);
        dataset.setStatus(Dataset.DatasetStatus.ACTIVE);
        Dataset updated = datasetRepository.save(dataset);

        List<DatasetItem> currentItems = datasetItemRepository.findByDatasetIdAndVersionAndStatusNot(
                datasetId, newVersion - 1, DatasetItem.ItemStatus.DELETED);

        if (!currentItems.isEmpty()) {
            List<DatasetItem> newVersionItems = new ArrayList<>();
            for (DatasetItem item : currentItems) {
                DatasetItem newItem = DatasetItem.builder()
                        .datasetId(item.getDatasetId())
                        .version(newVersion)
                        .sequence(item.getSequence())
                        .input(item.getInput())
                        .output(item.getOutput())
                        .metadata(item.getMetadata())
                        .status(DatasetItem.ItemStatus.ACTIVE)
                        .build();
                newVersionItems.add(newItem);
            }
            datasetItemRepository.saveAll(newVersionItems);
        }

        return datasetMapper.toResponse(updated);
    }

    /**
     * 导出数据集为 JSON 格式
     * <p>
     * 导出当前版本的所有数据项，包含数据集元数据和数据项列表。
     * 格式化输出便于阅读。
     * </p>
     *
     * @param datasetId 数据集唯一标识
     * @return JSON 格式的字节数组
     * @throws EntityNotFoundException 数据集不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportDatasetAsJson(String datasetId) {
        Dataset dataset = datasetRepository.findByIdAndStatusNot(datasetId, Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + datasetId));

        List<DatasetItem> items = datasetItemRepository.findByDatasetIdAndVersionAndStatusNot(
                datasetId, dataset.getVersion(), DatasetItem.ItemStatus.DELETED);

        ExportData exportData = new ExportData();
        exportData.setName(dataset.getName());
        exportData.setDescription(dataset.getDescription());
        exportData.setCategory(dataset.getCategory());
        exportData.setTags(dataset.getTags());
        exportData.setVersion(dataset.getVersion());
        exportData.setItems(items.stream().map(item -> {
            ExportItem exportItem = new ExportItem();
            exportItem.setInput(item.getInput());
            exportItem.setOutput(item.getOutput());
            exportItem.setMetadata(item.getMetadata());
            return exportItem;
        }).collect(Collectors.toList()));

        try {
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(exportData).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to export dataset as JSON", e);
        }
    }

    /**
     * 导出数据集为 CSV 格式
     * <p>
     * 导出当前版本的所有数据项，格式：input,output,metadata。
     * 支持字段内逗号、换行符和引号的转义。
     * </p>
     *
     * @param datasetId 数据集唯一标识
     * @return CSV 格式的字节数组
     * @throws EntityNotFoundException 数据集不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportDatasetAsCsv(String datasetId) {
        Dataset dataset = datasetRepository.findByIdAndStatusNot(datasetId, Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + datasetId));

        List<DatasetItem> items = datasetItemRepository.findByDatasetIdAndVersionAndStatusNot(
                datasetId, dataset.getVersion(), DatasetItem.ItemStatus.DELETED);

        StringBuilder csv = new StringBuilder();
        csv.append("input,output,metadata\n");

        for (DatasetItem item : items) {
            csv.append(escapeCsv(item.getInput())).append(",")
               .append(escapeCsv(item.getOutput())).append(",")
               .append(escapeCsv(item.getMetadata())).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * CSV 字段值转义
     * <p>
     * 处理字段值中的特殊字符：
     * <ul>
     *   <li>引号转义为双引号</li>
     *   <li>包含逗号、换行符或引号的字段用引号包围</li>
     * </ul>
     * </p>
     *
     * @param value 原始字段值
     * @return 转义后的字段值
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            escaped = "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * 数据集导出数据结构（JSON 导出用）
     */
    @lombok.Data
    private static class ExportData {
        private String name;
        private String description;
        private String category;
        private String tags;
        private Integer version;
        private List<ExportItem> items;
    }

    /**
     * 数据集项导出数据结构（JSON 导出用）
     */
    @lombok.Data
    private static class ExportItem {
        private String input;
        private String output;
        private String metadata;
    }
}
