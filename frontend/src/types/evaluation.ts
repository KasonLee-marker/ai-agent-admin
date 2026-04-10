// 评估任务
export interface EvaluationJob {
    id: string;
    name: string;
    description?: string;
    datasetId: string;
    promptId?: string;
    modelId?: string;
    status: EvaluationStatus;
    totalItems: number;
    completedItems: number;
    successItems: number;
    failedItems: number;
    createdAt: string;
    updatedAt: string;
    createdBy?: string;
}

export type EvaluationStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

// 评估结果
export interface EvaluationResult {
    id: string;
    jobId: string;
    itemIndex: number;
    input: string;
    expectedOutput?: string;
    actualOutput: string;
    passed?: boolean;
    score?: number;
    latencyMs?: number;
    tokenCount?: number;
    error?: string;
    createdAt: string;
}

// 评估指标统计
export interface EvaluationMetrics {
    totalItems: number;
    passedItems: number;
    failedItems: number;
    averageScore?: number;
    averageLatencyMs?: number;
    totalTokenCount?: number;
    passRate?: number;
}

// 创建评估任务请求
export interface CreateEvaluationRequest {
    name: string;
    description?: string;
    datasetId: string;
    promptId?: string;
    modelId?: string;
}

// 更新评估任务请求
export interface UpdateEvaluationRequest {
    name?: string;
    description?: string;
}

// 对比请求
export interface CompareRequest {
    jobIds: string[];
}