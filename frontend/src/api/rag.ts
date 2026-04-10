import client from './client'
import {ApiResponse} from '@/types/api'
import {RagChatRequest, RagChatResponse, VectorSearchRequest, VectorSearchResult} from '@/types/rag'

const VECTOR_URL = '/vector'
const RAG_URL = '/rag'

// 向量搜索
export async function vectorSearch(data: VectorSearchRequest): Promise<ApiResponse<VectorSearchResult[]>> {
    return client.post(`${VECTOR_URL}/search`, data)
}

// RAG 对话
export async function ragChat(data: RagChatRequest): Promise<ApiResponse<RagChatResponse>> {
    return client.post(`${RAG_URL}/chat`, data)
}