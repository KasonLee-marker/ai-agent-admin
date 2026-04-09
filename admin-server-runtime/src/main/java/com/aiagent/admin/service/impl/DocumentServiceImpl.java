package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.DocumentChunkResponse;
import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.DocumentChunk;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.domain.repository.DocumentRepository;
import com.aiagent.admin.service.DocumentService;
import com.aiagent.admin.service.IdGenerator;
import com.aiagent.admin.service.mapper.DocumentChunkMapper;
import com.aiagent.admin.service.mapper.DocumentMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final IdGenerator idGenerator;

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown",
            "text/csv"
    );

    @Override
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String name, String createdBy) {
        String contentType = file.getContentType();
        if (!isSupportedContentTypes(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }

        com.aiagent.admin.domain.entity.Document document = com.aiagent.admin.domain.entity.Document.builder()
                .id(idGenerator.generateId())
                .name(name != null ? name : file.getOriginalFilename())
                .contentType(contentType)
                .fileSize(file.getSize())
                .status(com.aiagent.admin.domain.entity.Document.DocumentStatus.PROCESSING)
                .createdBy(createdBy)
                .build();

        document = documentRepository.save(document);

        // Process document asynchronously
        processDocumentAsync(document.getId(), file);

        return documentMapper.toResponse(document);
    }

    @Async
    protected void processDocumentAsync(String documentId, MultipartFile file) {
        try {
            com.aiagent.admin.domain.entity.Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found"));

            // Extract text
            String text = extractText(file.getInputStream(), document.getContentType());

            // Split into chunks
            List<String> chunks = splitText(text);

            // Create chunk entities
            List<DocumentChunk> chunkEntities = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                String chunkId = idGenerator.generateId();
                String chunkContent = chunks.get(i);

                DocumentChunk chunk = DocumentChunk.builder()
                        .id(chunkId)
                        .documentId(documentId)
                        .chunkIndex(i)
                        .content(chunkContent)
                        .metadata(String.format("{\"documentId\":\"%s\",\"documentName\":\"%s\",\"chunkIndex\":%d}",
                                documentId, document.getName().replace("\"", "\\\""), i))
                        .build();
                chunkEntities.add(chunk);
            }

            // Save chunks
            documentChunkRepository.saveAll(chunkEntities);

            // Update document status
            document.setStatus(com.aiagent.admin.domain.entity.Document.DocumentStatus.COMPLETED);
            document.setTotalChunks(chunks.size());
            documentRepository.save(document);

            log.info("Document processed successfully: {} with {} chunks", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(com.aiagent.admin.domain.entity.Document.DocumentStatus.FAILED);
                doc.setErrorMessage(e.getMessage());
                documentRepository.save(doc);
            });
        }
    }

    private String extractText(InputStream inputStream, String contentType) throws IOException {
        return switch (contentType) {
            case "application/pdf" -> extractPdfText(inputStream);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    extractWordText(inputStream);
            case "text/plain", "text/markdown", "text/csv" ->
                    new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    private String extractPdfText(InputStream inputStream) throws IOException {
        try (PDDocument pdfDocument = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdfDocument);
        }
    }

    private String extractWordText(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        // Clean text
        text = text.replaceAll("\\s+", " ").trim();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + DEFAULT_CHUNK_SIZE, text.length());

            // Try to find a good break point
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('。', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int lastSpace = text.lastIndexOf(' ', end);

                int breakPoint = Math.max(Math.max(lastPeriod, lastNewline), lastSpace);
                if (breakPoint > start + DEFAULT_CHUNK_SIZE / 2) {
                    end = breakPoint + 1;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end - DEFAULT_CHUNK_OVERLAP;
            if (start < 0) start = 0;
            if (start >= text.length()) break;
        }

        return chunks;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(String documentId) {
        com.aiagent.admin.domain.entity.Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
        return documentMapper.toResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(String createdBy, Pageable pageable) {
        Page<com.aiagent.admin.domain.entity.Document> documents = documentRepository.findByCreatedByOrderByCreatedAtDesc(createdBy, pageable);
        return documents.map(documentMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteDocument(String documentId) {
        com.aiagent.admin.domain.entity.Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));

        // Delete chunks
        documentChunkRepository.deleteByDocumentId(documentId);

        // Delete document
        documentRepository.delete(document);

        log.info("Document deleted: {}", documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> getDocumentChunks(String documentId) {
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        return chunks.stream()
                .map(documentChunkMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentStatus(String documentId) {
        return getDocument(documentId);
    }

    @Override
    public List<VectorSearchResult> searchSimilar(VectorSearchRequest request) {
        // Simple text-based search fallback
        // In production, this would use vector similarity search with pgvector
        List<DocumentChunk> chunks = documentChunkRepository.findAll();

        String queryLower = request.getQuery().toLowerCase();

        return chunks.stream()
                .filter(chunk -> chunk.getContent().toLowerCase().contains(queryLower))
                .limit(request.getTopK())
                .map(chunk -> VectorSearchResult.builder()
                        .chunkId(chunk.getId())
                        .documentId(chunk.getDocumentId())
                        .chunkIndex(chunk.getChunkIndex())
                        .content(chunk.getContent())
                        .score(1.0)
                        .metadata(chunk.getMetadata())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getSupportedContentTypes() {
        return new ArrayList<>(SUPPORTED_CONTENT_TYPES);
    }

    private boolean isSupportedContentTypes(String contentType) {
        return contentType != null && SUPPORTED_CONTENT_TYPES.contains(contentType);
    }
}