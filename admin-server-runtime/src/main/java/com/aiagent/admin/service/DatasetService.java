package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 数据集服务接口
 * <p>
 * 提供数据集和数据项的管理功能：
 * <ul>
 *   <li>数据集的创建、查询、更新、删除</li>
 *   <li>数据项的 CRUD 操作</li>
 *   <li>数据集导入导出（JSON/CSV）</li>
 *   <li>数据集版本管理</li>
 * </ul>
 * </p>
 * <p>
 * 数据集用于存储测试数据，支持评估系统进行模型性能评测。
 * </p>
 *
 * @see DatasetResponse
 * @see DatasetItemResponse
 */
public interface DatasetService {

    /**
     * 创建数据集
     *
     * @param request 创建请求，包含名称、描述、类别等
     * @return 创建成功的数据集响应 DTO
     */
    DatasetResponse createDataset(DatasetCreateRequest request);

    /**
     * 更新数据集信息
     *
     * @param id      数据集唯一标识
     * @param request 更新请求
     * @return 更新后的数据集响应 DTO
     */
    DatasetResponse updateDataset(String id, DatasetUpdateRequest request);

    /**
     * 删除数据集
     * <p>
     * 同时删除数据集下的所有数据项。
     * </p>
     *
     * @param id 数据集唯一标识
     */
    void deleteDataset(String id);

    /**
     * 获取数据集详情
     *
     * @param id 数据集唯一标识
     * @return 数据集响应 DTO
     */
    DatasetResponse getDataset(String id);

    /**
     * 分页查询数据集列表
     *
     * @param category 数据集类别过滤（可选）
     * @param keyword  搜索关键词（可选）
     * @param pageable 分页参数
     * @return 数据集分页响应
     */
    PageResponse<DatasetResponse> listDatasets(String category, String keyword, Pageable pageable);

    /**
     * 创建数据项
     *
     * @param request 创建请求，包含问题、期望回答等
     * @return 创建成功的数据项响应 DTO
     */
    DatasetItemResponse createDatasetItem(DatasetItemCreateRequest request);

    /**
     * 更新数据项
     *
     * @param id      数据项唯一标识
     * @param request 更新请求
     * @return 更新后的数据项响应 DTO
     */
    DatasetItemResponse updateDatasetItem(String id, DatasetItemUpdateRequest request);

    /**
     * 删除数据项
     *
     * @param id 数据项唯一标识
     */
    void deleteDatasetItem(String id);

    /**
     * 获取数据项详情
     *
     * @param id 数据项唯一标识
     * @return 数据项响应 DTO
     */
    DatasetItemResponse getDatasetItem(String id);

    /**
     * 分页查询数据项列表
     *
     * @param datasetId 数据集 ID
     * @param pageable  分页参数
     * @return 数据项分页响应
     */
    PageResponse<DatasetItemResponse> listDatasetItems(String datasetId, Pageable pageable);

    /**
     * 查询指定版本的数据项列表
     *
     * @param datasetId 数据集 ID
     * @param version   版本号
     * @return 数据项响应列表
     */
    List<DatasetItemResponse> listDatasetItemsByVersion(String datasetId, Integer version);

    /**
     * 导入数据集（批量创建数据项）
     *
     * @param request 导入请求，包含文件数据
     * @return 导入后的数据集响应 DTO
     */
    DatasetResponse importDataset(DatasetImportRequest request);

    /**
     * 向指定数据集导入数据项
     *
     * @param datasetId 数据集 ID
     * @param items     数据项导入数据列表
     * @return 导入成功的数据项响应列表
     */
    List<DatasetItemResponse> importItemsToDataset(String datasetId, List<DatasetImportRequest.DatasetItemImportData> items);

    /**
     * 创建数据集新版本
     *
     * @param datasetId 数据集 ID
     * @param request   版本创建请求
     * @return 新版本的数据集响应 DTO
     */
    DatasetResponse createNewVersion(String datasetId, DatasetVersionCreateRequest request);

    /**
     * 导出数据集为 JSON 格式
     *
     * @param datasetId 数据集 ID
     * @return JSON 格式的字节数组
     */
    byte[] exportDatasetAsJson(String datasetId);

    /**
     * 导出数据集为 CSV 格式
     *
     * @param datasetId 数据集 ID
     * @return CSV 格式的字节数组
     */
    byte[] exportDatasetAsCsv(String datasetId);
}
