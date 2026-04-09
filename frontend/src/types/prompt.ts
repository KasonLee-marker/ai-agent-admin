// Prompt 模板
export interface PromptTemplate {
    id: string;
    name: string;
    content: string;
    description?: string;
    category?: string;
    tags?: string[];
    version: number;
    variables?: string[];
    createdAt: string;
    updatedAt: string;
    createdBy?: string;
}

// Prompt 版本
export interface PromptVersion {
    id: string;
    promptId: string;
    version: number;
    content: string;
    createdAt: string;
    createdBy?: string;
}

// 创建请求
export interface PromptTemplateCreateRequest {
    name: string;
    content: string;
    description?: string;
    category?: string;
    tags?: string[];
}

// 更新请求
export interface PromptTemplateUpdateRequest {
    name?: string;
    content?: string;
    description?: string;
    category?: string;
    tags?: string[];
}

// 回滚请求
export interface RollbackRequest {
    version: number;
}