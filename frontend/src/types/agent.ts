/**
 * Agent 类型定义
 */
export interface Agent {
    id: string
    name: string
    description?: string
    version: string
    modelId: string
    modelName?: string
    systemPrompt?: string
    config: AgentConfig
    status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
    tools: ToolBinding[]
    createdAt?: string
    updatedAt?: string
}

export interface AgentConfig {
    temperature?: number
    maxTokens?: number
    topP?: number
}

export interface ToolBinding {
    id: string
    toolId: string
    toolName: string
    toolDescription?: string
    enabled: boolean
    config: Record<string, unknown>
}

export interface Tool {
    id: string
    name: string
    description: string
    category: string
    type: 'BUILTIN' | 'CUSTOM' | 'MCP'
    schema: Record<string, unknown>
    executor?: string
    config: Record<string, unknown>
    createdAt?: string
}

export interface CreateAgentRequest {
    name: string
    description?: string
    modelId: string
    systemPrompt?: string
    config?: AgentConfig
    tools?: ToolBindingRequest[]
}

export interface UpdateAgentRequest {
    name?: string
    description?: string
    modelId?: string
    systemPrompt?: string
    config?: AgentConfig
}

export interface ToolBindingRequest {
    toolId: string
    enabled?: boolean
    config?: Record<string, unknown>
}

export interface AgentExecuteRequest {
    message: string
    sessionId?: string
    context?: Record<string, unknown>
}

export interface AgentExecuteResponse {
    response: string
    sessionId?: string
    toolCalls: ToolCallRecord[]
    durationMs: number
    executionLogId: string
}

export interface ToolCallRecord {
    toolId: string
    toolName: string
    args: Record<string, unknown>
    result?: unknown
    durationMs: number
    success: boolean
    errorMessage?: string
}

export interface AgentExecutionLog {
    id: string
    agentId: string
    sessionId?: string
    inputSummary: string
    outputSummary: string
    toolCallCount: number
    durationMs: number
    success: boolean
    errorMessage?: string
    createdAt: string
    toolCalls?: ToolCallRecord[]
}

export interface AgentStatusRequest {
    status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
}

/**
 * 流式事件类型枚举
 */
export type StreamingEventType =
    | 'MODEL_OUTPUT'
    | 'TOOL_START'
    | 'TOOL_END'
    | 'THINKING'
    | 'ERROR'
    | 'DONE'

/**
 * Agent 流式事件
 */
export interface AgentStreamingEvent {
    type: StreamingEventType
    content?: string
    toolCall?: ToolCallRecord
    sequence: number
    timestamp: string
}