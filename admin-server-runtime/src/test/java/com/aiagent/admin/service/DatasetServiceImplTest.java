package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Dataset;
import com.aiagent.admin.domain.entity.DatasetItem;
import com.aiagent.admin.domain.repository.DatasetItemRepository;
import com.aiagent.admin.domain.repository.DatasetRepository;
import com.aiagent.admin.service.impl.DatasetServiceImpl;
import com.aiagent.admin.service.mapper.DatasetItemMapper;
import com.aiagent.admin.service.mapper.DatasetMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatasetServiceImplTest {

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private DatasetItemRepository datasetItemRepository;

    @Mock
    private DatasetMapper datasetMapper;

    @Mock
    private DatasetItemMapper datasetItemMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DatasetServiceImpl datasetService;

    private Dataset testDataset;
    private DatasetResponse testDatasetResponse;
    private DatasetItem testDatasetItem;
    private DatasetItemResponse testDatasetItemResponse;

    @BeforeEach
    void setUp() {
        testDataset = Dataset.builder()
                .id("dataset-id-123")
                .name("Test Dataset")
                .description("Test description")
                .category("test-category")
                .tags("tag1,tag2")
                .version(1)
                .status(Dataset.DatasetStatus.ACTIVE)
                .itemCount(2)
                .sourceType("IMPORT")
                .build();

        testDatasetResponse = new DatasetResponse();
        testDatasetResponse.setId("dataset-id-123");
        testDatasetResponse.setName("Test Dataset");
        testDatasetResponse.setDescription("Test description");
        testDatasetResponse.setCategory("test-category");
        testDatasetResponse.setTags("tag1,tag2");
        testDatasetResponse.setVersion(1);
        testDatasetResponse.setStatus("ACTIVE");
        testDatasetResponse.setItemCount(2);

        testDatasetItem = DatasetItem.builder()
                .id("item-id-123")
                .datasetId("dataset-id-123")
                .version(1)
                .sequence(1)
                .input("Test input")
                .output("Test output")
                .metadata("{}")
                .status(DatasetItem.ItemStatus.ACTIVE)
                .build();

        testDatasetItemResponse = new DatasetItemResponse();
        testDatasetItemResponse.setId("item-id-123");
        testDatasetItemResponse.setDatasetId("dataset-id-123");
        testDatasetItemResponse.setVersion(1);
        testDatasetItemResponse.setSequence(1);
        testDatasetItemResponse.setInput("Test input");
        testDatasetItemResponse.setOutput("Test output");
        testDatasetItemResponse.setMetadata("{}");
        testDatasetItemResponse.setStatus("ACTIVE");
    }

    @Test
    void createDataset_Success() {
        DatasetCreateRequest request = new DatasetCreateRequest();
        request.setName("New Dataset");
        request.setDescription("New description");
        request.setCategory("category");
        request.setTags("tag1");

        Dataset entity = Dataset.builder()
                .name("New Dataset")
                .description("New description")
                .build();

        when(datasetRepository.existsByNameAndStatusNot("New Dataset", Dataset.DatasetStatus.DELETED))
                .thenReturn(false);
        when(datasetMapper.toEntity(request)).thenReturn(entity);
        when(datasetRepository.save(any(Dataset.class))).thenReturn(testDataset);
        when(datasetMapper.toResponse(testDataset)).thenReturn(testDatasetResponse);

        DatasetResponse result = datasetService.createDataset(request);

        assertNotNull(result);
        assertEquals("dataset-id-123", result.getId());
        verify(datasetRepository).save(any(Dataset.class));
    }

    @Test
    void createDataset_DuplicateName_ThrowsException() {
        DatasetCreateRequest request = new DatasetCreateRequest();
        request.setName("Existing Dataset");

        when(datasetRepository.existsByNameAndStatusNot("Existing Dataset", Dataset.DatasetStatus.DELETED))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> datasetService.createDataset(request));
    }

    @Test
    void getDataset_Success() {
        when(datasetRepository.findByIdAndStatusNot("dataset-id-123", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.of(testDataset));
        when(datasetMapper.toResponse(testDataset)).thenReturn(testDatasetResponse);

        DatasetResponse result = datasetService.getDataset("dataset-id-123");

        assertNotNull(result);
        assertEquals("dataset-id-123", result.getId());
    }

    @Test
    void getDataset_NotFound_ThrowsException() {
        when(datasetRepository.findByIdAndStatusNot("non-existent", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> datasetService.getDataset("non-existent"));
    }

    @Test
    void updateDataset_Success() {
        DatasetUpdateRequest request = new DatasetUpdateRequest();
        request.setName("Updated Dataset");
        request.setDescription("Updated description");

        Dataset updatedDataset = Dataset.builder()
                .id("dataset-id-123")
                .name("Updated Dataset")
                .description("Updated description")
                .version(1)
                .build();

        when(datasetRepository.findByIdAndStatusNot("dataset-id-123", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.of(testDataset));
        when(datasetRepository.existsByNameAndStatusNot("Updated Dataset", Dataset.DatasetStatus.DELETED))
                .thenReturn(false);
        when(datasetRepository.save(any(Dataset.class))).thenReturn(updatedDataset);
        when(datasetMapper.toResponse(updatedDataset)).thenReturn(testDatasetResponse);

        DatasetResponse result = datasetService.updateDataset("dataset-id-123", request);

        assertNotNull(result);
        verify(datasetMapper).updateEntity(testDataset, request);
        verify(datasetRepository).save(testDataset);
    }

    @Test
    void deleteDataset_Success() {
        when(datasetRepository.findByIdAndStatusNot("dataset-id-123", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.of(testDataset));
        when(datasetRepository.save(any(Dataset.class))).thenReturn(testDataset);

        datasetService.deleteDataset("dataset-id-123");

        assertEquals(Dataset.DatasetStatus.DELETED, testDataset.getStatus());
        verify(datasetRepository).save(testDataset);
        verify(datasetItemRepository).updateStatusByDatasetId("dataset-id-123", DatasetItem.ItemStatus.DELETED);
    }

    @Test
    void listDatasets_WithKeyword_Success() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("updatedAt").descending());
        Page<Dataset> page = new PageImpl<>(Collections.singletonList(testDataset), pageable, 1);

        when(datasetRepository.searchByKeyword("test", Dataset.DatasetStatus.DELETED, pageable)).thenReturn(page);
        when(datasetMapper.toResponse(testDataset)).thenReturn(testDatasetResponse);

        PageResponse<DatasetResponse> result = datasetService.listDatasets(null, "test", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listDatasets_WithCategory_Success() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("updatedAt").descending());
        Page<Dataset> page = new PageImpl<>(Collections.singletonList(testDataset), pageable, 1);

        when(datasetRepository.findByCategoryAndStatusNot("test-category", Dataset.DatasetStatus.DELETED, pageable))
                .thenReturn(page);
        when(datasetMapper.toResponse(testDataset)).thenReturn(testDatasetResponse);

        PageResponse<DatasetResponse> result = datasetService.listDatasets("test-category", null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listDatasets_NoFilter_Success() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("updatedAt").descending());
        Page<Dataset> page = new PageImpl<>(Collections.singletonList(testDataset), pageable, 1);

        when(datasetRepository.findByStatusNot(Dataset.DatasetStatus.DELETED, pageable)).thenReturn(page);
        when(datasetMapper.toResponse(testDataset)).thenReturn(testDatasetResponse);

        PageResponse<DatasetResponse> result = datasetService.listDatasets(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void createDatasetItem_Success() {
        DatasetItemCreateRequest request = new DatasetItemCreateRequest();
        request.setDatasetId("dataset-id-123");
        request.setInput("New input");
        request.setOutput("New output");
        request.setMetadata("{}");

        DatasetItem entity = DatasetItem.builder()
                .input("New input")
                .output("New output")
                .build();

        when(datasetRepository.findByIdAndStatusNot("dataset-id-123", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.of(testDataset));
        when(datasetItemRepository.countByDatasetIdAndVersionAndStatusNot("dataset-id-123", 1, DatasetItem.ItemStatus.DELETED))
                .thenReturn(2L);
        when(datasetItemMapper.toEntity(request)).thenReturn(entity);
        when(datasetItemRepository.save(any(DatasetItem.class))).thenReturn(testDatasetItem);
        when(datasetItemMapper.toResponse(testDatasetItem)).thenReturn(testDatasetItemResponse);
        when(datasetRepository.save(any(Dataset.class))).thenReturn(testDataset);

        DatasetItemResponse result = datasetService.createDatasetItem(request);

        assertNotNull(result);
        assertEquals("item-id-123", result.getId());
        verify(datasetItemRepository).save(any(DatasetItem.class));
        verify(datasetRepository).save(testDataset);
    }

    @Test
    void getDatasetItem_Success() {
        when(datasetItemRepository.findByIdAndStatusNot("item-id-123", DatasetItem.ItemStatus.DELETED))
                .thenReturn(Optional.of(testDatasetItem));
        when(datasetItemMapper.toResponse(testDatasetItem)).thenReturn(testDatasetItemResponse);

        DatasetItemResponse result = datasetService.getDatasetItem("item-id-123");

        assertNotNull(result);
        assertEquals("item-id-123", result.getId());
    }

    @Test
    void updateDatasetItem_Success() {
        DatasetItemUpdateRequest request = new DatasetItemUpdateRequest();
        request.setInput("Updated input");
        request.setOutput("Updated output");

        DatasetItem updatedItem = DatasetItem.builder()
                .id("item-id-123")
                .input("Updated input")
                .output("Updated output")
                .build();

        when(datasetItemRepository.findByIdAndStatusNot("item-id-123", DatasetItem.ItemStatus.DELETED))
                .thenReturn(Optional.of(testDatasetItem));
        when(datasetItemRepository.save(any(DatasetItem.class))).thenReturn(updatedItem);
        when(datasetItemMapper.toResponse(updatedItem)).thenReturn(testDatasetItemResponse);

        DatasetItemResponse result = datasetService.updateDatasetItem("item-id-123", request);

        assertNotNull(result);
        verify(datasetItemMapper).updateEntity(testDatasetItem, request);
        verify(datasetItemRepository).save(testDatasetItem);
    }

    @Test
    void deleteDatasetItem_Success() {
        when(datasetItemRepository.findByIdAndStatusNot("item-id-123", DatasetItem.ItemStatus.DELETED))
                .thenReturn(Optional.of(testDatasetItem));
        when(datasetItemRepository.save(any(DatasetItem.class))).thenReturn(testDatasetItem);
        when(datasetRepository.findByIdAndStatusNot("dataset-id-123", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.of(testDataset));
        when(datasetItemRepository.countByDatasetIdAndStatusNot("dataset-id-123", DatasetItem.ItemStatus.DELETED))
                .thenReturn(1L);
        when(datasetRepository.save(any(Dataset.class))).thenReturn(testDataset);

        datasetService.deleteDatasetItem("item-id-123");

        assertEquals(DatasetItem.ItemStatus.DELETED, testDatasetItem.getStatus());
        verify(datasetItemRepository).save(testDatasetItem);
    }

    @Test
    void listDatasetItems_Success() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("sequence").ascending());
        Page<DatasetItem> page = new PageImpl<>(Collections.singletonList(testDatasetItem), pageable, 1);

        when(datasetRepository.existsById("dataset-id-123")).thenReturn(true);
        when(datasetItemRepository.findByDatasetIdAndStatusNot("dataset-id-123", DatasetItem.ItemStatus.DELETED, pageable))
                .thenReturn(page);
        when(datasetItemMapper.toResponse(testDatasetItem)).thenReturn(testDatasetItemResponse);

        PageResponse<DatasetItemResponse> result = datasetService.listDatasetItems("dataset-id-123", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listDatasetItemsByVersion_Success() {
        when(datasetRepository.existsById("dataset-id-123")).thenReturn(true);
        when(datasetItemRepository.findByDatasetIdAndVersionAndStatusNot("dataset-id-123", 1, DatasetItem.ItemStatus.DELETED))
                .thenReturn(Collections.singletonList(testDatasetItem));
        when(datasetItemMapper.toResponseList(anyList())).thenReturn(Collections.singletonList(testDatasetItemResponse));

        List<DatasetItemResponse> result = datasetService.listDatasetItemsByVersion("dataset-id-123", 1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void importDataset_Success() {
        DatasetImportRequest request = new DatasetImportRequest();
        request.setName("Imported Dataset");
        request.setDescription("Imported description");
        request.setCategory("import-category");

        DatasetImportRequest.DatasetItemImportData itemData = new DatasetImportRequest.DatasetItemImportData();
        itemData.setInput("Input 1");
        itemData.setOutput("Output 1");
        itemData.setMetadata("{}");
        request.setItems(Collections.singletonList(itemData));

        Dataset importedDataset = Dataset.builder()
                .id("imported-id")
                .name("Imported Dataset")
                .description("Imported description")
                .category("import-category")
                .version(1)
                .status(Dataset.DatasetStatus.ACTIVE)
                .itemCount(0)
                .sourceType("IMPORT")
                .build();

        Dataset savedDataset = Dataset.builder()
                .id("imported-id")
                .name("Imported Dataset")
                .description("Imported description")
                .category("import-category")
                .version(1)
                .status(Dataset.DatasetStatus.ACTIVE)
                .itemCount(1)
                .sourceType("IMPORT")
                .build();

        when(datasetRepository.existsByNameAndStatusNot("Imported Dataset", Dataset.DatasetStatus.DELETED))
                .thenReturn(false);
        when(datasetRepository.save(any(Dataset.class))).thenReturn(importedDataset).thenReturn(savedDataset);
        when(datasetItemMapper.importDataToEntity(itemData)).thenReturn(testDatasetItem);
        when(datasetItemRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testDatasetItem));
        when(datasetMapper.toResponse(savedDataset)).thenReturn(testDatasetResponse);

        DatasetResponse result = datasetService.importDataset(request);

        assertNotNull(result);
        verify(datasetRepository, times(2)).save(any(Dataset.class));
        verify(datasetItemRepository).saveAll(anyList());
    }

    @Test
    void importItemsToDataset_Success() {
        DatasetImportRequest.DatasetItemImportData itemData = new DatasetImportRequest.DatasetItemImportData();
        itemData.setInput("New input");
        itemData.setOutput("New output");

        List<DatasetImportRequest.DatasetItemImportData> items = Collections.singletonList(itemData);

        when(datasetRepository.findByIdAndStatusNot("dataset-id-123", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.of(testDataset));
        when(datasetItemRepository.countByDatasetIdAndVersionAndStatusNot("dataset-id-123", 1, DatasetItem.ItemStatus.DELETED))
                .thenReturn(2L);
        when(datasetItemMapper.importDataToEntity(itemData)).thenReturn(testDatasetItem);
        when(datasetItemRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testDatasetItem));
        when(datasetItemMapper.toResponseList(anyList())).thenReturn(Collections.singletonList(testDatasetItemResponse));
        when(datasetItemRepository.countByDatasetIdAndStatusNot("dataset-id-123", DatasetItem.ItemStatus.DELETED))
                .thenReturn(3L);
        when(datasetRepository.save(any(Dataset.class))).thenReturn(testDataset);

        List<DatasetItemResponse> result = datasetService.importItemsToDataset("dataset-id-123", items);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(datasetItemRepository).saveAll(anyList());
    }

    @Test
    void createNewVersion_Success() {
        DatasetVersionCreateRequest request = new DatasetVersionCreateRequest();
        request.setDatasetId("dataset-id-123");
        request.setDescription("New version");

        Dataset updatedDataset = Dataset.builder()
                .id("dataset-id-123")
                .name("Test Dataset")
                .version(2)
                .build();

        when(datasetRepository.findByIdAndStatusNot("dataset-id-123", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.of(testDataset));
        when(datasetRepository.save(any(Dataset.class))).thenReturn(updatedDataset);
        when(datasetItemRepository.findByDatasetIdAndVersionAndStatusNot("dataset-id-123", 1, DatasetItem.ItemStatus.DELETED))
                .thenReturn(Collections.singletonList(testDatasetItem));
        when(datasetItemRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testDatasetItem));
        when(datasetMapper.toResponse(updatedDataset)).thenReturn(testDatasetResponse);

        DatasetResponse result = datasetService.createNewVersion("dataset-id-123", request);

        assertNotNull(result);
        assertEquals(2, testDataset.getVersion().intValue());
        verify(datasetRepository).save(testDataset);
        verify(datasetItemRepository).saveAll(anyList());
    }

    @Test
    void exportDatasetAsJson_Success() {
        when(datasetRepository.findByIdAndStatusNot("dataset-id-123", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.of(testDataset));
        when(datasetItemRepository.findByDatasetIdAndVersionAndStatusNot("dataset-id-123", 1, DatasetItem.ItemStatus.DELETED))
                .thenReturn(Collections.singletonList(testDatasetItem));

        byte[] result = datasetService.exportDatasetAsJson("dataset-id-123");

        assertNotNull(result);
        assertTrue(result.length > 0);
        String json = new String(result);
        assertTrue(json.contains("Test Dataset"));
        assertTrue(json.contains("Test input"));
    }

    @Test
    void exportDatasetAsCsv_Success() {
        when(datasetRepository.findByIdAndStatusNot("dataset-id-123", Dataset.DatasetStatus.DELETED))
                .thenReturn(Optional.of(testDataset));
        when(datasetItemRepository.findByDatasetIdAndVersionAndStatusNot("dataset-id-123", 1, DatasetItem.ItemStatus.DELETED))
                .thenReturn(Collections.singletonList(testDatasetItem));

        byte[] result = datasetService.exportDatasetAsCsv("dataset-id-123");

        assertNotNull(result);
        assertTrue(result.length > 0);
        String csv = new String(result);
        assertTrue(csv.contains("input,output,metadata"));
        assertTrue(csv.contains("Test input"));
        assertTrue(csv.contains("Test output"));
    }
}