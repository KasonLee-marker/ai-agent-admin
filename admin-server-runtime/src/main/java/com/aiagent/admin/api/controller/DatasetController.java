package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
@Tag(name = "Dataset Management", description = "APIs for managing datasets and dataset items")
public class DatasetController {

    private final DatasetService datasetService;

    @PostMapping
    @Operation(summary = "Create a new dataset")
    public ApiResponse<DatasetResponse> createDataset(
            @Valid @RequestBody DatasetCreateRequest request) {
        return ApiResponse.success(datasetService.createDataset(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dataset by ID")
    public ApiResponse<DatasetResponse> getDataset(
            @Parameter(description = "Dataset ID") @PathVariable String id) {
        return ApiResponse.success(datasetService.getDataset(id));
    }

    @GetMapping
    @Operation(summary = "List datasets with pagination and filters")
    public ApiResponse<PageResponse<DatasetResponse>> listDatasets(
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ApiResponse.success(datasetService.listDatasets(category, keyword, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a dataset")
    public ApiResponse<DatasetResponse> updateDataset(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Valid @RequestBody DatasetUpdateRequest request) {
        return ApiResponse.success(datasetService.updateDataset(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a dataset")
    public ApiResponse<Void> deleteDataset(
            @Parameter(description = "Dataset ID") @PathVariable String id) {
        datasetService.deleteDataset(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Create a new dataset item")
    public ApiResponse<DatasetItemResponse> createDatasetItem(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Valid @RequestBody DatasetItemCreateRequest request) {
        request.setDatasetId(id);
        return ApiResponse.success(datasetService.createDatasetItem(request));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "List dataset items with pagination")
    public ApiResponse<PageResponse<DatasetItemResponse>> listDatasetItems(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sequence"));
        return ApiResponse.success(datasetService.listDatasetItems(id, pageable));
    }

    @GetMapping("/{id}/items/all")
    @Operation(summary = "List all dataset items by version")
    public ApiResponse<List<DatasetItemResponse>> listDatasetItemsByVersion(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Parameter(description = "Version number") @RequestParam(required = false) Integer version) {
        if (version == null) {
            DatasetResponse dataset = datasetService.getDataset(id);
            version = dataset.getVersion();
        }
        return ApiResponse.success(datasetService.listDatasetItemsByVersion(id, version));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "Get dataset item by ID")
    public ApiResponse<DatasetItemResponse> getDatasetItem(
            @Parameter(description = "Item ID") @PathVariable String itemId) {
        return ApiResponse.success(datasetService.getDatasetItem(itemId));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update a dataset item")
    public ApiResponse<DatasetItemResponse> updateDatasetItem(
            @Parameter(description = "Item ID") @PathVariable String itemId,
            @Valid @RequestBody DatasetItemUpdateRequest request) {
        return ApiResponse.success(datasetService.updateDatasetItem(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Delete a dataset item")
    public ApiResponse<Void> deleteDatasetItem(
            @Parameter(description = "Item ID") @PathVariable String itemId) {
        datasetService.deleteDatasetItem(itemId);
        return ApiResponse.success();
    }

    @PostMapping("/import")
    @Operation(summary = "Import a dataset with items")
    public ApiResponse<DatasetResponse> importDataset(
            @Valid @RequestBody DatasetImportRequest request) {
        return ApiResponse.success(datasetService.importDataset(request));
    }

    @PostMapping("/{id}/import")
    @Operation(summary = "Import items to an existing dataset")
    public ApiResponse<List<DatasetItemResponse>> importItemsToDataset(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Valid @RequestBody List<DatasetImportRequest.DatasetItemImportData> items) {
        return ApiResponse.success(datasetService.importItemsToDataset(id, items));
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "Create a new version of a dataset")
    public ApiResponse<DatasetResponse> createNewVersion(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Valid @RequestBody DatasetVersionCreateRequest request) {
        return ApiResponse.success(datasetService.createNewVersion(id, request));
    }

    @GetMapping("/{id}/export/json")
    @Operation(summary = "Export dataset as JSON")
    public ResponseEntity<byte[]> exportDatasetAsJson(
            @Parameter(description = "Dataset ID") @PathVariable String id) {
        byte[] data = datasetService.exportDatasetAsJson(id);
        DatasetResponse dataset = datasetService.getDataset(id);
        String filename = dataset.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + "_v" + dataset.getVersion() + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    @GetMapping("/{id}/export/csv")
    @Operation(summary = "Export dataset as CSV")
    public ResponseEntity<byte[]> exportDatasetAsCsv(
            @Parameter(description = "Dataset ID") @PathVariable String id) {
        byte[] data = datasetService.exportDatasetAsCsv(id);
        DatasetResponse dataset = datasetService.getDataset(id);
        String filename = dataset.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + "_v" + dataset.getVersion() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }
}