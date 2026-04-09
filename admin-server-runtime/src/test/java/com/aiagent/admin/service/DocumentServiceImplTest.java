package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.domain.entity.DocumentChunk;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.domain.repository.DocumentRepository;
import com.aiagent.admin.service.impl.DocumentServiceImpl;
import com.aiagent.admin.service.mapper.DocumentChunkMapper;
import com.aiagent.admin.service.mapper.DocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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

        var response = documentService.getDocument("doc-123");

        assertNotNull(response);
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
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc("doc-123")).thenReturn(List.of(testChunk));

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
        when(documentChunkRepository.findAll()).thenReturn(List.of(testChunk));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQuery("Test");
        request.setTopK(5);

        var result = documentService.searchSimilar(request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("chunk-123", result.get(0).getChunkId());
    }
}