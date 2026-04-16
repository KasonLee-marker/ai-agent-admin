package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.ReindexProgressResponse;
import com.aiagent.admin.domain.entity.KnowledgeBase;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.repository.KnowledgeBaseRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.ReIndexService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * 知识库重索引服务实现类
 * <p>
 * 负责协调重索引任务的启动、进度查询和取消。
 * 实际的异步批处理由 {@link ReIndexAsyncService} 执行。
 * </p>
 *
 * @see ReIndexService
 * @see ReIndexAsyncService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReIndexServiceImpl implements ReIndexService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final ReIndexAsyncService reIndexAsyncService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 启动知识库重索引（异步执行）
     * <p>
     * 执行流程：
     * <ol>
     *   <li>验证知识库存在且无正在进行的重索引</li>
     *   <li>验证新 Embedding 模型已健康检查（有 dimension 和 table）</li>
     *   <li>初始化进度状态</li>
     *   <li>异步执行重索引任务</li>
     * </ol>
     * </p>
     *
     * @param knowledgeBaseId     知识库 ID
     * @param newEmbeddingModelId 新 Embedding 模型 ID
     * @return 重索引进度响应
     */
    @Override
    @Transactional
    public ReindexProgressResponse startReindex(String knowledgeBaseId, String newEmbeddingModelId) {
        // 1. 验证知识库
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found: " + knowledgeBaseId));

        // 2. 检查是否有正在进行的重索引
        if (kb.getReindexStatus() == KnowledgeBase.ReindexStatus.IN_PROGRESS) {
            throw new IllegalStateException("Reindex is already in progress for knowledge base: " + knowledgeBaseId);
        }

        // 3. 验证新 Embedding 模型
        ModelConfig newModel = modelConfigRepository.findById(newEmbeddingModelId)
                .orElseThrow(() -> new EntityNotFoundException("Embedding model not found: " + newEmbeddingModelId));

        if (newModel.getEmbeddingDimension() == null || newModel.getEmbeddingTableName() == null) {
            throw new IllegalStateException("Embedding model has not been health checked. Please run health check first.");
        }

        // 4. 初始化进度状态
        kb.setReindexStatus(KnowledgeBase.ReindexStatus.IN_PROGRESS);
        kb.setReindexProgressCurrent(0);
        kb.setReindexProgressTotal(0); // 异步任务会设置总数
        kb.setReindexStartedAt(java.time.LocalDateTime.now());
        kb.setReindexCompletedAt(null);
        kb.setReindexErrorMessage(null);
        knowledgeBaseRepository.save(kb);

        log.info("Starting reindex for knowledge base {} with new embedding model {}", knowledgeBaseId, newEmbeddingModelId);

        // 5. 异步执行重索引
        reIndexAsyncService.reindexKnowledgeBaseAsync(knowledgeBaseId, newEmbeddingModelId);

        return buildProgressResponse(kb, newEmbeddingModelId);
    }

    /**
     * 获取重索引进度
     *
     * @param knowledgeBaseId 知识库 ID
     * @return 重索引进度响应
     */
    @Override
    public ReindexProgressResponse getReindexProgress(String knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found: " + knowledgeBaseId));

        return buildProgressResponse(kb, null);
    }

    /**
     * 取消正在进行的重索引
     *
     * @param knowledgeBaseId 知识库 ID
     */
    @Override
    @Transactional
    public void cancelReindex(String knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new EntityNotFoundException("Knowledge base not found: " + knowledgeBaseId));

        if (kb.getReindexStatus() != KnowledgeBase.ReindexStatus.IN_PROGRESS) {
            throw new IllegalStateException("Reindex is not in progress for knowledge base: " + knowledgeBaseId);
        }

        kb.setReindexStatus(KnowledgeBase.ReindexStatus.FAILED);
        kb.setReindexErrorMessage("Cancelled by user");
        kb.setReindexCompletedAt(java.time.LocalDateTime.now());
        knowledgeBaseRepository.save(kb);

        log.info("Cancelled reindex for knowledge base {}", knowledgeBaseId);
    }

    /**
     * 构建进度响应
     *
     * @param kb                  知识库实体
     * @param newEmbeddingModelId 新 Embedding 模型 ID（可选）
     * @return 进度响应
     */
    private ReindexProgressResponse buildProgressResponse(KnowledgeBase kb, String newEmbeddingModelId) {
        Integer current = kb.getReindexProgressCurrent() != null ? kb.getReindexProgressCurrent() : 0;
        Integer total = kb.getReindexProgressTotal() != null ? kb.getReindexProgressTotal() : 0;
        Integer percentage = total > 0 ? (int) ((current * 100.0) / total) : 0;

        return ReindexProgressResponse.builder()
                .knowledgeBaseId(kb.getId())
                .status(kb.getReindexStatus())
                .current(current)
                .total(total)
                .percentage(percentage)
                .startedAt(kb.getReindexStartedAt() != null ? kb.getReindexStartedAt().format(DATE_FORMATTER) : null)
                .completedAt(kb.getReindexCompletedAt() != null ? kb.getReindexCompletedAt().format(DATE_FORMATTER) : null)
                .errorMessage(kb.getReindexErrorMessage())
                .currentEmbeddingModelId(kb.getDefaultEmbeddingModelId())
                .newEmbeddingModelId(newEmbeddingModelId != null ? newEmbeddingModelId : kb.getDefaultEmbeddingModelId())
                .build();
    }
}