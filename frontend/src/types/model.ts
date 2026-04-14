// 模型配置
export interface ModelConfig {
    id: string;
    name: string;
    provider: string;
    modelName: string;
    apiKey?: string;
    baseUrl?: string;
    temperature?: number;
    maxTokens?: number;
    topP?: number;
    extraParams?: string;
    isDefault: boolean;
    isDefaultEmbedding: boolean;
    isActive: boolean;
    healthStatus: 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN';
    modelType: 'CHAT' | 'EMBEDDING';
    /** 向量维度（仅 EMBEDDING 类型有效） */
    embeddingDimension?: number;
    /** 向量表名（仅 EMBEDDING 类型有效） */
    embeddingTableName?: string;
    lastHealthCheck?: string;
    createdAt: string;
    updatedAt: string;
    createdBy?: string;
}

// 供应商信息
export interface ProviderInfo {
    name: string;
    displayName: string;
    defaultBaseUrl: string;
    /** 模型类型：CHAT 或 EMBEDDING */
    modelType: string;
    builtinModels: BuiltinModel[];
}

// 内置模型
export interface BuiltinModel {
    name: string;
    displayName: string;
    description: string;
}

// 创建请求
export interface CreateModelRequest {
    name: string;
    provider: string;
    modelName: string;
    apiKey?: string;
    baseUrl?: string;
    temperature?: number;
    maxTokens?: number;
    topP?: number;
}

// 更新请求
export interface UpdateModelRequest {
    name?: string;
    modelName?: string;
    apiKey?: string;
    baseUrl?: string;
    temperature?: number;
    maxTokens?: number;
    topP?: number;
    isActive?: boolean;
}