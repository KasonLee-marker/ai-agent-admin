package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.RagChatResponse;
import reactor.core.publisher.Flux;

/**
 * RAG（检索增强生成）服务接口
 * <p>
 * 提供 RAG 对话的核心功能：
 * <ul>
 *   <li>知识库文档检索（向量检索 + BM25 检索）</li>
 *   <li>检索结果重排序（Rerank）</li>
 *   <li>构建带上下文的 Prompt</li>
 *   <li>调用 AI 模型生成回答</li>
 *   <li>流式响应输出</li>
 * </ul>
 * </p>
 * <p>
 * RAG 流程：
 * <ol>
 *   <li>用户提问 → 计算 Embedding</li>
 *   <li>向量检索 → 获取候选文档</li>
 *   <li>Rerank 重排序 → 精选 topK 文档</li>
 *   <li>构建 Prompt → 包含检索上下文</li>
 *   <li>AI 模型生成回答</li>
 * </ol>
 * </p>
 *
 * @see RagChatRequest
 * @see RagChatResponse
 * @see RagSessionService
 */
public interface RagService {

    /**
     * 执行 RAG 对话（同步）
     * <p>
     * 检索知识库文档，构建带上下文的 Prompt，调用 AI 模型生成回答。
     * </p>
     *
     * @param request RAG 对话请求，包含问题、知识库ID、模型ID等
     * @return RAG 对话响应，包含回答和检索来源
     */
    RagChatResponse chat(RagChatRequest request);

    /**
     * 执行 RAG 流式对话（SSE 格式）
     * <p>
     * 返回 Flux<String> 流，每个元素为响应内容片段。
     * 检索阶段为阻塞操作，在 boundedElastic 线程池执行。
     * </p>
     *
     * @param request RAG 对话请求
     * @return SSE 格式的流式响应
     */
    Flux<String> chatStream(RagChatRequest request);
}