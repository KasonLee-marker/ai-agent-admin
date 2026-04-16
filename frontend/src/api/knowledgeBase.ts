import client from './client'
import {ApiResponse} from '@/types/api'
import {KnowledgeBase, KnowledgeBaseRequest, ReindexProgressResponse, ReindexRequest} from '@/types/knowledgeBase'

const KB_URL = '/knowledge-bases'

/**
 * 创建知识库
 */
export async function createKnowledgeBase(data: KnowledgeBaseRequest): Promise<ApiResponse<KnowledgeBase>> {
    return client.post(KB_URL, data)
}

/**
 * 获取知识库详情
 */
export async function getKnowledgeBase(id: string): Promise<ApiResponse<KnowledgeBase>> {
    return client.get(`${KB_URL}/${id}`)
}

/**
 * 分页获取知识库列表
 */
export async function listKnowledgeBases(page = 0, size = 10): Promise<ApiResponse<KnowledgeBase[]>> {
    return client.get(KB_URL, {params: {page, size}})
}

/**
 * 获取所有知识库列表（不分页）
 */
export async function listAllKnowledgeBases(): Promise<ApiResponse<KnowledgeBase[]>> {
    return client.get(`${KB_URL}/all`)
}

/**
 * 更新知识库
 */
export async function updateKnowledgeBase(id: string, data: KnowledgeBaseRequest): Promise<ApiResponse<KnowledgeBase>> {
    return client.put(`${KB_URL}/${id}`, data)
}

/**
 * 删除知识库
 */
export async function deleteKnowledgeBase(id: string): Promise<ApiResponse<void>> {
    return client.delete(`${KB_URL}/${id}`)
}

/**
 * 启动知识库重索引
 */
export async function startReindex(id: string, data: ReindexRequest): Promise<ApiResponse<ReindexProgressResponse>> {
    return client.post(`${KB_URL}/${id}/reindex`, data)
}

/**
 * 获取重索引进度
 */
export async function getReindexProgress(id: string): Promise<ApiResponse<ReindexProgressResponse>> {
    return client.get(`${KB_URL}/${id}/reindex/progress`)
}

/**
 * 取消重索引
 */
export async function cancelReindex(id: string): Promise<ApiResponse<void>> {
    return client.post(`${KB_URL}/${id}/reindex/cancel`)
}