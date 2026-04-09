// API 通用响应类型
export interface ApiResponse<T = unknown> {
    success: boolean;
    data: T;
    message?: string;
}

// 分页响应
export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

// 分页请求参数
export interface PageParams {
    page?: number;
    size?: number;
}