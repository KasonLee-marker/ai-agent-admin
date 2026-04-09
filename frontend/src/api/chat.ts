import client from './client'
import {ApiResponse, PageResponse, PageParams} from '@/types/api'
import {ChatSession, ChatMessage, CreateSessionRequest, SendMessageRequest, MessageListResponse} from '@/types/chat'

const BASE_URL = '/chat'

// 创建会话
export async function createSession(data: CreateSessionRequest): Promise<ApiResponse<ChatSession>> {
    return client.post(`${BASE_URL}/sessions`, data)
}

// 获取会话列表
export async function listSessions(params?: PageParams & {
    keyword?: string
}): Promise<ApiResponse<PageResponse<ChatSession>>> {
    return client.get(`${BASE_URL}/sessions`, {params})
}

// 获取会话详情
export async function getSession(id: string): Promise<ApiResponse<ChatSession>> {
    return client.get(`${BASE_URL}/sessions/${id}`)
}

// 删除会话
export async function deleteSession(id: string): Promise<ApiResponse<void>> {
    return client.delete(`${BASE_URL}/sessions/${id}`)
}

// 发送消息
export async function sendMessage(data: SendMessageRequest): Promise<ApiResponse<ChatMessage>> {
    return client.post(`${BASE_URL}/messages`, data)
}

// 获取会话消息
export async function getSessionMessages(sessionId: string): Promise<ApiResponse<MessageListResponse>> {
    return client.get(`${BASE_URL}/sessions/${sessionId}/messages`)
}

// 获取对话历史
export async function getConversationHistory(sessionId: string): Promise<ApiResponse<MessageListResponse>> {
    return client.get(`${BASE_URL}/sessions/${sessionId}/history`)
}