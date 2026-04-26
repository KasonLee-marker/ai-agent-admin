import client from './client'
import {ApiResponse} from '@/types/api'
import {
    Agent,
    AgentExecuteRequest,
    AgentExecuteResponse,
    AgentExecutionLog,
    AgentStatusRequest,
    AgentStreamingEvent,
    CreateAgentRequest,
    Tool,
    ToolBinding,
    ToolBindingRequest,
    UpdateAgentRequest
} from '@/types/agent'

/**
 * Agent API 调用
 */

// Agent CRUD
export async function listAgents(params?: {
    status?: string;
    modelId?: string;
    keyword?: string
}): Promise<ApiResponse<Agent[]>> {
    return client.get('/agents', {params})
}

export async function getAgent(id: string): Promise<ApiResponse<Agent>> {
    return client.get(`/agents/${id}`)
}

export async function createAgent(data: CreateAgentRequest): Promise<ApiResponse<Agent>> {
    return client.post('/agents', data)
}

export async function updateAgent(id: string, data: UpdateAgentRequest): Promise<ApiResponse<Agent>> {
    return client.put(`/agents/${id}`, data)
}

export async function deleteAgent(id: string): Promise<ApiResponse<void>> {
    return client.delete(`/agents/${id}`)
}

// Agent 状态管理
export async function updateAgentStatus(id: string, data: AgentStatusRequest): Promise<ApiResponse<Agent>> {
    return client.put(`/agents/${id}/status`, data)
}

// Agent-Tool 绑定
export async function bindTool(agentId: string, data: ToolBindingRequest): Promise<ApiResponse<ToolBinding>> {
    return client.post(`/agents/${agentId}/tools`, data)
}

export async function unbindTool(agentId: string, toolId: string): Promise<ApiResponse<void>> {
    return client.delete(`/agents/${agentId}/tools/${toolId}`)
}

export async function updateToolBinding(agentId: string, toolId: string, data: ToolBindingRequest): Promise<ApiResponse<ToolBinding>> {
    return client.put(`/agents/${agentId}/tools/${toolId}`, data)
}

export async function getAgentTools(agentId: string): Promise<ApiResponse<ToolBinding[]>> {
    return client.get(`/agents/${agentId}/tools`)
}

// Agent 执行
export async function executeAgent(agentId: string, data: AgentExecuteRequest): Promise<ApiResponse<AgentExecuteResponse>> {
    return client.post(`/agents/${agentId}/execute`, data)
}

// Agent 执行日志
export async function getExecutionLogs(agentId: string, limit?: number): Promise<ApiResponse<AgentExecutionLog[]>> {
    return client.get(`/agents/${agentId}/logs`, {params: {limit}})
}

export async function getExecutionLogDetail(agentId: string, logId: string): Promise<ApiResponse<AgentExecutionLog>> {
    return client.get(`/agents/${agentId}/logs/${logId}`)
}

/**
 * Tool API 调用
 */

export async function listTools(params?: { type?: string; category?: string }): Promise<ApiResponse<Tool[]>> {
    return client.get('/tools', {params})
}

export async function getBuiltinTools(): Promise<ApiResponse<Tool[]>> {
    return client.get('/tools/builtin')
}

export async function getTool(id: string): Promise<ApiResponse<Tool>> {
    return client.get(`/tools/${id}`)
}

/**
 * 流式执行 Agent
 * <p>
 * 使用 SSE 接收实时事件，解析不同事件类型：
 * - MODEL_OUTPUT: 模型响应文本
 * - TOOL_START: 工具调用开始
 * - TOOL_END: 工具调用完成
 * - DONE: 执行完成
 * </p>
 *
 * @param agentId Agent ID
 * @param data 执行请求
 * @param onEvent 事件回调
 * @param onComplete 完成回调
 * @param onError 错误回调
 */
export async function executeAgentStream(
    agentId: string,
    data: AgentExecuteRequest,
    onEvent: (event: AgentStreamingEvent) => void,
    onComplete: () => void,
    onError: (error: Error) => void
): Promise<void> {
    try {
        const response = await fetch(`/api/v1/agents/${agentId}/execute/stream`, {
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

        while (true) {
            const {done, value} = await reader.read()
            if (done) break

            buffer += decoder.decode(value, {stream: true})

            // 解析 SSE 格式: "data: xxx\n" 或 "data:data: xxx\n"
            const lines = buffer.split('\n')
            buffer = lines.pop() || ''

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    // 处理可能的双重 "data:" 前缀
                    let content = line.slice(5).trim()
                    if (content.startsWith('data:')) {
                        content = content.slice(5).trim()
                    }
                    if (content) {
                        try {
                            const event: AgentStreamingEvent = JSON.parse(content)
                            onEvent(event)
                        } catch (e) {
                            console.error('Failed to parse event:', content, e)
                        }
                    }
                }
            }
        }

        // 处理剩余 buffer
        if (buffer.startsWith('data:')) {
            // 处理可能的双重 "data:" 前缀
            let content = buffer.slice(5).trim()
            if (content.startsWith('data:')) {
                content = content.slice(5).trim()
            }
            if (content) {
                try {
                    const event: AgentStreamingEvent = JSON.parse(content)
                    onEvent(event)
                } catch (e) {
                    console.error('Failed to parse event:', buffer, e)
                }
            }
        }

        onComplete()
    } catch (error) {
        onError(error instanceof Error ? error : new Error('Unknown error'))
    }
}