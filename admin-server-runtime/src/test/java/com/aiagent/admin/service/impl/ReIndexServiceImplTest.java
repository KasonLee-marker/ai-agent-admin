package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.ReindexProgressResponse;
import com.aiagent.admin.domain.entity.KnowledgeBase;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.KnowledgeBaseRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ReIndexServiceImpl 单元测试
 * <p>
 * 测试重索引服务的核心功能：
 * <ul>
 *   <li>启动重索引</li>
 *   <li>获取进度</li>
 *   <li>取消重索引</li>
 *   <li>异常情况处理</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ReIndexServiceImplTest {

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private ModelConfigRepository modelConfigRepository;

    @Mock
    private ReIndexAsyncService reIndexAsyncService;

    @InjectMocks
    private ReIndexServiceImpl reIndexService;

    private KnowledgeBase testKnowledgeBase;
    private ModelConfig testEmbeddingModel;

    @BeforeEach
    void setUp() {
        testKnowledgeBase = KnowledgeBase.builder()
                .id("kb-123")
                .name("Test Knowledge Base")
                .defaultEmbeddingModelId("emb-old")
                .documentCount(5)
                .chunkCount(100)
                .reindexStatus(KnowledgeBase.ReindexStatus.NONE)
                .build();

        testEmbeddingModel = ModelConfig.builder()
                .id("emb-new")
                .name("New Embedding Model")
                .provider(ModelProvider.OPENAI_EMBEDDING)
                .modelName("text-embedding-3-small")
                .embeddingDimension(1536)
                .embeddingTableName("document_embeddings_1536")
                .build();
    }

    @Test
    void startReindex_shouldThrowException_whenKnowledgeBaseNotFound() {
        when(knowledgeBaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> reIndexService.startReindex("nonexistent", "emb-new"));
    }

    @Test
    void startReindex_shouldThrowException_whenReindexInProgress() {
        testKnowledgeBase.setReindexStatus(KnowledgeBase.ReindexStatus.IN_PROGRESS);
        when(knowledgeBaseRepository.findById("kb-123")).thenReturn(Optional.of(testKnowledgeBase));

        assertThrows(IllegalStateException.class,
                () -> reIndexService.startReindex("kb-123", "emb-new"));
    }

    @Test
    void startReindex_shouldThrowException_whenModelNotFound() {
        when(knowledgeBaseRepository.findById("kb-123")).thenReturn(Optional.of(testKnowledgeBase));
        when(modelConfigRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> reIndexService.startReindex("kb-123", "nonexistent"));
    }

    @Test
    void startReindex_shouldThrowException_whenModelNotHealthChecked() {
        testEmbeddingModel.setEmbeddingDimension(null);
        testEmbeddingModel.setEmbeddingTableName(null);
        when(knowledgeBaseRepository.findById("kb-123")).thenReturn(Optional.of(testKnowledgeBase));
        when(modelConfigRepository.findById("emb-new")).thenReturn(Optional.of(testEmbeddingModel));

        assertThrows(IllegalStateException.class,
                () -> reIndexService.startReindex("kb-123", "emb-new"));
    }

    @Test
    void startReindex_shouldReturnProgress_whenValid() {
        when(knowledgeBaseRepository.findById("kb-123")).thenReturn(Optional.of(testKnowledgeBase));
        when(modelConfigRepository.findById("emb-new")).thenReturn(Optional.of(testEmbeddingModel));

        ReindexProgressResponse response = reIndexService.startReindex("kb-123", "emb-new");

        assertNotNull(response);
        assertEquals("kb-123", response.getKnowledgeBaseId());
        assertEquals(KnowledgeBase.ReindexStatus.IN_PROGRESS, response.getStatus());
        assertEquals("emb-new", response.getNewEmbeddingModelId());

        verify(knowledgeBaseRepository, times(1)).save(any(KnowledgeBase.class));
        verify(reIndexAsyncService, times(1)).reindexKnowledgeBaseAsync("kb-123", "emb-new");
    }

    @Test
    void getReindexProgress_shouldThrowException_whenKnowledgeBaseNotFound() {
        when(knowledgeBaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> reIndexService.getReindexProgress("nonexistent"));
    }

    @Test
    void getReindexProgress_shouldReturnProgress() {
        testKnowledgeBase.setReindexStatus(KnowledgeBase.ReindexStatus.IN_PROGRESS);
        testKnowledgeBase.setReindexProgressCurrent(50);
        testKnowledgeBase.setReindexProgressTotal(100);
        when(knowledgeBaseRepository.findById("kb-123")).thenReturn(Optional.of(testKnowledgeBase));

        ReindexProgressResponse response = reIndexService.getReindexProgress("kb-123");

        assertNotNull(response);
        assertEquals(KnowledgeBase.ReindexStatus.IN_PROGRESS, response.getStatus());
        assertEquals(50, response.getCurrent());
        assertEquals(100, response.getTotal());
        assertEquals(50, response.getPercentage());
    }

    @Test
    void cancelReindex_shouldThrowException_whenKnowledgeBaseNotFound() {
        when(knowledgeBaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> reIndexService.cancelReindex("nonexistent"));
    }

    @Test
    void cancelReindex_shouldThrowException_whenNotInProgress() {
        testKnowledgeBase.setReindexStatus(KnowledgeBase.ReindexStatus.NONE);
        when(knowledgeBaseRepository.findById("kb-123")).thenReturn(Optional.of(testKnowledgeBase));

        assertThrows(IllegalStateException.class,
                () -> reIndexService.cancelReindex("kb-123"));
    }

    @Test
    void cancelReindex_shouldSetFailedStatus_whenInProgress() {
        testKnowledgeBase.setReindexStatus(KnowledgeBase.ReindexStatus.IN_PROGRESS);
        when(knowledgeBaseRepository.findById("kb-123")).thenReturn(Optional.of(testKnowledgeBase));

        reIndexService.cancelReindex("kb-123");

        verify(knowledgeBaseRepository, times(1)).save(any(KnowledgeBase.class));
    }
}