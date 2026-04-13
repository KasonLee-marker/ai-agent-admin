// 数据集
export interface Dataset {
    id: string;
    name: string;
    description?: string;
    version: number;
    itemCount: number;
    status: DatasetStatus;
    createdAt: string;
    updatedAt: string;
    createdBy?: string;
}

export type DatasetStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'

// 数据项
export interface DatasetItem {
    id: string;
    datasetId: string;
    input: string;
    output?: string;
    /** 期望检索到的文档ID列表（用于RAG评估） */
    expectedDocIds?: string;  // JSON数组格式
    /** 参考上下文（用于RAG评估） */
    context?: string;
    metadata?: Record<string, unknown>;
    createdAt: string;
}

// 创建数据集请求
export interface CreateDatasetRequest {
    name: string;
    description?: string;
}

// 更新数据集请求
export interface UpdateDatasetRequest {
    name?: string;
    description?: string;
    status?: DatasetStatus;
}

// 创建数据项请求
export interface CreateDatasetItemRequest {
    input: string;
    output?: string;
    /** 期望检索到的文档ID列表（用于RAG评估） */
    expectedDocIds?: string;
    /** 参考上下文（用于RAG评估） */
    context?: string;
    metadata?: Record<string, unknown>;
}

// 导入请求
export interface ImportDatasetRequest {
    name: string;
    description?: string;
    items: DatasetItemImport[];
}

export interface DatasetItemImport {
    input: string;
    output?: string;
    metadata?: Record<string, unknown>;
}