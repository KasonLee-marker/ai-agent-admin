import client from './client'
import {ApiResponse, PageParams, PageResponse} from '@/types/api'
import {ChatMessage, ChatSession, CreateSessionRequest, MessageListResponse, SendMessageRequest} from '@/types/chat'

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

// 流式发送消息
export async function sendMessageStream(
    data: SendMessageRequest,
    onChunk: (text: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
): Promise<void> {
    try {
        const response = await fetch('/api/v1/chat/messages/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        })

        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`)
        }

        const reader = response.body?.getReader()
        if (!reader) {
            throw new Error('No response body')
        }

        const decoder = new TextDecoder()
        let buffer = ''
        let fullContent = ''

        while (true) {
            const {done, value} = await reader.read()
            if (done) break

            buffer += decoder.decode(value, {stream: true})

            // 解析 SSE 格式: "data: xxx\n"
            const lines = buffer.split('\n')
            buffer = lines.pop() || '' // 保留不完整的行

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const content = line.slice(5).trim()
                    if (content) {
                        fullContent += content
                        onChunk(fullContent)
                    }
                }
            }
        }

        // 处理剩余 buffer
        if (buffer.startsWith('data:')) {
            const content = buffer.slice(5).trim()
            if (content) {
                fullContent += content
                onChunk(fullContent)
            }
        }

        onComplete()
    } catch (error) {
        onError(error instanceof Error ? error : new Error('Unknown error'))
    }
}