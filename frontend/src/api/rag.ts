import client from './client'
import {ApiResponse} from '@/types/api'
import {RagChatRequest, RagChatResponse, VectorSearchRequest, VectorSearchResult} from '@/types/rag'

const VECTOR_URL = '/vector'
const RAG_URL = '/rag'

// 向量搜索
export async function vectorSearch(data: VectorSearchRequest): Promise<ApiResponse<VectorSearchResult[]>> {
    return client.post(`${VECTOR_URL}/search`, data)
}

// RAG 对话（同步）
export async function ragChat(data: RagChatRequest): Promise<ApiResponse<RagChatResponse>> {
    return client.post(`${RAG_URL}/chat`, data)
}

// RAG 流式对话（SSE）
export async function ragChatStream(
    data: RagChatRequest,
    onChunk: (text: string) => void,
    onComplete: (sessionId?: string) => void,
    onError: (error: Error) => void
): Promise<void> {
    try {
        const response = await fetch('/api/v1/rag/chat/stream', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
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
        let sessionId: string | undefined = undefined

        while (true) {
            const {done, value} = await reader.read()
            if (done) break

            buffer += decoder.decode(value, {stream: true})

            // 解析 SSE 格式: "data: xxx\n"
            const lines = buffer.split('\n')
            buffer = lines.pop() || ''

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const content = line.slice(5).trim()
                    if (content) {
                        // 检查是否是 sessionId 消息（格式: SESSION_ID:xxx）
                        if (content.startsWith('SESSION_ID:')) {
                            sessionId = content.slice('SESSION_ID:'.length)
                        } else {
                            fullContent += content
                            onChunk(fullContent)
                        }
                    }
                }
            }
        }

        // 处理剩余 buffer
        if (buffer.startsWith('data:')) {
            const content = buffer.slice(5).trim()
            if (content) {
                if (content.startsWith('SESSION_ID:')) {
                    sessionId = content.slice('SESSION_ID:'.length)
                } else {
                    fullContent += content
                    onChunk(fullContent)
                }
            }
        }

        onComplete(sessionId)
    } catch (error) {
        onError(error instanceof Error ? error : new Error('Unknown error'))
    }
}