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

/**
 * 数据集管理 REST 控制器
 * <p>
 * 提供数据集和数据集项的管理 API：
 * <ul>
 *   <li>数据集创建、查询、更新、删除</li>
 *   <li>数据集项（测试数据）管理</li>
 *   <li>数据集导入导出（JSON/CSV）</li>
 *   <li>版本管理</li>
 * </ul>
 * </p>
 * <p>
 * 数据集用于模型评估，存储输入数据和预期输出。
 * </p>
 *
 * @see DatasetService
 */
@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
@Tag(name = "Dataset Management", description = "APIs for managing datasets and dataset items")
public class DatasetController {

    private final DatasetService datasetService;

    /**
     * 创建新数据集
     *
     * @param request 数据集创建请求，包含名称、描述、分类、标签等
     * @return 创建成功的数据集信息
     */
    @PostMapping
    @Operation(summary = "Create a new dataset")
    public ApiResponse<DatasetResponse> createDataset(
            @Valid @RequestBody DatasetCreateRequest request) {
        return ApiResponse.success(datasetService.createDataset(request));
    }

    /**
     * 根据ID获取数据集详情
     *
     * @param id 数据集ID
     * @return 数据集详情信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get dataset by ID")
    public ApiResponse<DatasetResponse> getDataset(
            @Parameter(description = "Dataset ID") @PathVariable String id) {
        return ApiResponse.success(datasetService.getDataset(id));
    }

    /**
     * 分页查询数据集列表
     * <p>
     * 支持按分类、关键词筛选，结果按更新时间倒序排列。
     * </p>
     *
     * @param category 分类筛选（可选）
     * @param keyword  搜索关键词（可选，匹配名称和描述）
     * @param page     页码（从0开始）
     * @param size     每页数量
     * @return 分页的数据集列表
     */
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

    /**
     * 更新数据集信息
     *
     * @param id      数据集ID
     * @param request 更新请求，包含新的名称、描述、分类等
     * @return 更新后的数据集信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a dataset")
    public ApiResponse<DatasetResponse> updateDataset(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Valid @RequestBody DatasetUpdateRequest request) {
        return ApiResponse.success(datasetService.updateDataset(id, request));
    }

    /**
     * 删除数据集
     * <p>
     * 同时删除数据集下的所有数据项。
     * </p>
     *
     * @param id 数据集ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a dataset")
    public ApiResponse<Void> deleteDataset(
            @Parameter(description = "Dataset ID") @PathVariable String id) {
        datasetService.deleteDataset(id);
        return ApiResponse.success();
    }

    /**
     * 创建数据集项（测试数据）
     *
     * @param id      数据集ID
     * @param request 数据项创建请求，包含输入、预期输出等
     * @return 创建成功的数据项信息
     */
    @PostMapping("/{id}/items")
    @Operation(summary = "Create a new dataset item")
    public ApiResponse<DatasetItemResponse> createDatasetItem(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Valid @RequestBody DatasetItemCreateRequest request) {
        request.setDatasetId(id);
        return ApiResponse.success(datasetService.createDatasetItem(request));
    }

    /**
     * 分页查询数据集项列表
     * <p>
     * 结果按序号升序排列。
     * </p>
     *
     * @param id   数据集ID
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 分页的数据项列表
     */
    @GetMapping("/{id}/items")
    @Operation(summary = "List dataset items with pagination")
    public ApiResponse<PageResponse<DatasetItemResponse>> listDatasetItems(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sequence"));
        return ApiResponse.success(datasetService.listDatasetItems(id, pageable));
    }

    /**
     * 查询指定版本的所有数据集项
     * <p>
     * 如果不指定版本，返回当前最新版本的数据项。
     * </p>
     *
     * @param id      数据集ID
     * @param version 版本号（可选）
     * @return 数据项列表
     */
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

    /**
     * 根据ID获取数据集项详情
     *
     * @param itemId 数据项ID
     * @return 数据项详情信息
     */
    @GetMapping("/items/{itemId}")
    @Operation(summary = "Get dataset item by ID")
    public ApiResponse<DatasetItemResponse> getDatasetItem(
            @Parameter(description = "Item ID") @PathVariable String itemId) {
        return ApiResponse.success(datasetService.getDatasetItem(itemId));
    }

    /**
     * 更新数据集项
     *
     * @param itemId  数据项ID
     * @param request 更新请求
     * @return 更新后的数据项信息
     */
    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update a dataset item")
    public ApiResponse<DatasetItemResponse> updateDatasetItem(
            @Parameter(description = "Item ID") @PathVariable String itemId,
            @Valid @RequestBody DatasetItemUpdateRequest request) {
        return ApiResponse.success(datasetService.updateDatasetItem(itemId, request));
    }

    /**
     * 删除数据集项
     *
     * @param itemId 数据项ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Delete a dataset item")
    public ApiResponse<Void> deleteDatasetItem(
            @Parameter(description = "Item ID") @PathVariable String itemId) {
        datasetService.deleteDatasetItem(itemId);
        return ApiResponse.success();
    }

    /**
     * 删除数据集项（通过数据集ID和项ID）
     *
     * @param id     数据集ID
     * @param itemId 数据项ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "Delete a dataset item by dataset ID and item ID")
    public ApiResponse<Void> deleteDatasetItemByDatasetId(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Parameter(description = "Item ID") @PathVariable String itemId) {
        datasetService.deleteDatasetItem(itemId);
        return ApiResponse.success();
    }

    /**
     * 导入数据集（含数据项）
     * <p>
     * 解析导入请求中的数据，创建新数据集并添加数据项。
     * </p>
     *
     * @param request 导入请求，包含数据集信息和数据项列表
     * @return 创建成功的数据集信息
     */
    @PostMapping("/import")
    @Operation(summary = "Import a dataset with items")
    public ApiResponse<DatasetResponse> importDataset(
            @Valid @RequestBody DatasetImportRequest request) {
        return ApiResponse.success(datasetService.importDataset(request));
    }

    /**
     * 向现有数据集导入数据项
     *
     * @param id    数据集ID
     * @param items 要导入的数据项列表
     * @return 导入成功的数据项列表
     */
    @PostMapping("/{id}/import")
    @Operation(summary = "Import items to an existing dataset")
    public ApiResponse<List<DatasetItemResponse>> importItemsToDataset(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Valid @RequestBody List<DatasetImportRequest.DatasetItemImportData> items) {
        return ApiResponse.success(datasetService.importItemsToDataset(id, items));
    }

    /**
     * 创建数据集新版本
     * <p>
     * 基于当前版本创建新版本，保留历史版本数据。
     * </p>
     *
     * @param id      数据集ID
     * @param request 版本创建请求，包含版本描述
     * @return 新版本的数据集信息
     */
    @PostMapping("/{id}/versions")
    @Operation(summary = "Create a new version of a dataset")
    public ApiResponse<DatasetResponse> createNewVersion(
            @Parameter(description = "Dataset ID") @PathVariable String id,
            @Valid @RequestBody DatasetVersionCreateRequest request) {
        return ApiResponse.success(datasetService.createNewVersion(id, request));
    }

    /**
     * 导出数据集为JSON文件
     * <p>
     * 导出包含数据集信息和所有数据项的JSON文件，
     * 文件名格式为：数据集名称_v版本号.json。
     * </p>
     *
     * @param id 数据集ID
     * @return JSON文件下载响应
     */
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

    /**
     * 导出数据集为CSV文件
     * <p>
     * 导出包含数据项的CSV文件，
     * 文件名格式为：数据集名称_v版本号.csv。
     * </p>
     *
     * @param id 数据集ID
     * @return CSV文件下载响应
     */
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