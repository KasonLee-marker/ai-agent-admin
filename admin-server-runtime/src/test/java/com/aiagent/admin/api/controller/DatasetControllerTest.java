package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.DatasetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DatasetController.class)
@ContextConfiguration(classes = DatasetController.class)
class DatasetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DatasetService datasetService;

    private DatasetResponse testDatasetResponse;
    private DatasetItemResponse testDatasetItemResponse;

    @BeforeEach
    void setUp() {
        testDatasetResponse = new DatasetResponse();
        testDatasetResponse.setId("dataset-id-123");
        testDatasetResponse.setName("Test Dataset");
        testDatasetResponse.setDescription("Test description");
        testDatasetResponse.setCategory("test-category");
        testDatasetResponse.setVersion(1);
        testDatasetResponse.setStatus("ACTIVE");
        testDatasetResponse.setItemCount(2);
        testDatasetResponse.setCreatedAt(LocalDateTime.now());
        testDatasetResponse.setUpdatedAt(LocalDateTime.now());

        testDatasetItemResponse = new DatasetItemResponse();
        testDatasetItemResponse.setId("item-id-123");
        testDatasetItemResponse.setDatasetId("dataset-id-123");
        testDatasetItemResponse.setVersion(1);
        testDatasetItemResponse.setSequence(1);
        testDatasetItemResponse.setInput("Test input");
        testDatasetItemResponse.setOutput("Test output");
        testDatasetItemResponse.setStatus("ACTIVE");
        testDatasetItemResponse.setCreatedAt(LocalDateTime.now());
        testDatasetItemResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createDataset_Success() throws Exception {
        DatasetCreateRequest request = new DatasetCreateRequest();
        request.setName("New Dataset");
        request.setDescription("New description");
        request.setCategory("category");

        when(datasetService.createDataset(any(DatasetCreateRequest.class))).thenReturn(testDatasetResponse);

        mockMvc.perform(post("/api/v1/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("dataset-id-123"))
                .andExpect(jsonPath("$.data.name").value("Test Dataset"));

        verify(datasetService).createDataset(any(DatasetCreateRequest.class));
    }

    @Test
    void getDataset_Success() throws Exception {
        when(datasetService.getDataset("dataset-id-123")).thenReturn(testDatasetResponse);

        mockMvc.perform(get("/api/v1/datasets/dataset-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("dataset-id-123"))
                .andExpect(jsonPath("$.data.name").value("Test Dataset"));

        verify(datasetService).getDataset("dataset-id-123");
    }

    @Test
    void listDatasets_Success() throws Exception {
        PageResponse<DatasetResponse> pageResponse = PageResponse.of(
                Collections.singletonList(testDatasetResponse), 0, 20, 1);

        when(datasetService.listDatasets(any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/datasets")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value("dataset-id-123"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(datasetService).listDatasets(null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    @Test
    void updateDataset_Success() throws Exception {
        DatasetUpdateRequest request = new DatasetUpdateRequest();
        request.setName("Updated Dataset");
        request.setDescription("Updated description");

        when(datasetService.updateDataset(eq("dataset-id-123"), any(DatasetUpdateRequest.class)))
                .thenReturn(testDatasetResponse);

        mockMvc.perform(put("/api/v1/datasets/dataset-id-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("dataset-id-123"));

        verify(datasetService).updateDataset(eq("dataset-id-123"), any(DatasetUpdateRequest.class));
    }

    @Test
    void deleteDataset_Success() throws Exception {
        doNothing().when(datasetService).deleteDataset("dataset-id-123");

        mockMvc.perform(delete("/api/v1/datasets/dataset-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(datasetService).deleteDataset("dataset-id-123");
    }

    @Test
    void createDatasetItem_Success() throws Exception {
        DatasetItemCreateRequest request = new DatasetItemCreateRequest();
        request.setDatasetId("dataset-id-123");
        request.setInput("New input");
        request.setOutput("New output");

        when(datasetService.createDatasetItem(any(DatasetItemCreateRequest.class)))
                .thenReturn(testDatasetItemResponse);

        mockMvc.perform(post("/api/v1/datasets/dataset-id-123/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("item-id-123"));

        verify(datasetService).createDatasetItem(any(DatasetItemCreateRequest.class));
    }

    @Test
    void listDatasetItems_Success() throws Exception {
        PageResponse<DatasetItemResponse> pageResponse = PageResponse.of(
                Collections.singletonList(testDatasetItemResponse), 0, 20, 1);

        when(datasetService.listDatasetItems(eq("dataset-id-123"), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/datasets/dataset-id-123/items")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value("item-id-123"));

        verify(datasetService).listDatasetItems(eq("dataset-id-123"), any(Pageable.class));
    }

    @Test
    void listDatasetItemsByVersion_Success() throws Exception {
        when(datasetService.listDatasetItemsByVersion("dataset-id-123", 1))
                .thenReturn(Collections.singletonList(testDatasetItemResponse));

        mockMvc.perform(get("/api/v1/datasets/dataset-id-123/items/all")
                        .param("version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("item-id-123"));

        verify(datasetService).listDatasetItemsByVersion("dataset-id-123", 1);
    }

    @Test
    void getDatasetItem_Success() throws Exception {
        when(datasetService.getDatasetItem("item-id-123")).thenReturn(testDatasetItemResponse);

        mockMvc.perform(get("/api/v1/datasets/items/item-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("item-id-123"));

        verify(datasetService).getDatasetItem("item-id-123");
    }

    @Test
    void updateDatasetItem_Success() throws Exception {
        DatasetItemUpdateRequest request = new DatasetItemUpdateRequest();
        request.setInput("Updated input");
        request.setOutput("Updated output");

        when(datasetService.updateDatasetItem(eq("item-id-123"), any(DatasetItemUpdateRequest.class)))
                .thenReturn(testDatasetItemResponse);

        mockMvc.perform(put("/api/v1/datasets/items/item-id-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("item-id-123"));

        verify(datasetService).updateDatasetItem(eq("item-id-123"), any(DatasetItemUpdateRequest.class));
    }

    @Test
    void deleteDatasetItem_Success() throws Exception {
        doNothing().when(datasetService).deleteDatasetItem("item-id-123");

        mockMvc.perform(delete("/api/v1/datasets/items/item-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(datasetService).deleteDatasetItem("item-id-123");
    }

    @Test
    void importDataset_Success() throws Exception {
        DatasetImportRequest request = new DatasetImportRequest();
        request.setName("Imported Dataset");
        request.setDescription("Imported description");

        DatasetImportRequest.DatasetItemImportData itemData = new DatasetImportRequest.DatasetItemImportData();
        itemData.setInput("Input 1");
        itemData.setOutput("Output 1");
        request.setItems(Collections.singletonList(itemData));

        when(datasetService.importDataset(any(DatasetImportRequest.class))).thenReturn(testDatasetResponse);

        mockMvc.perform(post("/api/v1/datasets/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("dataset-id-123"));

        verify(datasetService).importDataset(any(DatasetImportRequest.class));
    }

    @Test
    void importItemsToDataset_Success() throws Exception {
        DatasetImportRequest.DatasetItemImportData itemData = new DatasetImportRequest.DatasetItemImportData();
        itemData.setInput("Input 1");
        itemData.setOutput("Output 1");

        when(datasetService.importItemsToDataset(eq("dataset-id-123"), anyList()))
                .thenReturn(Collections.singletonList(testDatasetItemResponse));

        mockMvc.perform(post("/api/v1/datasets/dataset-id-123/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(itemData))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("item-id-123"));

        verify(datasetService).importItemsToDataset(eq("dataset-id-123"), anyList());
    }

    @Test
    void createNewVersion_Success() throws Exception {
        DatasetVersionCreateRequest request = new DatasetVersionCreateRequest();
        request.setDatasetId("dataset-id-123");
        request.setDescription("New version");

        when(datasetService.createNewVersion(eq("dataset-id-123"), any(DatasetVersionCreateRequest.class)))
                .thenReturn(testDatasetResponse);

        mockMvc.perform(post("/api/v1/datasets/dataset-id-123/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("dataset-id-123"));

        verify(datasetService).createNewVersion(eq("dataset-id-123"), any(DatasetVersionCreateRequest.class));
    }

    @Test
    void exportDatasetAsJson_Success() throws Exception {
        String jsonContent = "{\"name\":\"Test Dataset\",\"items\":[]}";
        when(datasetService.exportDatasetAsJson("dataset-id-123")).thenReturn(jsonContent.getBytes());
        when(datasetService.getDataset("dataset-id-123")).thenReturn(testDatasetResponse);

        mockMvc.perform(get("/api/v1/datasets/dataset-id-123/export/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Test_Dataset_v1.json\""));

        verify(datasetService).exportDatasetAsJson("dataset-id-123");
    }

    @Test
    void exportDatasetAsCsv_Success() throws Exception {
        String csvContent = "input,output,metadata\nTest input,Test output,{}";
        when(datasetService.exportDatasetAsCsv("dataset-id-123")).thenReturn(csvContent.getBytes());
        when(datasetService.getDataset("dataset-id-123")).thenReturn(testDatasetResponse);

        mockMvc.perform(get("/api/v1/datasets/dataset-id-123/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Test_Dataset_v1.csv\""));

        verify(datasetService).exportDatasetAsCsv("dataset-id-123");
    }
}