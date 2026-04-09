import client from './client'
import {ApiResponse} from '@/types/api'

export interface LoginRequest {
    username: string
    password: string
}

export interface LoginResponse {
    token: string
    username: string
}

// 登录（前端模拟，后端暂未实现）
export async function login(request: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    // 模拟登录验证
    if (request.username === 'admin' && request.password === 'admin123') {
        const response: LoginResponse = {
            token: 'mock-token-' + Date.now(),
            username: request.username,
        }
        return {success: true, data: response}
    }
    return {success: false, data: {} as LoginResponse, message: '用户名或密码错误'}
}

// 登出
export function logout(): void {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
}