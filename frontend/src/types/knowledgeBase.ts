/**
 * 知识库实体接口
 */
export interface KnowledgeBase {
    id: string;
    name: string;
    description?: string;
    defaultEmbeddingModelId: string;
    defaultEmbeddingModelName?: string;
    documentCount: number;
    chunkCount: number;
    reindexStatus?: ReindexStatus;
    reindexProgressCurrent?: number;
    reindexProgressTotal?: number;
    reindexErrorMessage?: string;
    createdAt: string;
    updatedAt: string;
    createdBy: string;
}

/**
 * 知识库创建/更新请求接口
 */
export interface KnowledgeBaseRequest {
    name: string;
    description?: string;
    defaultEmbeddingModelId: string;
}

/**
 * 重索引状态枚举
 */
export type ReindexStatus = 'NONE' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';

/**
 * 重索引进度响应接口
 */
export interface ReindexProgressResponse {
    knowledgeBaseId: string;
    status: ReindexStatus;
    current: number;
    total: number;
    percentage: number;
    startedAt?: string;
    completedAt?: string;
    errorMessage?: string;
    currentEmbeddingModelId?: string;
    newEmbeddingModelId?: string;
}

/**
 * 重索引请求接口
 */
export interface ReindexRequest {
    newEmbeddingModelId: string;
}