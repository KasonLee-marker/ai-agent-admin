package com.aiagent.admin.service.event;

import com.aiagent.admin.domain.entity.ModelConfig;

/**
 * Embedding 计算开始事件
 * <p>
 * 当用户触发 Embedding 计算后触发此事件，
 * 用于异步计算文档分块的向量。
 * </p>
 */
public class EmbeddingStartEvent {

    private final String documentId;
    private final ModelConfig embeddingConfig;

    public EmbeddingStartEvent(String documentId, ModelConfig embeddingConfig) {
        this.documentId = documentId;
        this.embeddingConfig = embeddingConfig;
    }

    public String getDocumentId() {
        return documentId;
    }

    public ModelConfig getEmbeddingConfig() {
        return embeddingConfig;
    }
}