// 向量搜索请求
export interface VectorSearchRequest {
    query: string;
    topK?: number;
    threshold?: number;
}

// 向量搜索结果
export interface VectorSearchResult {
    chunkId: string;
    documentId: string;
    documentName: string;
    content: string;
    score: number;
    chunkIndex: number;
}

// RAG 对话请求
export interface RagChatRequest {
    question: string;  // 用户问题（后端字段名为 question）
    promptTemplateId?: string;  // 提示词模板ID（可选）
    documentIds?: string[];
    topK?: number;
    threshold?: number;  // 相似度阈值（可选）
    sessionId?: string;  // RAG会话ID（可选，用于多轮对话）
    modelId?: string;  // 对话模型ID（可选，默认使用系统默认模型）
    embeddingModelId?: string;  // Embedding模型ID（可选，用于向量检索）
    knowledgeBaseId?: string;  // 知识库ID过滤（可选）
    strategy?: string;  // 检索策略（VECTOR/BM25/HYBRID）
    enableRerank?: boolean;  // 是否启用 Rerank 重排序（可选）
    rerankModelId?: string;  // Rerank 模型 ID（可选）
}

// RAG 对话响应
export interface RagChatResponse {
    answer: string;
    sources: RagSource[];
    sessionId: string;  // 会话ID（用于多轮对话）
}

// RAG 来源
export interface RagSource {
    chunkId: string;
    documentId: string;
    documentName: string;
    content: string;
    score: number;
}

// RAG 会话
export interface RagSession {
    id: string;
    title?: string;
    knowledgeBaseId?: string;
    knowledgeBaseName?: string;
    modelId?: string;
    modelName?: string;
    embeddingModelId?: string;
    embeddingModelName?: string;
    messageCount: number;
    createdAt: string;
    updatedAt: string;
    createdBy: string;
}

// RAG 消息
export interface RagMessage {
    id: string;
    sessionId: string;
    role: 'USER' | 'ASSISTANT';
    content: string;
    sources?: VectorSearchResult[];
    modelName?: string;
    latencyMs?: number;
    isError?: boolean;
    errorMessage?: string;
    createdAt: string;
}