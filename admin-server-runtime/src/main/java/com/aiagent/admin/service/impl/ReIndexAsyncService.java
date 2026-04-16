package com.aiagent.admin.service.impl;

import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.domain.entity.DocumentChunk;
import com.aiagent.admin.domain.entity.KnowledgeBase;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.domain.repository.DocumentRepository;
import com.aiagent.admin.domain.repository.KnowledgeBaseRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.EmbeddingService;
import com.aiagent.admin.service.EmbeddingStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库重索引异步服务
 * <p>
 * 在独立线程中执行批量重计算任务，避免阻塞主线程。
 * 参考 {@link DocumentAsyncService} 的异步处理模式。
 * </p>
 *
 * @see ReIndexServiceImpl
 * @see DocumentAsyncService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReIndexAsyncService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final EmbeddingService embeddingService;
    private final EmbeddingStorageService embeddingStorageService;

    /**
     * 异步执行知识库重索引
     * <p>
     * 执行流程：
     * <ol>
     *   <li>获取知识库和新模型配置</li>
     *   <li>获取所有文档和分块</li>
     *   <li>清除旧向量数据</li>
     *   <li>分批计算新 Embedding</li>
     *   <li>存储到新向量表</li>
     *   <li>更新文档和知识库状态</li>
     * </ol>
     * </p>
     *
     * @param knowledgeBaseId     知识库 ID
     * @param newEmbeddingModelId 新 Embedding 模型 ID
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reindexKnowledgeBaseAsync(String knowledgeBaseId, String newEmbeddingModelId) {
        try {
            // 1. 获取知识库和新模型配置
            KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                    .orElseThrow(() -> new IllegalStateException("Knowledge base not found: " + knowledgeBaseId));

            ModelConfig newModel = modelConfigRepository.findById(newEmbeddingModelId)
                    .orElseThrow(() -> new IllegalStateException("Embedding model not found: " + newEmbeddingModelId));

            // 2. 获取所有文档
            List<Document> docs = documentRepository.findByKnowledgeBaseId(knowledgeBaseId);

            if (docs.isEmpty()) {
                log.info("No documents in knowledge base {}, reindex skipped", knowledgeBaseId);
                kb.setReindexStatus(KnowledgeBase.ReindexStatus.COMPLETED);
                kb.setReindexCompletedAt(java.time.LocalDateTime.now());
                knowledgeBaseRepository.save(kb);
                return;
            }

            // 3. 获取所有分块
            List<String> docIds = docs.stream().map(Document::getId).collect(Collectors.toList());
            List<DocumentChunk> allChunks = documentChunkRepository.findByDocumentIdInOrderByDocumentIdAscChunkIndexAsc(docIds);

            if (allChunks.isEmpty()) {
                log.info("No chunks in knowledge base {}, reindex skipped", knowledgeBaseId);
                kb.setReindexStatus(KnowledgeBase.ReindexStatus.COMPLETED);
                kb.setReindexCompletedAt(java.time.LocalDateTime.now());
                knowledgeBaseRepository.save(kb);
                return;
            }

            int totalChunks = allChunks.size();
            log.info("Reindexing {} chunks in knowledge base {} using model {}", totalChunks, knowledgeBaseId, newModel.getName());

            // 4. 更新总数
            kb.setReindexProgressTotal(totalChunks);
            knowledgeBaseRepository.save(kb);

            // 5. 清除旧向量数据（如果旧表名存在）
            for (Document doc : docs) {
                if (doc.getEmbeddingModelId() != null) {
                    ModelConfig oldModel = modelConfigRepository.findById(doc.getEmbeddingModelId()).orElse(null);
                    if (oldModel != null && oldModel.getEmbeddingTableName() != null) {
                        embeddingStorageService.deleteByDocument(doc.getId(), oldModel.getEmbeddingTableName());
                    }
                }
            }

            // 6. 分批计算新 Embedding
            int batchSize = 10;
            int processedCount = 0;

            for (int i = 0; i < allChunks.size(); i += batchSize) {
                // 检查是否被取消
                KnowledgeBase currentKb = knowledgeBaseRepository.findById(knowledgeBaseId).orElse(null);
                if (currentKb == null || currentKb.getReindexStatus() == KnowledgeBase.ReindexStatus.FAILED) {
                    log.info("Reindex cancelled for knowledge base {}", knowledgeBaseId);
                    return;
                }

                int end = Math.min(i + batchSize, allChunks.size());
                List<DocumentChunk> batch = allChunks.subList(i, end);

                // 计算 embedding
                List<String> texts = batch.stream()
                        .map(DocumentChunk::getContent)
                        .collect(Collectors.toList());
                List<float[]> embeddings = embeddingService.embedBatchWithModel(texts, newModel);

                // 存储向量
                List<EmbeddingStorageService.VectorData> vectorDataList = new ArrayList<>();
                for (int j = 0; j < batch.size(); j++) {
                    DocumentChunk chunk = batch.get(j);
                    vectorDataList.add(new EmbeddingStorageService.VectorData(
                            chunk.getId(),
                            chunk.getDocumentId(),
                            embeddings.get(j)
                    ));
                }
                embeddingStorageService.storeVectorsBatch(vectorDataList, newModel.getEmbeddingDimension(), newModel.getEmbeddingTableName());

                processedCount += batch.size();

                // 更新进度
                kb.setReindexProgressCurrent(processedCount);
                knowledgeBaseRepository.save(kb);

                log.info("Reindex progress: {} / {} chunks for knowledge base {}", processedCount, totalChunks, knowledgeBaseId);
            }

            // 7. 更新文档的 Embedding 模型信息
            for (Document doc : docs) {
                doc.setEmbeddingModelId(newEmbeddingModelId);
                doc.setEmbeddingModelName(newModel.getName());
                doc.setEmbeddingDimension(newModel.getEmbeddingDimension());
            }
            documentRepository.saveAll(docs);

            // 8. 更新知识库状态
            kb.setReindexStatus(KnowledgeBase.ReindexStatus.COMPLETED);
            kb.setReindexCompletedAt(java.time.LocalDateTime.now());
            kb.setDefaultEmbeddingModelId(newEmbeddingModelId);
            knowledgeBaseRepository.save(kb);

            log.info("Reindex completed for knowledge base {} with {} chunks, using model {}",
                    knowledgeBaseId, totalChunks, newModel.getName());

        } catch (Exception e) {
            log.error("Reindex failed for knowledge base {}: {}", knowledgeBaseId, e.getMessage(), e);

            // 更新失败状态
            knowledgeBaseRepository.findById(knowledgeBaseId).ifPresent(kb -> {
                kb.setReindexStatus(KnowledgeBase.ReindexStatus.FAILED);
                kb.setReindexErrorMessage(e.getMessage());
                kb.setReindexCompletedAt(java.time.LocalDateTime.now());
                knowledgeBaseRepository.save(kb);
            });
        }
    }
}