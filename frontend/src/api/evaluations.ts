import client from './client'
import {ApiResponse, PageParams, PageResponse} from '@/types/api'
import {
    CompareRequest,
    CreateEvaluationRequest,
    EvaluationJob,
    EvaluationMetrics,
    EvaluationResult,
    UpdateEvaluationRequest
} from '@/types/evaluation'

const BASE_URL = '/evaluations'

// 获取评估任务列表
export async function listEvaluations(params?: PageParams): Promise<ApiResponse<PageResponse<EvaluationJob>>> {
    return client.get(BASE_URL, {params})
}

// 获取评估任务详情
export async function getEvaluation(id: string): Promise<ApiResponse<EvaluationJob>> {
    return client.get(`${BASE_URL}/${id}`)
}

// 创建评估任务
export async function createEvaluation(data: CreateEvaluationRequest): Promise<ApiResponse<EvaluationJob>> {
    return client.post(BASE_URL, data)
}

// 更新评估任务
export async function updateEvaluation(id: string, data: UpdateEvaluationRequest): Promise<ApiResponse<EvaluationJob>> {
    return client.put(`${BASE_URL}/${id}`, data)
}

// 删除评估任务
export async function deleteEvaluation(id: string): Promise<ApiResponse<void>> {
    return client.delete(`${BASE_URL}/${id}`)
}

// 运行评估
export async function runEvaluation(id: string): Promise<ApiResponse<EvaluationJob>> {
    return client.post(`${BASE_URL}/${id}/run`)
}

// 取消评估
export async function cancelEvaluation(id: string): Promise<ApiResponse<EvaluationJob>> {
    return client.post(`${BASE_URL}/${id}/cancel`)
}

// 重新评估（删除之前的结果并重新运行）
export async function rerunEvaluation(id: string): Promise<ApiResponse<EvaluationJob>> {
    return client.post(`${BASE_URL}/${id}/rerun`)
}

// 获取评估结果
export async function getEvaluationResults(id: string, params?: PageParams): Promise<ApiResponse<PageResponse<EvaluationResult>>> {
    return client.get(`${BASE_URL}/${id}/results`, {params})
}

// 获取评估指标
export async function getEvaluationMetrics(id: string): Promise<ApiResponse<EvaluationMetrics>> {
    return client.get(`${BASE_URL}/${id}/metrics`)
}

// 对比评估结果
export async function compareEvaluations(data: CompareRequest): Promise<ApiResponse<Record<string, EvaluationMetrics>>> {
    return client.post(`${BASE_URL}/compare`, data)
}