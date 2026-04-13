package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.DocumentChunkResponse;
import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.DocumentChunk;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.domain.repository.DocumentRepository;
import com.aiagent.admin.service.DocumentService;
import com.aiagent.admin.service.EmbeddingService;
import com.aiagent.admin.service.IdGenerator;
import com.aiagent.admin.service.mapper.DocumentChunkMapper;
import com.aiagent.admin.service.mapper.DocumentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文档管理服务实现类
 * <p>
 * 提供文档上传、处理和管理功能：
 * <ul>
 *   <li>文档上传（PDF、Word、纯文本、Markdown、CSV）</li>
 *   <li>文本提取和分块</li>
 *   <li>向量相似度检索</li>
 *   <li>文档和分块的查询删除</li>
 * </ul>
 * </p>
 * <p>
 * 文档处理流程：
 * <ol>
 *   <li>验证文件类型</li>
 *   <li>创建文档记录（状态：PROCESSING）</li>
 *   <li>异步提取文本内容</li>
 *   <li>将文本分块（默认 500 字符，重叠 50）</li>
 *   <li>保存分块记录</li>
 *   <li>更新文档状态（COMPLETED）</li>
 * </ol>
 * </p>
 * <p>
 * 注意：当前版本使用简单的文本匹配进行检索，
 * 生产环境应使用 pgvector 进行真正的向量相似度搜索。
 * </p>
 *
 * @see DocumentService
 * @see DocumentChunk
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final IdGenerator idGenerator;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 默认分块大小（字符数）
     */
    private static final int DEFAULT_CHUNK_SIZE = 500;
    /** 默认分块重叠大小（字符数） */
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    /** 支持的文件内容类型集合 */
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown",
            "text/csv"
    );

    /**
     * 上传文档并异步处理
     * <p>
     * 创建文档记录后异步执行文本提取和分块。
     * 文档初始状态为 PROCESSING，处理完成后更新为 COMPLETED。
     * </p>
     *
     * @param file      上传的文件
     * @param name      文档名称（可选，默认使用文件名）
     * @param createdBy 创建者标识
     * @return 文档响应 DTO（状态为 PROCESSING）
     * @throws IllegalArgumentException 文件类型不支持时抛出
     */
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

    /**
     * 异步处理文档（提取文本、分块、存储）
     * <p>
     * 使用 @Async 注解异步执行，避免阻塞上传响应。
     * 处理流程：提取文本 → 分块 → 创建分块实体 → 保存 → 更新状态。
     * 处理失败时更新文档状态为 FAILED 并记录错误信息。
     * </p>
     *
     * @param documentId 文档唯一标识
     * @param file       上传的文件
     */
    @Async
    protected void processDocumentAsync(String documentId, MultipartFile file) {
        try {
            com.aiagent.admin.domain.entity.Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found"));

            // Extract text
            String text = extractText(file.getInputStream(), document.getContentType());

            // Split into chunks
            List<String> chunks = splitText(text);

            // Create chunk entities with embeddings
            List<DocumentChunk> chunkEntities = new ArrayList<>();

            // Batch compute embeddings for efficiency
            List<float[]> embeddings = embeddingService.embedBatch(chunks);

            for (int i = 0; i < chunks.size(); i++) {
                String chunkId = idGenerator.generateId();
                String chunkContent = chunks.get(i);
                float[] embedding = embeddings.get(i);

                // Serialize embedding to JSON string for storage
                String embeddingJson = serializeEmbedding(embedding);

                DocumentChunk chunk = DocumentChunk.builder()
                        .id(chunkId)
                        .documentId(documentId)
                        .chunkIndex(i)
                        .content(chunkContent)
                        .embedding(embeddingJson)  // Store embedding vector
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

            log.info("Document processed successfully: {} with {} chunks (embeddings computed)", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(com.aiagent.admin.domain.entity.Document.DocumentStatus.FAILED);
                doc.setErrorMessage(e.getMessage());
                documentRepository.save(doc);
            });
        }
    }

    /**
     * 将 Embedding 向量序列化为 JSON 字符串
     *
     * @param embedding 向量数组
     * @return JSON 字符串
     */
    private String serializeEmbedding(float[] embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize embedding", e);
            return null;
        }
    }

    /**
     * 从 JSON 字符串解析 Embedding 向量
     *
     * @param embeddingJson JSON 字符串
     * @return 向量数组
     */
    private float[] deserializeEmbedding(String embeddingJson) {
        if (embeddingJson == null || embeddingJson.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(embeddingJson, float[].class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize embedding", e);
            return null;
        }
    }

    /**
     * 根据文件类型提取文本内容
     * <p>
     * 支持的文件类型：
     * <ul>
     *   <li>PDF：使用 Apache PDFBox 提取</li>
     *   <li>Word (.docx)：使用 Apache POI 提取</li>
     *   <li>纯文本、Markdown、CSV：直接读取</li>
     * </ul>
     * </p>
     *
     * @param inputStream  文件输入流
     * @param contentType  文件内容类型
     * @return 提取的文本内容
     * @throws IOException 文件读取失败时抛出
     * @throws IllegalArgumentException 文件类型不支持时抛出
     */
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

    /**
     * 使用 Apache PDFBox 提取 PDF 文件文本
     *
     * @param inputStream PDF 文件输入流
     * @return 提取的文本内容
     * @throws IOException 文件读取失败时抛出
     */
    private String extractPdfText(InputStream inputStream) throws IOException {
        try (PDDocument pdfDocument = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdfDocument);
        }
    }

    /**
     * 使用 Apache POI 提取 Word 文件文本
     *
     * @param inputStream Word 文件输入流
     * @return 提取的文本内容
     * @throws IOException 文件读取失败时抛出
     */
    private String extractWordText(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * 将文本分割成固定大小的块
     * <p>
     * 分块策略：
     * <ul>
     *   <li>默认块大小：500 字符</li>
     *   <li>块重叠：50 字符（避免边界信息丢失）</li>
     *   <li>优先在句号、换行符或空格处分割</li>
     * </ul>
     * </p>
     *
     * @param text 要分割的文本
     * @return 文本块列表
     */
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

    /**
     * 根据ID获取文档详情
     *
     * @param documentId 文档唯一标识
     * @return 文档响应 DTO
     * @throws EntityNotFoundException 文档不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(String documentId) {
        com.aiagent.admin.domain.entity.Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
        return documentMapper.toResponse(document);
    }

    /**
     * 分页查询用户的文档列表
     * <p>
     * 按创建时间倒序排列。
     * </p>
     *
     * @param createdBy 创建者标识
     * @param pageable  分页参数
     * @return 分页的文档响应 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(String createdBy, Pageable pageable) {
        Page<com.aiagent.admin.domain.entity.Document> documents = documentRepository.findByCreatedByOrderByCreatedAtDesc(createdBy, pageable);
        return documents.map(documentMapper::toResponse);
    }

    /**
     * 删除文档及其所有分块
     * <p>
     * 先删除文档分块记录，再删除文档记录。
     * </p>
     *
     * @param documentId 文档唯一标识
     * @throws EntityNotFoundException 文档不存在时抛出
     */
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

    /**
     * 获取文档的所有分块列表
     * <p>
     * 按分块索引升序排列。
     * </p>
     *
     * @param documentId 文档唯一标识
     * @return 分块响应 DTO 列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> getDocumentChunks(String documentId) {
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        return chunks.stream()
                .map(documentChunkMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取文档处理状态
     * <p>
     * 用于轮询检查文档处理是否完成。
     * </p>
     *
     * @param documentId 文档唯一标识
     * @return 文档响应 DTO（包含状态信息）
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentStatus(String documentId) {
        return getDocument(documentId);
    }

    /**
     * 执行向量相似度搜索
     * <p>
     * 执行流程：
     * <ol>
     *   <li>计算查询文本的 Embedding 向量</li>
     *   <li>获取所有文档分块（或按文档ID过滤）</li>
     *   <li>计算每个分块与查询的余弦相似度</li>
     *   <li>按相似度排序，返回 TopK 结果</li>
     * </ol>
     * </p>
     * <p>
     * 注意：当前使用内存计算，适合小规模场景（<10000分块）。
     * 大规模场景应使用 pgvector 进行数据库层面的向量检索。
     * </p>
     *
     * @param request 搜索请求，包含查询文本、topK、可选文档ID过滤
     * @return 相似文档片段列表（包含内容和元数据）
     */
    @Override
    public List<VectorSearchResult> searchSimilar(VectorSearchRequest request) {
        // 计算查询文本的 Embedding
        float[] queryEmbedding = embeddingService.embed(request.getQuery());

        // 获取文档分块（按文档ID过滤或获取全部）
        List<DocumentChunk> chunks;
        if (request.getDocumentId() != null && !request.getDocumentId().isEmpty()) {
            chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(request.getDocumentId());
        } else {
            chunks = documentChunkRepository.findAll();
        }

        // 计算相似度并排序
        int topK = request.getTopK() != null ? request.getTopK() : 5;

        return chunks.stream()
                .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty())
                .map(chunk -> {
                    float[] chunkEmbedding = deserializeEmbedding(chunk.getEmbedding());
                    if (chunkEmbedding == null) {
                        return null;
                    }
                    float similarity = embeddingService.cosineSimilarity(queryEmbedding, chunkEmbedding);
                    return VectorSearchResult.builder()
                            .chunkId(chunk.getId())
                            .documentId(chunk.getDocumentId())
                            .chunkIndex(chunk.getChunkIndex())
                            .content(chunk.getContent())
                            .score((double) similarity)
                            .metadata(chunk.getMetadata())
                            .build();
                })
                .filter(result -> result != null && result.getScore() > 0.1)  // 过滤低相似度结果
                .sorted(Comparator.comparingDouble(VectorSearchResult::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 获取系统支持的文件类型列表
     *
     * @return 支持的内容类型 MIME 列表
     */
    @Override
    public List<String> getSupportedContentTypes() {
        return new ArrayList<>(SUPPORTED_CONTENT_TYPES);
    }

    /**
     * 检查文件类型是否支持
     *
     * @param contentType 文件内容类型
     * @return 是否支持该类型
     */
    private boolean isSupportedContentTypes(String contentType) {
        return contentType != null && SUPPORTED_CONTENT_TYPES.contains(contentType);
    }
}