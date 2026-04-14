import client from './client'
import {ApiResponse, PageParams, PageResponse} from '@/types/api'
import {Document, DocumentChunk, SupportedType} from '@/types/document'

const BASE_URL = '/documents'

// 上传文档（支持分块策略参数）
export async function uploadDocument(file: File, options?: {
    name?: string
    chunkStrategy?: 'FIXED_SIZE' | 'PARAGRAPH'
    chunkSize?: number
    chunkOverlap?: number
}): Promise<ApiResponse<Document>> {
    const formData = new FormData()
    formData.append('file', file)
    if (options?.name) formData.append('name', options.name)
    if (options?.chunkStrategy) formData.append('chunkStrategy', options.chunkStrategy)
    if (options?.chunkSize) formData.append('chunkSize', options.chunkSize.toString())
    if (options?.chunkOverlap) formData.append('chunkOverlap', options.chunkOverlap.toString())
    return client.post(`${BASE_URL}/upload`, formData, {
        headers: {'Content-Type': 'multipart/form-data'}
    })
}

// 开始 Embedding 计算（可选指定 embedding 模型）
export async function startEmbedding(id: string, embeddingModelId?: string): Promise<ApiResponse<Document>> {
    const params = embeddingModelId ? {embeddingModelId} : {}
    return client.post(`${BASE_URL}/${id}/embed`, null, {params})
}

// 获取文档列表
export async function listDocuments(params?: PageParams): Promise<ApiResponse<PageResponse<Document>>> {
    return client.get(BASE_URL, {params})
}

// 获取文档详情
export async function getDocument(id: string): Promise<ApiResponse<Document>> {
    return client.get(`${BASE_URL}/${id}`)
}

// 删除文档
export async function deleteDocument(id: string): Promise<ApiResponse<void>> {
    return client.delete(`${BASE_URL}/${id}`)
}

// 获取文档分块
export async function getDocumentChunks(id: string, params?: PageParams): Promise<ApiResponse<PageResponse<DocumentChunk>>> {
    return client.get(`${BASE_URL}/${id}/chunks`, {params})
}

// 获取处理状态
export async function getDocumentStatus(id: string): Promise<ApiResponse<Document>> {
    return client.get(`${BASE_URL}/${id}/status`)
}

// 获取支持的文件类型
export async function getSupportedTypes(): Promise<ApiResponse<SupportedType[]>> {
    return client.get(`${BASE_URL}/supported-types`)
}