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

@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetServiceImpl implements DatasetService {

    private final DatasetRepository datasetRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final DatasetMapper datasetMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final ObjectMapper objectMapper;

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

    @Override
    @Transactional
    public void deleteDataset(String id) {
        Dataset dataset = datasetRepository.findByIdAndStatusNot(id, Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + id));

        dataset.setStatus(Dataset.DatasetStatus.DELETED);
        datasetRepository.save(dataset);

        datasetItemRepository.updateStatusByDatasetId(id, DatasetItem.ItemStatus.DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public DatasetResponse getDataset(String id) {
        Dataset entity = datasetRepository.findByIdAndStatusNot(id, Dataset.DatasetStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + id));
        return datasetMapper.toResponse(entity);
    }

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

    @Override
    @Transactional
    public DatasetItemResponse updateDatasetItem(String id, DatasetItemUpdateRequest request) {
        DatasetItem existing = datasetItemRepository.findByIdAndStatusNot(id, DatasetItem.ItemStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset item not found with id: " + id));

        datasetItemMapper.updateEntity(existing, request);
        DatasetItem updated = datasetItemRepository.save(existing);
        return datasetItemMapper.toResponse(updated);
    }

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

    @Override
    @Transactional(readOnly = true)
    public DatasetItemResponse getDatasetItem(String id) {
        DatasetItem entity = datasetItemRepository.findByIdAndStatusNot(id, DatasetItem.ItemStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Dataset item not found with id: " + id));
        return datasetItemMapper.toResponse(entity);
    }

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

    @lombok.Data
    private static class ExportData {
        private String name;
        private String description;
        private String category;
        private String tags;
        private Integer version;
        private List<ExportItem> items;
    }

    @lombok.Data
    private static class ExportItem {
        private String input;
        private String output;
        private String metadata;
    }
}
