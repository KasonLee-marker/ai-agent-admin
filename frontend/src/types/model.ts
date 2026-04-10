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
    isActive: boolean;
    healthStatus: 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN';
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