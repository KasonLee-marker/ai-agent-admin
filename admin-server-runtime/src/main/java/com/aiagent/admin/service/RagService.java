package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.RagChatResponse;

public interface RagService {

    /**
     * 执行 RAG 对话
     */
    RagChatResponse chat(RagChatRequest request);
}