// 消息角色
export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM'

// 向量搜索结果（RAG 检索来源）
export interface VectorSearchResult {
    chunkId: string;
    documentId: string;
    documentName: string;
    content: string;
    score: number;
    chunkIndex?: number;
}

// 对话会话
export interface ChatSession {
    id: string;
    title?: string;
    modelId?: string;
    promptId?: string;
    systemMessage?: string;
    messageCount: number;
    isActive: boolean;
    // Agent 关联字段
    agentId?: string;
    agentName?: string;
    // RAG 配置字段
    enableRag?: boolean;
    knowledgeBaseId?: string;
    ragTopK?: number;
    ragThreshold?: number;
    ragStrategy?: string;
    ragEmbeddingModelId?: string;
    createdAt: string;
    updatedAt: string;
    createdBy: string;
}

// 对话消息
export interface ChatMessage {
    id: string;
    sessionId: string;
    role: MessageRole;
    content: string;
    modelName?: string;
    tokenCount?: number;
    latencyMs?: number;
    isError: boolean;
    errorMessage?: string;
    // RAG 检索来源
    sources?: VectorSearchResult[];
    // 工具调用状态（Agent 流式执行时使用）
    toolCalls?: ToolCallDisplay[];
    // 工具调用记录（从后端返回的已保存数据）
    toolCallRecords?: ToolCallRecord[];
    createdAt: string;
}

// 工具调用显示状态
export interface ToolCallDisplay {
    toolName: string;
    args?: Record<string, unknown>;
    result?: unknown;
    status: 'pending' | 'running' | 'completed' | 'error';
    durationMs?: number;
    errorMessage?: string;
}

// 创建会话请求
export interface CreateSessionRequest {
    title?: string;
    modelId?: string;
    promptId?: string;
    systemMessage?: string;
    // Agent 关联字段
    agentId?: string;
    // RAG 配置字段
    enableRag?: boolean;
    knowledgeBaseId?: string;
    ragTopK?: number;
    ragThreshold?: number;
    ragStrategy?: string;
    ragEmbeddingModelId?: string;
}

// 发送消息请求
export interface SendMessageRequest {
    sessionId: string;
    content: string;
    modelId?: string;
}

// 消息列表响应
export interface MessageListResponse {
    sessionId: string;
    messages: ChatMessage[];
    total: number;
}

// 工具调用记录（从后端返回）
export interface ToolCallRecord {
    toolId?: string;
    toolName: string;
    args?: Record<string, unknown>;
    result?: unknown;
    durationMs?: number;
    success?: boolean;
    errorMessage?: string;
}