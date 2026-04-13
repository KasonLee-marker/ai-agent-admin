import client from './client'
import {ApiResponse, PageParams, PageResponse} from '@/types/api'
import {
    CreateDatasetItemRequest,
    CreateDatasetRequest,
    Dataset,
    DatasetItem,
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

// 更新数据项
export async function updateDatasetItem(itemId: string, data: {
    input: string;
    output?: string
}): Promise<ApiResponse<DatasetItem>> {
    return client.put(`${BASE_URL}/items/${itemId}`, data)
}

// 导入数据项到现有数据集
export async function importItemsToDataset(datasetId: string, items: {
    input: string;
    output?: string
}[]): Promise<ApiResponse<DatasetItem[]>> {
    return client.post(`${BASE_URL}/${datasetId}/import`, items)
}

// 导出 JSON（文件下载）
export async function exportDatasetJson(datasetId: string): Promise<void> {
    const response = await client.get(`${BASE_URL}/${datasetId}/export/json`, {responseType: 'blob'})
    const blob = new Blob([response.data], {type: 'application/json'})
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `dataset_export.json`
    link.click()
    window.URL.revokeObjectURL(url)
}

// 导出 CSV（文件下载）
export async function exportDatasetCsv(datasetId: string): Promise<void> {
    const response = await client.get(`${BASE_URL}/${datasetId}/export/csv`, {responseType: 'blob'})
    const blob = new Blob([response.data], {type: 'text/csv'})
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `dataset_export.csv`
    link.click()
    window.URL.revokeObjectURL(url)
}

// 创建新版本
export async function createDatasetVersion(id: string): Promise<ApiResponse<Dataset>> {
    return client.post(`${BASE_URL}/${id}/versions`)
}