package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.RagChatResponse;
import reactor.core.publisher.Flux;

public interface RagService {

    /**
     * 执行 RAG 对话（同步）
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