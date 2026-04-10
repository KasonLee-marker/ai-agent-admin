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
    query: string;
    documentIds?: string[];
    topK?: number;
    sessionId?: string;
}

// RAG 对话响应
export interface RagChatResponse {
    answer: string;
    sources: RagSource[];
    sessionId: string;
}

// RAG 来源
export interface RagSource {
    chunkId: string;
    documentId: string;
    documentName: string;
    content: string;
    score: number;
}