import client from './client'
import {ApiResponse} from '@/types/api'
import {McpServer, McpTool} from '@/types/mcp'
import {Tool} from '@/types/agent'

/**
 * MCP Server API
 */

export async function listMcpServers(): Promise<ApiResponse<McpServer[]>> {
    return client.get('/mcp-servers')
}

export async function getMcpServer(id: string): Promise<ApiResponse<McpServer>> {
    return client.get(`/mcp-servers/${id}`)
}

export async function createMcpServerFromJson(configJson: string): Promise<ApiResponse<McpServer[]>> {
    // 发送 JSON string，后端解析
    return client.post('/mcp-servers/from-json', {configJson})
}

export async function updateMcpServerFromJson(id: string, configJson: string): Promise<ApiResponse<McpServer>> {
    return client.put(`/mcp-servers/${id}/from-json`, {configJson})
}

export async function updateMcpServer(id: string, data: { description?: string }): Promise<ApiResponse<McpServer>> {
    return client.put(`/mcp-servers/${id}`, data)
}

export async function deleteMcpServer(id: string): Promise<ApiResponse<void>> {
    return client.delete(`/mcp-servers/${id}`)
}

export async function refreshMcpTools(id: string): Promise<ApiResponse<McpTool[]>> {
    return client.post(`/mcp-servers/${id}/refresh-tools`)
}

export async function getMcpServerTools(id: string): Promise<ApiResponse<Tool[]>> {
    return client.get(`/mcp-servers/${id}/tools`)
}