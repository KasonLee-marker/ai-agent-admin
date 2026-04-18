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

    /**
     * 文档 ID
     */
    private final String documentId;

    /**
     * Embedding 模型配置
     */
    private final ModelConfig embeddingConfig;

    /**
     * 构造 Embedding 开始事件
     *
     * @param documentId      文档 ID
     * @param embeddingConfig Embedding 模型配置
     */
    public EmbeddingStartEvent(String documentId, ModelConfig embeddingConfig) {
        this.documentId = documentId;
        this.embeddingConfig = embeddingConfig;
    }

    /**
     * 获取文档 ID
     *
     * @return 文档 ID
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * 获取 Embedding 模型配置
     *
     * @return Embedding 模型配置
     */
    public ModelConfig getEmbeddingConfig() {
        return embeddingConfig;
    }
}