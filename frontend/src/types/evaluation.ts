// 评估任务
export interface EvaluationJob {
    id: string;
    name: string;
    description?: string;
    datasetId: string;
    promptTemplateId?: string;
    modelConfigId?: string;
    /** 关联的知识库ID（用于RAG评估） */
    knowledgeBaseId?: string;
    /** 是否启用RAG评估模式 */
    enableRag?: boolean;
    /** Embedding 模型配置ID（用于计算语义相似度） */
    embeddingModelId?: string;
    promptTemplateVersion?: number;
    status: EvaluationStatus;
    totalItems: number;
    completedItems: number;
    successCount: number;
    failedCount: number;
    createdAt: string;
    updatedAt: string;
    createdBy?: string;
}

export type EvaluationStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

// 评估结果
export interface EvaluationResult {
    id: string;
    jobId: string;
    input: string;
    expectedOutput?: string;
    actualOutput: string;
    score?: number;
    scoreReason?: string;
    /** Embedding语义相似度（0-1） */
    semanticSimilarity?: number;
    /** 实际检索到的文档ID列表 */
    retrievedDocIds?: string;
    /** 检索评估得分 */
    retrievalScore?: number;
    /** 事实忠实度 */
    faithfulness?: number;
    latencyMs?: number;
    inputTokens?: number;
    outputTokens?: number;
    status?: string;
    errorMessage?: string;
    createdAt: string;
}

// 评估指标统计
export interface EvaluationMetrics {
    totalItems: number;
    completedItems: number;
    successCount: number;
    failedCount: number;
    averageScore?: number;
    /** 平均语义相似度 */
    averageSemanticSimilarity?: number;
    /** 平均检索得分 */
    averageRetrievalScore?: number;
    /** 平均忠实度 */
    averageFaithfulness?: number;
    averageLatencyMs?: number;
    totalTokenCount?: number;
    successRate?: number;
}

// 创建评估任务请求
export interface CreateEvaluationRequest {
    name: string;
    description?: string;
    datasetId: string;
    promptTemplateId?: string;
    modelConfigId?: string;
    /** 关联的知识库ID（用于RAG评估） */
    knowledgeBaseId?: string;
    /** 是否启用RAG评估模式 */
    enableRag?: boolean;
    /** Embedding 模型配置ID（用于计算语义相似度） */
    embeddingModelId?: string;
}

// 更新评估任务请求
export interface UpdateEvaluationRequest {
    name?: string;
    description?: string;
    datasetId?: string;
    promptTemplateId?: string;
    modelConfigId?: string;
    /** 关联的知识库ID（用于RAG评估） */
    knowledgeBaseId?: string;
    /** 是否启用RAG评估模式 */
    enableRag?: boolean;
    /** Embedding 模型配置ID（用于计算语义相似度） */
    embeddingModelId?: string;
}

// 对比请求
export interface CompareRequest {
    jobIds: string[];
}