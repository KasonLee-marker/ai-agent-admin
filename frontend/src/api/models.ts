import client from './client'
import {ApiResponse} from '@/types/api'
import {ModelConfig, ProviderInfo, BuiltinModel, CreateModelRequest, UpdateModelRequest} from '@/types/model'

const BASE_URL = '/models'

// 获取列表
export async function listModels(params?: {
    provider?: string;
    isActive?: boolean;
    keyword?: string
}): Promise<ApiResponse<ModelConfig[]>> {
    return client.get(BASE_URL, {params})
}

// 获取详情
export async function getModel(id: string): Promise<ApiResponse<ModelConfig>> {
    return client.get(`${BASE_URL}/${id}`)
}

// 创建
export async function createModel(data: CreateModelRequest): Promise<ApiResponse<ModelConfig>> {
    return client.post(BASE_URL, data)
}

// 更新
export async function updateModel(id: string, data: UpdateModelRequest): Promise<ApiResponse<ModelConfig>> {
    return client.put(`${BASE_URL}/${id}`, data)
}

// 删除
export async function deleteModel(id: string): Promise<ApiResponse<void>> {
    return client.delete(`${BASE_URL}/${id}`)
}

// 健康检查
export async function testModel(id: string): Promise<ApiResponse<{ modelId: string; healthy: boolean }>> {
    return client.post(`${BASE_URL}/${id}/test`)
}

// 设为默认
export async function setDefaultModel(id: string): Promise<ApiResponse<void>> {
    return client.post(`${BASE_URL}/${id}/default`)
}

// 获取默认模型
export async function getDefaultModel(): Promise<ApiResponse<ModelConfig>> {
    return client.get(`${BASE_URL}/default`)
}

// 获取供应商列表
export async function listProviders(): Promise<ApiResponse<ProviderInfo[]>> {
    return client.get(`${BASE_URL}/providers`)
}

// 获取内置模型
export async function getBuiltinModels(provider: string): Promise<ApiResponse<BuiltinModel[]>> {
    return client.get(`${BASE_URL}/providers/${provider}/builtin`)
}

// 切换模型
export async function switchModel(modelId: string): Promise<ApiResponse<void>> {
    return client.post(`${BASE_URL}/switch`, {modelId})
}