package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.domain.entity.DocumentChunk;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.domain.repository.DocumentRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.DocumentService;
import com.aiagent.admin.service.EmbeddingService;
import com.aiagent.admin.service.EmbeddingStorageService;
import com.aiagent.admin.service.IdGenerator;
import com.aiagent.admin.service.mapper.DocumentChunkMapper;
import com.aiagent.admin.service.mapper.DocumentMapper;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文档管理服务实现类
 * <p>
 * 提供文档上传、处理和管理功能：
 * <ul>
 *   <li>文档上传（PDF、Word、纯文本、Markdown、CSV）</li>
 *   <li>文本提取和分块（支持固定大小和按段落分块）</li>
 *   <li>异步 Embedding 计算</li>
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
 *   <li>按指定策略分块</li>
 *   <li>保存分块记录（状态：CHUNKED）</li>
 *   <li>用户触发 Embedding（状态：EMBEDDING）</li>
 *   <li>完成（状态：COMPLETED）</li>
 * </ol>
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
    private final EmbeddingStorageService embeddingStorageService;
    private final ModelConfigRepository modelConfigRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 支持的文件内容类型集合 */
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown",
            "text/csv",
            "application/octet-stream"
    );

    /**
     * 文件扩展名到内容类型的映射
     */
    private static final java.util.Map<String, String> EXTENSION_TO_CONTENT_TYPE = java.util.Map.of(
            ".pdf", "application/pdf",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".txt", "text/plain",
            ".md", "text/markdown",
            ".markdown", "text/markdown",
            ".csv", "text/csv"
    );

    /**
     * 上传文档并异步处理（提取文本、分块）
     * <p>
     * 创建文档记录后异步执行文本提取和分块。
     * 文档初始状态为 PROCESSING，分块完成后更新为 CHUNKED。
     * Embedding 需要用户单独触发。
     * </p>
     *
     * @param file         上传的文件
     * @param name         文档名称（可选，默认使用文件名）
     * @param chunkStrategy 分块策略（FIXED_SIZE 或 PARAGRAPH）
     * @param chunkSize    分块大小（字符数）
     * @param chunkOverlap 分块重叠（字符数）
     * @param createdBy    创建者标识
     * @return 文档响应 DTO（状态为 PROCESSING）
     * @throws IllegalArgumentException 文件类型不支持时抛出
     */
    @Override
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String name,
                                           String chunkStrategy, Integer chunkSize, Integer chunkOverlap,
                                           String createdBy) {
        String contentType = file.getContentType();

        // 根据文件扩展名推断实际类型
        if ("application/octet-stream".equals(contentType) || contentType == null) {
            contentType = inferContentTypeFromExtension(file.getOriginalFilename());
        }

        if (!isSupportedContentTypes(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType +
                    ". Supported types: PDF, Word (.docx), TXT, Markdown, CSV");
        }

        Document document = Document.builder()
                .id(idGenerator.generateId())
                .name(name != null ? name : file.getOriginalFilename())
                .contentType(contentType)
                .fileSize(file.getSize())
                .chunkStrategy(chunkStrategy != null ? chunkStrategy : "FIXED_SIZE")
                .chunkSize(chunkSize != null ? chunkSize : 500)
                .chunkOverlap(chunkOverlap != null ? chunkOverlap : 50)
                .chunksCreated(0)
                .chunksEmbedded(0)
                .status(Document.DocumentStatus.PROCESSING)
                .createdBy(createdBy)
                .build();

        document = documentRepository.save(document);

        // 异步处理（提取文本 + 分块）
        processDocumentAsync(document.getId(), file, contentType);

        return documentMapper.toResponse(document);
    }

    /**
     * 开始对已分块的文档进行 Embedding 计算
     * <p>
     * 只有状态为 CHUNKED 的文档才能开始 Embedding。
     * 异步执行，状态变为 EMBEDDING，完成后变为 COMPLETED。
     * </p>
     *
     * @param documentId       文档唯一标识
     * @param embeddingModelId Embedding 模型配置 ID（可选，默认使用系统默认 embedding 模型）
     * @return 文档响应 DTO
     */
    @Override
    @Transactional
    public DocumentResponse startEmbedding(String documentId, String embeddingModelId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));

        if (document.getStatus() != Document.DocumentStatus.CHUNKED) {
            throw new IllegalStateException("Document must be in CHUNKED status to start embedding. Current status: " + document.getStatus());
        }

        // 获取 embedding 模型配置
        ModelConfig embeddingConfig;
        if (embeddingModelId != null && !embeddingModelId.isEmpty()) {
            embeddingConfig = modelConfigRepository.findById(embeddingModelId)
                    .orElseThrow(() -> new EntityNotFoundException("Embedding model not found: " + embeddingModelId));
        } else {
            // 使用默认 embedding 模型
            embeddingConfig = modelConfigRepository.findByIsDefaultEmbeddingTrueAndIsActiveTrue()
                    .orElseThrow(() -> new IllegalStateException("No default embedding model configured"));
        }

        // 记录 embedding 模型信息到文档
        document.setEmbeddingModelId(embeddingConfig.getId());
        document.setEmbeddingModelName(embeddingConfig.getName());
        // 根据模型名称推断向量维度
        document.setEmbeddingDimension(inferEmbeddingDimension(embeddingConfig.getModelName()));

        document.setStatus(Document.DocumentStatus.EMBEDDING);
        documentRepository.save(document);

        // 异步执行 Embedding，传入模型配置
        embedChunksAsync(documentId, embeddingConfig);

        return documentMapper.toResponse(document);
    }

    /**
     * 根据模型名称推断向量维度
     */
    private int inferEmbeddingDimension(String modelName) {
        if (modelName == null) return 1536;
        if (modelName.contains("large") || modelName.contains("3072")) return 3072;
        if (modelName.contains("v1") && !modelName.contains("v2") && !modelName.contains("v3")) return 1024;
        if (modelName.contains("v3")) return 1024;
        return 1536; // 默认维度
    }

    /**
     * 根据文件扩展名推断内容类型
     *
     * @param filename 文件名（包含扩展名）
     * @return 推断的内容类型
     */
    private String inferContentTypeFromExtension(String filename) {
        if (filename == null) {
            return null;
        }

        String lowerName = filename.toLowerCase();
        for (java.util.Map.Entry<String, String> entry : EXTENSION_TO_CONTENT_TYPE.entrySet()) {
            if (lowerName.endsWith(entry.getKey())) {
                log.info("Inferred content type {} from filename {}", entry.getValue(), filename);
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 异步处理文档（提取文本 + 分块，不含 Embedding）
     *
     * @param documentId  文档唯一标识
     * @param file        上传的文件
     * @param contentType 文件内容类型
     */
    @Async
    protected void processDocumentAsync(String documentId, MultipartFile file, String contentType) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found"));

            // 提取文本
            String text = extractText(file.getInputStream(), contentType);

            // 分块
            List<String> chunks = splitText(text, document.getChunkStrategy(),
                    document.getChunkSize(), document.getChunkOverlap());

            // 创建分块实体（不含 embedding）
            List<DocumentChunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = DocumentChunk.builder()
                        .id(idGenerator.generateId())
                        .documentId(documentId)
                        .chunkIndex(i)
                        .content(chunks.get(i))
                        .embedding(null)  // 暂不计算 embedding
                        .metadata(String.format("{\"documentId\":\"%s\",\"documentName\":\"%s\",\"chunkIndex\":%d}",
                                documentId, document.getName().replace("\"", "\\\""), i))
                        .build();
                chunkEntities.add(chunk);
            }

            // 保存分块
            documentChunkRepository.saveAll(chunkEntities);

            // 更新文档状态为 CHUNKED
            document.setStatus(Document.DocumentStatus.CHUNKED);
            document.setChunksCreated(chunks.size());
            documentRepository.save(document);

            log.info("Document chunked successfully: {} with {} chunks", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(Document.DocumentStatus.FAILED);
                doc.setErrorMessage(e.getMessage());
                documentRepository.save(doc);
            });
        }
    }

    /**
     * 异步执行 Embedding 计算
     * <p>
     * 执行流程：
     * <ol>
     *   <li>获取文档分块列表</li>
     *   <li>分批调用 Embedding API</li>
     *   <li>将向量存储到对应的维度表（pgvector）</li>
     *   <li>更新进度</li>
     * </ol>
     * </p>
     *
     * @param documentId      文档唯一标识
     * @param embeddingConfig Embedding 模型配置
     */
    @Async
    protected void embedChunksAsync(String documentId, ModelConfig embeddingConfig) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found"));

            List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);

            log.info("Starting embedding for document {} with {} chunks using model {} (dimension {})",
                    documentId, chunks.size(), embeddingConfig.getName(), embeddingConfig.getEmbeddingDimension());

            // 确保向量表存在
            String tableName = embeddingConfig.getEmbeddingTableName();
            Integer dimension = embeddingConfig.getEmbeddingDimension();

            if (tableName == null || dimension == null) {
                throw new IllegalStateException("Embedding model has no dimension or table configured. Please run health check first.");
            }

            // 分批处理 embedding（每批 10 个，避免 API 限流）
            int batchSize = 10;
            int embeddedCount = 0;

            for (int i = 0; i < chunks.size(); i += batchSize) {
                int end = Math.min(i + batchSize, chunks.size());
                List<DocumentChunk> batch = chunks.subList(i, end);

                // 批量计算 embedding，使用指定的模型配置
                List<String> texts = batch.stream()
                        .map(DocumentChunk::getContent)
                        .collect(Collectors.toList());

                List<float[]> embeddings = embeddingService.embedBatchWithModel(texts, embeddingConfig);

                // 存储向量到 pgvector 表
                List<EmbeddingStorageService.VectorData> vectorDataList = new ArrayList<>();
                for (int j = 0; j < batch.size(); j++) {
                    DocumentChunk chunk = batch.get(j);
                    vectorDataList.add(new EmbeddingStorageService.VectorData(
                            chunk.getId(),
                            documentId,
                            embeddings.get(j)
                    ));
                }
                embeddingStorageService.storeVectorsBatch(vectorDataList, dimension, tableName);

                embeddedCount += batch.size();

                // 更新进度
                document.setChunksEmbedded(embeddedCount);
                documentRepository.save(document);

                log.info("Embedded {} / {} chunks for document {}", embeddedCount, chunks.size(), documentId);
            }

            // 完成
            document.setStatus(Document.DocumentStatus.COMPLETED);
            documentRepository.save(document);

            log.info("Document embedding completed: {} with dimension {}, stored in table {}",
                    documentId, dimension, tableName);

        } catch (Exception e) {
            log.error("Error embedding document: {}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(Document.DocumentStatus.FAILED);
                doc.setErrorMessage("Embedding failed: " + e.getMessage());
                documentRepository.save(doc);
            });
        }
    }

    /**
     * 根据文件类型提取文本内容
     */
    private String extractText(InputStream inputStream, String contentType) throws IOException {
        return switch (contentType) {
            case "application/pdf" -> extractPdfText(inputStream);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    extractWordText(inputStream);
            case "text/plain", "text/markdown", "text/csv" -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    /**
     * 使用 Apache PDFBox 提取 PDF 文件文本
     */
    private String extractPdfText(InputStream inputStream) throws IOException {
        try (PDDocument pdfDocument = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdfDocument);
        }
    }

    /**
     * 使用 Apache POI 提取 Word 文件文本
     */
    private String extractWordText(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * 将文本分割成块
     * <p>
     * 支持两种策略：
     * <ul>
     *   <li>FIXED_SIZE: 固定大小分块，尝试在句号/换行/空格处分割</li>
     *   <li>PARAGRAPH: 按段落分块（双换行分隔）</li>
     * </ul>
     * </p>
     *
     * @param text         要分割的文本
     * @param strategy     分块策略
     * @param chunkSize    分块大小（仅 FIXED_SIZE 有效）
     * @param chunkOverlap 分块重叠（仅 FIXED_SIZE 有效）
     * @return 文本块列表
     */
    private List<String> splitText(String text, String strategy, Integer chunkSize, Integer chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        if ("PARAGRAPH".equals(strategy)) {
            // 按段落分块（双换行分隔）
            // 注意：不能把换行符替换掉，只替换多余空格
            text = text.replaceAll("[ \\t]+", " ").trim();

            String[] paragraphs = text.split("\\n\\s*\\n+");
            for (String para : paragraphs) {
                String trimmed = para.trim();
                if (!trimmed.isEmpty()) {
                    // 如果段落超过 chunkSize，需要进一步分割
                    if (trimmed.length() > chunkSize) {
                        int start = 0;
                        while (start < trimmed.length()) {
                            int end = Math.min(start + chunkSize, trimmed.length());
                            // 尝试在句子边界分割
                            if (end < trimmed.length()) {
                                int lastPeriod = Math.max(
                                        trimmed.lastIndexOf('。', end),
                                        trimmed.lastIndexOf('.', end));
                                if (lastPeriod > start + chunkSize / 2) {
                                    end = lastPeriod + 1;
                                }
                            }
                            String subChunk = trimmed.substring(start, end).trim();
                            if (!subChunk.isEmpty()) {
                                chunks.add(subChunk);
                            }
                            start = end;
                        }
                    } else {
                        chunks.add(trimmed);
                    }
                }
            }
        } else {
            // 固定大小分块 - 先清理空白
            text = text.replaceAll("\\s+", " ").trim();

            int size = chunkSize != null ? chunkSize : 500;
            int overlap = chunkOverlap != null ? chunkOverlap : 50;

            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + size, text.length());

                // 尝试在合适的位置分割
                if (end < text.length()) {
                    int lastPeriod = text.lastIndexOf('。', end);
                    int lastNewline = text.lastIndexOf('\n', end);
                    int lastSpace = text.lastIndexOf(' ', end);

                    int breakPoint = Math.max(Math.max(lastPeriod, lastNewline), lastSpace);
                    if (breakPoint > start + size / 2) {
                        end = breakPoint + 1;
                    }
                }

                String chunk = text.substring(start, end).trim();
                if (!chunk.isEmpty()) {
                    chunks.add(chunk);
                }

                start = end - overlap;
                if (start < 0) start = 0;
                if (start >= text.length()) break;
            }
        }

        return chunks;
    }

    /**
     * 根据ID获取文档详情
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
        return documentMapper.toResponse(document);
    }

    /**
     * 分页查询用户的文档列表
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(String createdBy, Pageable pageable) {
        Page<Document> documents = documentRepository.findByCreatedByOrderByCreatedAtDesc(createdBy, pageable);
        return documents.map(documentMapper::toResponse);
    }

    /**
     * 删除文档及其所有分块
     */
    @Override
    @Transactional
    public void deleteDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));

        documentChunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);

        log.info("Document deleted: {}", documentId);
    }

    /**
     * 获取文档的所有分块列表
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
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentStatus(String documentId) {
        return getDocument(documentId);
    }

    /**
     * 执行向量相似度搜索
     * <p>
     * 使用 pgvector 进行向量检索：
     * <ol>
     *   <li>获取 embedding 模型配置（默认或指定）</li>
     *   <li>计算查询文本的向量</li>
     *   <li>在对应的维度表中进行检索</li>
     *   <li>补充分块内容信息</li>
     * </ol>
     * </p>
     */
    @Override
    public List<VectorSearchResult> searchSimilar(VectorSearchRequest request) {
        // 获取 embedding 模型配置
        ModelConfig embeddingConfig;
        if (request.getEmbeddingModelId() != null && !request.getEmbeddingModelId().isEmpty()) {
            embeddingConfig = modelConfigRepository.findById(request.getEmbeddingModelId())
                    .orElseThrow(() -> new EntityNotFoundException("Embedding model not found: " + request.getEmbeddingModelId()));
        } else {
            embeddingConfig = modelConfigRepository.findByIsDefaultEmbeddingTrueAndIsActiveTrue()
                    .orElseThrow(() -> new IllegalStateException("No default embedding model configured"));
        }

        // 验证模型配置了维度和表名
        if (embeddingConfig.getEmbeddingDimension() == null || embeddingConfig.getEmbeddingTableName() == null) {
            throw new IllegalStateException("Embedding model has not been health checked. Please run health check first.");
        }

        // 计算查询向量
        float[] queryEmbedding = embeddingService.embedWithModel(request.getQuery(), embeddingConfig);

        // 使用 EmbeddingStorageService 进行向量检索
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        double threshold = request.getThreshold() != null ? request.getThreshold() : 0.1;

        List<VectorSearchResult> results = embeddingStorageService.searchSimilar(
                queryEmbedding, embeddingConfig, request.getDocumentId(), topK, threshold);

        // 补充分块内容信息
        results.forEach(result -> {
            documentChunkRepository.findById(result.getChunkId()).ifPresent(chunk -> {
                result.setChunkIndex(chunk.getChunkIndex());
                result.setContent(chunk.getContent());
                result.setMetadata(chunk.getMetadata());
            });
        });

        return results;
    }

    /**
     * 获取系统支持的文件类型列表
     */
    @Override
    public List<String> getSupportedContentTypes() {
        return new ArrayList<>(SUPPORTED_CONTENT_TYPES);
    }

    /**
     * 获取支持的文件类型详细信息（用于前端展示）
     */
    @Override
    public List<SupportedTypeResponse> getSupportedTypesInfo() {
        return List.of(
                SupportedTypeResponse.builder()
                        .contentType("application/pdf")
                        .extension(".pdf")
                        .displayName("PDF")
                        .build(),
                SupportedTypeResponse.builder()
                        .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                        .extension(".docx")
                        .displayName("Word")
                        .build(),
                SupportedTypeResponse.builder()
                        .contentType("text/plain")
                        .extension(".txt")
                        .displayName("TXT")
                        .build(),
                SupportedTypeResponse.builder()
                        .contentType("text/markdown")
                        .extension(".md")
                        .displayName("Markdown")
                        .build(),
                SupportedTypeResponse.builder()
                        .contentType("text/csv")
                        .extension(".csv")
                        .displayName("CSV")
                        .build()
        );
    }

    /**
     * 检查文件类型是否支持
     */
    private boolean isSupportedContentTypes(String contentType) {
        return contentType != null && SUPPORTED_CONTENT_TYPES.contains(contentType);
    }
}