export interface McpServer {
    id: string
    name: string
    description?: string
    transportType: 'stdio' | 'sse'
    runtimeMode?: 'LOCAL' | 'DOCKER'
    command?: string
    url?: string
    args?: string[]
    env?: Record<string, string>
    status: string
    processStatus?: string
    containerId?: string
    processId?: number
    lastLog?: string
    createdAt?: string
    updatedAt?: string
    toolCount?: number
}

export interface CreateMcpServerRequest {
    name: string
    description?: string
    transportType?: 'stdio' | 'sse'
    runtimeMode?: 'LOCAL' | 'DOCKER'
    command?: string
    url?: string
    args?: string[]
    env?: Record<string, string>
}

export interface McpTool {
    name: string
    description: string
    inputSchema: Record<string, unknown>
    serverName: string
}