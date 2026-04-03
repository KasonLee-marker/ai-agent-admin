package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DatasetService {

    DatasetResponse createDataset(DatasetCreateRequest request);

    DatasetResponse updateDataset(String id, DatasetUpdateRequest request);

    void deleteDataset(String id);

    DatasetResponse getDataset(String id);

    PageResponse<DatasetResponse> listDatasets(String category, String keyword, Pageable pageable);

    DatasetItemResponse createDatasetItem(DatasetItemCreateRequest request);

    DatasetItemResponse updateDatasetItem(String id, DatasetItemUpdateRequest request);

    void deleteDatasetItem(String id);

    DatasetItemResponse getDatasetItem(String id);

    PageResponse<DatasetItemResponse> listDatasetItems(String datasetId, Pageable pageable);

    List<DatasetItemResponse> listDatasetItemsByVersion(String datasetId, Integer version);

    DatasetResponse importDataset(DatasetImportRequest request);

    List<DatasetItemResponse> importItemsToDataset(String datasetId, List<DatasetImportRequest.DatasetItemImportData> items);

    DatasetResponse createNewVersion(String datasetId, DatasetVersionCreateRequest request);

    byte[] exportDatasetAsJson(String datasetId);

    byte[] exportDatasetAsCsv(String datasetId);
}
