// 文档
export interface Document {
    id: string;
    name: string;
    contentType: string;
    totalChunks: number;
    status: DocumentStatus;
    chunkStrategy?: 'FIXED_SIZE' | 'PARAGRAPH' | 'SENTENCE' | 'RECURSIVE' | 'SEMANTIC';
    chunkSize?: number;
    chunkOverlap?: number;
    chunksCreated?: number;
    chunksEmbedded?: number;
    embeddingModelId?: string;
    embeddingModelName?: string;
    embeddingDimension?: number;
    knowledgeBaseId?: string;
    knowledgeBaseName?: string;
    errorMessage?: string;
    metadata?: Record<string, unknown>;
    createdAt: string;
    updatedAt: string;
    // 语义切分进度
    semanticProgressCurrent?: number;
    semanticProgressTotal?: number;
}

export type DocumentStatus =
    'PROCESSING'
    | 'SEMANTIC_PROCESSING'
    | 'CHUNKED'
    | 'EMBEDDING'
    | 'COMPLETED'
    | 'FAILED'
    | 'DELETED'

// 语义切分进度响应
export interface SemanticProgress {
    status: DocumentStatus;
    current: number;
    total: number;
    percentage: number;
    errorMessage?: string;
}

// 文档分块
export interface DocumentChunk {
    id: string;
    documentId: string;
    chunkIndex: number;
    content: string;
    metadata?: Record<string, unknown>;
    createdAt: string;
}

// 支持的文件类型
export interface SupportedType {
    contentType: string;
    extension: string;
    displayName: string;
}