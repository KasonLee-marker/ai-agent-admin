// 文档
export interface Document {
    id: string;
    name: string;
    contentType: string;
    totalChunks: number;
    status: DocumentStatus;
    chunkStrategy?: 'FIXED_SIZE' | 'PARAGRAPH';
    chunkSize?: number;
    chunkOverlap?: number;
    chunksCreated?: number;
    chunksEmbedded?: number;
    embeddingModelId?: string;
    embeddingModelName?: string;
    embeddingDimension?: number;
    errorMessage?: string;
    metadata?: Record<string, unknown>;
    createdAt: string;
    updatedAt: string;
}

export type DocumentStatus = 'PROCESSING' | 'CHUNKED' | 'EMBEDDING' | 'COMPLETED' | 'FAILED' | 'DELETED'

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