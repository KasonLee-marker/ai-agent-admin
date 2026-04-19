package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.RagSessionDTO;
import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.impl.RagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceImplTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private ModelConfigRepository modelConfigRepository;

    @Mock
    private RagSessionService ragSessionService;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private RagServiceImpl ragService;

    private ModelConfig testModelConfig;
    private RagSessionDTO testSession;
    private VectorSearchResult testSearchResult;

    @BeforeEach
    void setUp() {
        testModelConfig = ModelConfig.builder()
                .id("model-123")
                .name("Test Model")
                .provider(ModelProvider.DASHSCOPE)
                .modelName("qwen-turbo")
                .apiKey("encrypted-key")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .isDefault(true)
                .isActive(true)
                .build();

        testSession = RagSessionDTO.builder()
                .id("session-123")
                .messageCount(0)
                .build();

        testSearchResult = VectorSearchResult.builder()
                .chunkId("chunk-1")
                .documentId("doc-1")
                .documentName("Test Document")
                .content("This is test content for RAG retrieval.")
                .score(0.85)
                .build();
    }

    @Test
    void testChat_NoDefaultModel() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("What is AI?");
        request.setSessionId("existing-session"); // 使用现有会话，跳过创建

        when(modelConfigRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> ragService.chat(request));
    }

    @Test
    void testChat_ModelNotFound() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("What is AI?");
        request.setModelId("nonexistent");
        request.setSessionId("existing-session"); // 使用现有会话，跳过创建

        when(modelConfigRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> ragService.chat(request));
    }

    // ========== retrieve 方法测试 ==========

    @Test
    void retrieve_Success() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("What is machine learning?");
        request.setKnowledgeBaseId("kb-123");
        request.setTopK(5);
        request.setThreshold(0.5);
        request.setEmbeddingModelId("emb-123");
        request.setStrategy("VECTOR");

        List<VectorSearchResult> expectedResults = List.of(testSearchResult);
        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(expectedResults);

        List<VectorSearchResult> results = ragService.retrieve(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("chunk-1", results.get(0).getChunkId());
        assertEquals("doc-1", results.get(0).getDocumentId());
        assertEquals("Test Document", results.get(0).getDocumentName());
        assertEquals(0.85, results.get(0).getScore());

        verify(documentService).searchSimilar(any(VectorSearchRequest.class));
    }

    @Test
    void retrieve_WithDefaultTopK_UsesValue5() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");
        // 不设置 topK，使用默认值
        request.setTopK(null);

        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(List.of());

        ragService.retrieve(request);

        verify(documentService).searchSimilar(argThat(searchReq ->
                searchReq.getTopK() == 5 // 默认 topK 为 5
        ));
    }

    @Test
    void retrieve_WithDefaultThreshold_UsesValue05() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");
        // 不设置 threshold，使用默认值
        request.setThreshold(null);

        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(List.of());

        ragService.retrieve(request);

        verify(documentService).searchSimilar(argThat(searchReq ->
                searchReq.getThreshold() == 0.5 // 默认 threshold 为 0.5
        ));
    }

    @Test
    void retrieve_WithCustomTopK_UsesCustomValue() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");
        request.setTopK(10);

        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(List.of());

        ragService.retrieve(request);

        verify(documentService).searchSimilar(argThat(searchReq ->
                searchReq.getTopK() == 10
        ));
    }

    @Test
    void retrieve_WithCustomThreshold_UsesCustomValue() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");
        request.setThreshold(0.7);

        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(List.of());

        ragService.retrieve(request);

        verify(documentService).searchSimilar(argThat(searchReq ->
                searchReq.getThreshold() == 0.7
        ));
    }

    @Test
    void retrieve_WithKnowledgeBaseId_PassesToSearch() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");
        request.setKnowledgeBaseId("kb-456");

        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(List.of());

        ragService.retrieve(request);

        verify(documentService).searchSimilar(argThat(searchReq ->
                "kb-456".equals(searchReq.getKnowledgeBaseId())
        ));
    }

    @Test
    void retrieve_WithEmbeddingModelId_PassesToSearch() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");
        request.setEmbeddingModelId("emb-456");

        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(List.of());

        ragService.retrieve(request);

        verify(documentService).searchSimilar(argThat(searchReq ->
                "emb-456".equals(searchReq.getEmbeddingModelId())
        ));
    }

    @Test
    void retrieve_WithStrategy_PassesToSearch() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");
        request.setStrategy("HYBRID");

        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(List.of());

        ragService.retrieve(request);

        verify(documentService).searchSimilar(argThat(searchReq ->
                "HYBRID".equals(searchReq.getStrategy())
        ));
    }

    @Test
    void retrieve_WithDocumentId_PassesToSearch() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");
        request.setDocumentId("doc-789");

        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(List.of());

        ragService.retrieve(request);

        verify(documentService).searchSimilar(argThat(searchReq ->
                "doc-789".equals(searchReq.getDocumentId())
        ));
    }

    @Test
    void retrieve_ReturnsMultipleResults() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");
        request.setTopK(3);

        VectorSearchResult result1 = VectorSearchResult.builder()
                .chunkId("chunk-1")
                .documentId("doc-1")
                .documentName("Doc 1")
                .content("Content 1")
                .score(0.9)
                .build();

        VectorSearchResult result2 = VectorSearchResult.builder()
                .chunkId("chunk-2")
                .documentId("doc-2")
                .documentName("Doc 2")
                .content("Content 2")
                .score(0.8)
                .build();

        VectorSearchResult result3 = VectorSearchResult.builder()
                .chunkId("chunk-3")
                .documentId("doc-3")
                .documentName("Doc 3")
                .content("Content 3")
                .score(0.7)
                .build();

        when(documentService.searchSimilar(any(VectorSearchRequest.class)))
                .thenReturn(List.of(result1, result2, result3));

        List<VectorSearchResult> results = ragService.retrieve(request);

        assertEquals(3, results.size());
        assertEquals(0.9, results.get(0).getScore());
        assertEquals(0.8, results.get(1).getScore());
        assertEquals(0.7, results.get(2).getScore());
    }

    @Test
    void retrieve_NoResults_ReturnsEmptyList() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("test question");

        when(documentService.searchSimilar(any(VectorSearchRequest.class))).thenReturn(List.of());

        List<VectorSearchResult> results = ragService.retrieve(request);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}