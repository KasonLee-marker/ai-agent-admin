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
    createdAt: string;
}

// 创建会话请求
export interface CreateSessionRequest {
    title?: string;
    modelId?: string;
    promptId?: string;
    systemMessage?: string;
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