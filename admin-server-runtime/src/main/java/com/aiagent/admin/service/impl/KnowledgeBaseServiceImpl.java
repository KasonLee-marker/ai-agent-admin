package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.KnowledgeBaseRequest;
import com.aiagent.admin.api.dto.KnowledgeBaseResponse;
import com.aiagent.admin.domain.entity.KnowledgeBase;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.domain.repository.DocumentRepository;
import com.aiagent.admin.domain.repository.KnowledgeBaseRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.KnowledgeBaseService;
import com.aiagent.admin.service.ReIndexService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库服务实现类
 * <p>
 * 提供知识库管理功能：
 * <ul>
 *   <li>创建、查询、更新、删除知识库</li>
 *   <li>统计知识库文档和分块数量</li>
 *   <li>验证知识库删除前置条件</li>
 *   <li>检测 Embedding 模型变更并触发重索引</li>
 * </ul>
 * </p>
 *
 * @see KnowledgeBaseService
 * @see KnowledgeBase
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final ReIndexService reIndexService;

    @Override
    @Transactional
    public KnowledgeBaseResponse createKnowledgeBase(KnowledgeBaseRequest request, String createdBy) {
        // 验证默认 Embedding 模型（必填）
        ModelConfig embeddingModel = modelConfigRepository.findById(request.getDefaultEmbeddingModelId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Embedding model not found: " + request.getDefaultEmbeddingModelId()));

        // 验证模型是否为 Embedding 类型
        if (!isEmbeddingModel(embeddingModel)) {
            throw new IllegalArgumentException(
                    "指定的模型不是 Embedding 类型，请选择 Embedding 模型（如 text-embedding-v4）");
        }

        // 创建知识库
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id("kb_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .name(request.getName())
                .description(request.getDescription())
                .defaultEmbeddingModelId(request.getDefaultEmbeddingModelId())
                .documentCount(0)
                .chunkCount(0)
                .createdBy(createdBy)
                .reindexStatus(KnowledgeBase.ReindexStatus.NONE)
                .build();

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
        log.info("Created knowledge base: {} with embedding model {} by {}",
                knowledgeBase.getName(), embeddingModel.getName(), createdBy);

        return toResponse(knowledgeBase);
    }

    @Override
    @Transactional
    public KnowledgeBaseResponse updateKnowledgeBase(String id, KnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found: " + id));

        // 验证新 Embedding 模型（必填）
        ModelConfig newEmbeddingModel = modelConfigRepository.findById(request.getDefaultEmbeddingModelId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Embedding model not found: " + request.getDefaultEmbeddingModelId()));

        // 验证模型是否为 Embedding 类型
        if (!isEmbeddingModel(newEmbeddingModel)) {
            throw new IllegalArgumentException(
                    "指定的模型不是 Embedding 类型，请选择 Embedding 模型");
        }

        // 更新基本字段
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());

        // 检测 Embedding 模型是否变更
        String oldEmbeddingModelId = knowledgeBase.getDefaultEmbeddingModelId();
        String newEmbeddingModelId = request.getDefaultEmbeddingModelId();
        boolean embeddingModelChanged = oldEmbeddingModelId != null
                && !oldEmbeddingModelId.equals(newEmbeddingModelId);

        // 更新 Embedding 模型 ID
        knowledgeBase.setDefaultEmbeddingModelId(newEmbeddingModelId);

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
        log.info("Updated knowledge base: {}", knowledgeBase.getName());

        // 如果 Embedding 模型变更且有文档，触发重索引
        if (embeddingModelChanged && knowledgeBase.getDocumentCount() > 0) {
            log.info("Embedding model changed from {} to {}, triggering reindex for knowledge base {}",
                    oldEmbeddingModelId, newEmbeddingModelId, id);
            reIndexService.startReindex(id, newEmbeddingModelId);
        }

        return toResponse(knowledgeBase);
    }

    @Override
    public KnowledgeBaseResponse getKnowledgeBase(String id) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found: " + id));
        return toResponse(knowledgeBase);
    }

    @Override
    public Page<KnowledgeBaseResponse> listKnowledgeBases(String createdBy, Pageable pageable) {
        return knowledgeBaseRepository.findByCreatedBy(createdBy, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<KnowledgeBaseResponse> listAllKnowledgeBases() {
        return knowledgeBaseRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(String id) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found: " + id));

        // 检查是否有文档
        if (documentRepository.existsByKnowledgeBaseId(id)) {
            throw new IllegalStateException("Cannot delete knowledge base with documents. " +
                    "Please delete all documents first.");
        }

        knowledgeBaseRepository.delete(knowledgeBase);
        log.info("Deleted knowledge base: {}", knowledgeBase.getName());
    }

    @Override
    @Transactional
    public void updateStatistics(String knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found: " + knowledgeBaseId));

        // 计算文档数量
        long docCount = documentRepository.countByKnowledgeBaseId(knowledgeBaseId);

        // 计算分块总数（通过所有文档的分块数求和）
        int chunkCount = documentRepository.findByKnowledgeBaseId(knowledgeBaseId)
                .stream()
                .mapToInt(doc -> doc.getChunksCreated() != null ? doc.getChunksCreated() : 0)
                .sum();

        knowledgeBase.setDocumentCount((int) docCount);
        knowledgeBase.setChunkCount(chunkCount);

        knowledgeBaseRepository.save(knowledgeBase);
        log.debug("Updated statistics for knowledge base {}: {} docs, {} chunks",
                knowledgeBaseId, docCount, chunkCount);
    }

    /**
     * 验证模型是否为 Embedding 类型
     *
     * @param modelConfig 模型配置
     * @return true 如果是 Embedding 类型模型
     */
    private boolean isEmbeddingModel(ModelConfig modelConfig) {
        return modelConfig.getProvider() != null
                && modelConfig.getProvider().getModelType() == ModelProvider.ModelType.EMBEDDING;
    }

    /**
     * 将知识库实体转换为响应 DTO
     *
     * @param knowledgeBase 知识库实体
     * @return 知识库响应 DTO
     */
    private KnowledgeBaseResponse toResponse(KnowledgeBase knowledgeBase) {
        // 获取默认 Embedding 模型名称
        String modelName = null;
        if (knowledgeBase.getDefaultEmbeddingModelId() != null) {
            ModelConfig model = modelConfigRepository.findById(knowledgeBase.getDefaultEmbeddingModelId())
                    .orElse(null);
            if (model != null) {
                modelName = model.getName();
            }
        }

        return KnowledgeBaseResponse.builder()
                .id(knowledgeBase.getId())
                .name(knowledgeBase.getName())
                .description(knowledgeBase.getDescription())
                .defaultEmbeddingModelId(knowledgeBase.getDefaultEmbeddingModelId())
                .defaultEmbeddingModelName(modelName)
                .documentCount(knowledgeBase.getDocumentCount())
                .chunkCount(knowledgeBase.getChunkCount())
                .reindexStatus(knowledgeBase.getReindexStatus())
                .reindexProgressCurrent(knowledgeBase.getReindexProgressCurrent())
                .reindexProgressTotal(knowledgeBase.getReindexProgressTotal())
                .reindexErrorMessage(knowledgeBase.getReindexErrorMessage())
                .createdAt(knowledgeBase.getCreatedAt())
                .updatedAt(knowledgeBase.getUpdatedAt())
                .createdBy(knowledgeBase.getCreatedBy())
                .build();
    }
}