package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.DocumentChunkResponse;
import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.api.dto.SupportedTypeResponse;
import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 文档控制器测试类
 * <p>
 * 使用 Mockito 单元测试，避免 Spring Boot 上下文加载问题。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentController documentController;

    private DocumentResponse testDocumentResponse;

    @BeforeEach
    void setUp() {
        testDocumentResponse = DocumentResponse.builder()
                .id("doc-123")
                .name("test.txt")
                .contentType("text/plain")
                .fileSize(100L)
                .totalChunks(1)
                .status(Document.DocumentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy("user1")
                .build();
    }

    @Test
    void testGetDocument() {
        when(documentService.getDocument("doc-123")).thenReturn(testDocumentResponse);

        var response = documentController.getDocument("doc-123");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("doc-123", response.getBody().getData().getId());
    }

    @Test
    void testListDocuments() {
        Page<DocumentResponse> page = new PageImpl<>(List.of(testDocumentResponse));
        when(documentService.listDocuments(any(), any())).thenReturn(page);

        var response = documentController.listDocuments(0, 10, "user1");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().getTotalElements());
    }

    @Test
    void testGetDocumentChunks() {
        DocumentChunkResponse chunk = DocumentChunkResponse.builder()
                .id("chunk-123")
                .documentId("doc-123")
                .chunkIndex(0)
                .content("Test content")
                .build();

        when(documentService.getDocumentChunks("doc-123")).thenReturn(List.of(chunk));

        var response = documentController.getDocumentChunks("doc-123");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void testGetSupportedContentTypes() {
        SupportedTypeResponse pdfType = SupportedTypeResponse.builder()
                .contentType("application/pdf")
                .extension(".pdf")
                .displayName("PDF Document")
                .build();
        SupportedTypeResponse txtType = SupportedTypeResponse.builder()
                .contentType("text/plain")
                .extension(".txt")
                .displayName("Text File")
                .build();

        when(documentService.getSupportedTypesInfo())
                .thenReturn(List.of(pdfType, txtType));

        var response = documentController.getSupportedContentTypes();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getData().size());
        assertEquals("application/pdf", response.getBody().getData().get(0).getContentType());
    }
}