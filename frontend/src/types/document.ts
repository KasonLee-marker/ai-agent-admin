// 文档
export interface Document {
    id: string;
    name: string;
    contentType: string;
    totalChunks: number;
    status: DocumentStatus;
    metadata?: Record<string, unknown>;
    createdAt: string;
    updatedAt: string;
}

export type DocumentStatus = 'PROCESSING' | 'COMPLETED' | 'FAILED'

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

// 上传响应
export interface UploadResponse {
    documentId: string;
    message: string;
}