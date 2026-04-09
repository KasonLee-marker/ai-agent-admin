package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.DocumentChunkResponse;
import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

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
    void testGetDocument() throws Exception {
        when(documentService.getDocument("doc-123")).thenReturn(testDocumentResponse);

        mockMvc.perform(get("/api/v1/documents/doc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("doc-123"))
                .andExpect(jsonPath("$.data.name").value("test.txt"));
    }

    @Test
    void testListDocuments() throws Exception {
        Page<DocumentResponse> page = new PageImpl<>(List.of(testDocumentResponse));
        when(documentService.listDocuments(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/documents")
                        .header("X-User-Id", "user1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("doc-123"));
    }

    @Test
    void testGetDocumentChunks() throws Exception {
        DocumentChunkResponse chunk = DocumentChunkResponse.builder()
                .id("chunk-123")
                .documentId("doc-123")
                .chunkIndex(0)
                .content("Test content")
                .build();

        when(documentService.getDocumentChunks("doc-123")).thenReturn(List.of(chunk));

        mockMvc.perform(get("/api/v1/documents/doc-123/chunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("chunk-123"));
    }

    @Test
    void testGetSupportedContentTypes() throws Exception {
        when(documentService.getSupportedContentTypes())
                .thenReturn(List.of("application/pdf", "text/plain"));

        mockMvc.perform(get("/api/v1/documents/supported-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("application/pdf"));
    }
}