import client from './client'
import {ApiResponse} from '@/types/api'
import {RagMessage, RagSession} from '@/types/rag'

const SESSION_URL = '/rag/sessions'

/**
 * 创建 RAG 会话
 */
export async function createRagSession(params?: {
    knowledgeBaseId?: string
    modelId?: string
    embeddingModelId?: string
}): Promise<ApiResponse<RagSession>> {
    return client.post(SESSION_URL, null, {params})
}

/**
 * 获取会话详情
 */
export async function getRagSession(id: string): Promise<ApiResponse<RagSession>> {
    return client.get(`${SESSION_URL}/${id}`)
}

/**
 * 获取会话列表
 */
export async function listRagSessions(page = 0, size = 10): Promise<ApiResponse<RagSession[]>> {
    return client.get(SESSION_URL, {params: {page, size}})
}

/**
 * 删除会话
 */
export async function deleteRagSession(id: string): Promise<ApiResponse<void>> {
    return client.delete(`${SESSION_URL}/${id}`)
}

/**
 * 获取会话消息历史
 */
export async function getRagSessionMessages(id: string, limit?: number): Promise<ApiResponse<RagMessage[]>> {
    return client.get(`${SESSION_URL}/${id}/messages`, {params: {limit}})
}