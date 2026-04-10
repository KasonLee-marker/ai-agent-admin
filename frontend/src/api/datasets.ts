import client from './client'
import {ApiResponse, PageParams, PageResponse} from '@/types/api'
import {
    CreateDatasetItemRequest,
    CreateDatasetRequest,
    Dataset,
    DatasetItem,
    ImportDatasetRequest,
    UpdateDatasetRequest
} from '@/types/dataset'

const BASE_URL = '/datasets'

// 获取数据集列表
export async function listDatasets(params?: PageParams): Promise<ApiResponse<PageResponse<Dataset>>> {
    return client.get(BASE_URL, {params})
}

// 获取数据集详情
export async function getDataset(id: string): Promise<ApiResponse<Dataset>> {
    return client.get(`${BASE_URL}/${id}`)
}

// 创建数据集
export async function createDataset(data: CreateDatasetRequest): Promise<ApiResponse<Dataset>> {
    return client.post(BASE_URL, data)
}

// 更新数据集
export async function updateDataset(id: string, data: UpdateDatasetRequest): Promise<ApiResponse<Dataset>> {
    return client.put(`${BASE_URL}/${id}`, data)
}

// 删除数据集
export async function deleteDataset(id: string): Promise<ApiResponse<void>> {
    return client.delete(`${BASE_URL}/${id}`)
}

// 获取数据项列表
export async function listDatasetItems(datasetId: string, params?: PageParams): Promise<ApiResponse<PageResponse<DatasetItem>>> {
    return client.get(`${BASE_URL}/${datasetId}/items`, {params})
}

// 创建数据项
export async function createDatasetItem(datasetId: string, data: CreateDatasetItemRequest): Promise<ApiResponse<DatasetItem>> {
    return client.post(`${BASE_URL}/${datasetId}/items`, data)
}

// 删除数据项
export async function deleteDatasetItem(datasetId: string, itemId: string): Promise<ApiResponse<void>> {
    return client.delete(`${BASE_URL}/${datasetId}/items/${itemId}`)
}

// 导入数据
export async function importDataset(data: ImportDatasetRequest): Promise<ApiResponse<Dataset>> {
    return client.post(`${BASE_URL}/import`, data)
}

// 导出 JSON
export async function exportDatasetJson(id: string): Promise<ApiResponse<DatasetItem[]>> {
    return client.get(`${BASE_URL}/${id}/export/json`)
}

// 导出 CSV
export async function exportDatasetCsv(id: string): Promise<ApiResponse<string>> {
    return client.get(`${BASE_URL}/${id}/export/csv`)
}

// 创建新版本
export async function createDatasetVersion(id: string): Promise<ApiResponse<Dataset>> {
    return client.post(`${BASE_URL}/${id}/versions`)
}