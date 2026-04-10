import client from './client'
import {ApiResponse, PageParams, PageResponse} from '@/types/api'
import {Document, DocumentChunk, SupportedType} from '@/types/document'

const BASE_URL = '/documents'

// 上传文档
export async function uploadDocument(file: File): Promise<ApiResponse<{ documentId: string, message: string }>> {
    const formData = new FormData()
    formData.append('file', file)
    return client.post(`${BASE_URL}/upload`, formData, {
        headers: {'Content-Type': 'multipart/form-data'}
    })
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
export async function getDocumentStatus(id: string): Promise<ApiResponse<{ status: string, progress: number }>> {
    return client.get(`${BASE_URL}/${id}/status`)
}

// 获取支持的文件类型
export async function getSupportedTypes(): Promise<ApiResponse<SupportedType[]>> {
    return client.get(`${BASE_URL}/supported-types`)
}