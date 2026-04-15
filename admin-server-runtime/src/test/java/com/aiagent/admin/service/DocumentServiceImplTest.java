package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.domain.entity.DocumentChunk;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.domain.repository.DocumentRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.impl.DocumentServiceImpl;
import com.aiagent.admin.service.mapper.DocumentChunkMapper;
import com.aiagent.admin.service.mapper.DocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private DocumentChunkMapper documentChunkMapper;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private EmbeddingStorageService embeddingStorageService;

    @Mock
    private ModelConfigRepository modelConfigRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private Document testDocument;
    private DocumentChunk testChunk;

    @BeforeEach
    void setUp() {
        testDocument = Document.builder()
                .id("doc-123")
                .name("test.txt")
                .contentType("text/plain")
                .fileSize(100L)
                .status(Document.DocumentStatus.PROCESSING)
                .createdBy("user1")
                .build();

        testChunk = DocumentChunk.builder()
                .id("chunk-123")
                .documentId("doc-123")
                .chunkIndex(0)
                .content("Test content")
                .metadata("{\"documentId\":\"doc-123\"}")
                .build();
    }

    @Test
    void testGetDocument_Success() {
        when(documentRepository.findById("doc-123")).thenReturn(Optional.of(testDocument));

        DocumentResponse mockResponse = DocumentResponse.builder()
                .id("doc-123")
                .name("test.txt")
                .status(Document.DocumentStatus.PROCESSING)
                .build();
        when(documentMapper.toResponse(testDocument)).thenReturn(mockResponse);

        var response = documentService.getDocument("doc-123");

        assertNotNull(response);
        assertEquals("doc-123", response.getId());
        verify(documentRepository).findById("doc-123");
    }

    @Test
    void testGetDocument_NotFound() {
        when(documentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> documentService.getDocument("nonexistent"));
    }

    @Test
    void testListDocuments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> page = new PageImpl<>(List.of(testDocument));
        when(documentRepository.findByCreatedByOrderByCreatedAtDesc("user1", pageable)).thenReturn(page);

        var result = documentService.listDocuments("user1", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testDeleteDocument() {
        when(documentRepository.findById("doc-123")).thenReturn(Optional.of(testDocument));

        documentService.deleteDocument("doc-123");

        verify(documentChunkRepository).deleteByDocumentId("doc-123");
        verify(documentRepository).delete(testDocument);
    }

    @Test
    void testGetDocumentChunks() {
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc("doc-123"))
                .thenReturn(List.of(testChunk));

        var result = documentService.getDocumentChunks("doc-123");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetSupportedContentTypes() {
        var result = documentService.getSupportedContentTypes();

        assertNotNull(result);
        assertTrue(result.contains("application/pdf"));
        assertTrue(result.contains("text/plain"));
    }

    @Test
    void testSearchSimilar() {
        // Given - 设置 embedding 模型配置
        ModelConfig embeddingConfig = ModelConfig.builder()
                .id("embedding-model-1")
                .name("Test Embedding")
                .embeddingDimension(1536)
                .embeddingTableName("document_embeddings_1536")
                .isDefaultEmbedding(true)
                .isActive(true)
                .build();

        when(modelConfigRepository.findByIsDefaultEmbeddingTrueAndIsActiveTrue())
                .thenReturn(Optional.of(embeddingConfig));

        float[] queryEmbedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingService.embedWithModel(anyString(), any(ModelConfig.class)))
                .thenReturn(queryEmbedding);

        // Mock EmbeddingStorageService 返回搜索结果
        List<com.aiagent.admin.api.dto.VectorSearchResult> mockResults = new ArrayList<>();
        mockResults.add(com.aiagent.admin.api.dto.VectorSearchResult.builder()
                .chunkId("chunk-123")
                .documentId("doc-123")
                .score(0.95)
                .build());
        when(embeddingStorageService.searchSimilar(any(float[].class), any(ModelConfig.class), any(), anyInt(), anyDouble()))
                .thenReturn(mockResults);

        // Mock 分块查询
        when(documentChunkRepository.findById("chunk-123")).thenReturn(Optional.of(testChunk));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQuery("Test");
        request.setTopK(5);

        var result = documentService.searchSimilar(request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("chunk-123", result.get(0).getChunkId());
    }

    @Test
    void testDefaultOverlap() {
        // Given - 创建文档时不指定 overlap
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
        when(idGenerator.generateId()).thenReturn("doc-123");

        // Mock MultipartFile with content type
        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("text/plain");
        when(mockFile.getSize()).thenReturn(100L);

        // When - 传入 name 参数，所以 getOriginalFilename 不会被调用
        documentService.uploadDocument(
                mockFile,
                "test.txt",  // 明确指定 name，避免调用 getOriginalFilename
                "FIXED_SIZE",
                500,
                null, // 不指定 overlap，应使用默认值 100
                null, // embeddingModelId
                "user1"
        );

        // Then - 通过 verify 检查保存的文档 overlap 为 100
        verify(documentRepository).save(argThat(doc -> doc.getChunkOverlap() == 100));
    }
}