import client from './client'
import {ApiResponse, PageResponse, PageParams} from '@/types/api'
import {
    PromptTemplate,
    PromptVersion,
    PromptTemplateCreateRequest,
    PromptTemplateUpdateRequest,
    RollbackRequest
} from '@/types/prompt'

const BASE_URL = '/prompts'

// 获取列表
export async function listPrompts(params?: PageParams & {
    category?: string;
    tag?: string;
    keyword?: string
}): Promise<ApiResponse<PageResponse<PromptTemplate>>> {
    return client.get(BASE_URL, {params})
}

// 获取详情
export async function getPrompt(id: string): Promise<ApiResponse<PromptTemplate>> {
    return client.get(`${BASE_URL}/${id}`)
}

// 创建
export async function createPrompt(data: PromptTemplateCreateRequest): Promise<ApiResponse<PromptTemplate>> {
    return client.post(BASE_URL, data)
}

// 更新
export async function updatePrompt(id: string, data: PromptTemplateUpdateRequest): Promise<ApiResponse<PromptTemplate>> {
    return client.put(`${BASE_URL}/${id}`, data)
}

// 删除
export async function deletePrompt(id: string): Promise<ApiResponse<void>> {
    return client.delete(`${BASE_URL}/${id}`)
}

// 获取版本历史
export async function getPromptVersions(id: string): Promise<ApiResponse<PromptVersion[]>> {
    return client.get(`${BASE_URL}/${id}/versions`)
}

// 版本回滚
export async function rollbackPrompt(id: string, data: RollbackRequest): Promise<ApiResponse<PromptTemplate>> {
    return client.post(`${BASE_URL}/${id}/rollback`, data)
}