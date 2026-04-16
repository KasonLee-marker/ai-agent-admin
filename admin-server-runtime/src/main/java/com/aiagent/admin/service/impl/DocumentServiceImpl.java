package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.domain.entity.DocumentChunk;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.domain.repository.DocumentRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.*;
import com.aiagent.admin.service.event.DocumentUploadEvent;
import com.aiagent.admin.service.event.EmbeddingStartEvent;
import com.aiagent.admin.service.mapper.DocumentChunkMapper;
import com.aiagent.admin.service.mapper.DocumentMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
 *   <li>创建文档记录（状态：PROCESSING 或 SEMANTIC_PROCESSING）</li>
 *   <li>异步提取文本内容（由 {@link DocumentAsyncService} 处理）</li>
 *   <li>按指定策略分块</li>
 *   <li>保存分块记录（状态：CHUNKED）</li>
 *   <li>用户触发 Embedding（状态：EMBEDDING）</li>
 *   <li>完成（状态：COMPLETED）</li>
 * </ol>
 * </p>
 *
 * @see DocumentService
 * @see DocumentAsyncService
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
    private final BM25SearchService bm25SearchService;
    private final RerankService rerankService;
    private final ModelConfigRepository modelConfigRepository;
    private final ApplicationEventPublisher eventPublisher;

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
     * 对于 SEMANTIC 策略，初始状态为 SEMANTIC_PROCESSING。
     * Embedding 需要用户单独触发（除 SEMANTIC 策略外）。
     * </p>
     *
     * @param file             上传的文件
     * @param name             文档名称（可选，默认使用文件名）
     * @param chunkStrategy    分块策略（FIXED_SIZE/PARAGRAPH/SENTENCE/RECURSIVE/SEMANTIC）
     * @param chunkSize        分块大小（字符数）
     * @param chunkOverlap     分块重叠（字符数）
     * @param embeddingModelId Embedding模型ID（语义分块时必填）
     * @param createdBy        创建者标识
     * @return 文档响应 DTO（状态为 PROCESSING 或 SEMANTIC_PROCESSING）
     * @throws IllegalArgumentException 文件类型不支持时抛出
     */
    @Override
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String name, String knowledgeBaseId,
                                           String chunkStrategy, Integer chunkSize, Integer chunkOverlap,
                                           String embeddingModelId, String createdBy) {
        String contentType = file.getContentType();

        // 根据文件扩展名推断实际类型
        if ("application/octet-stream".equals(contentType) || contentType == null) {
            contentType = inferContentTypeFromExtension(file.getOriginalFilename());
        }

        if (!isSupportedContentTypes(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType +
                    ". Supported types: PDF, Word (.docx), TXT, Markdown, CSV");
        }

        // 语义分块需要 Embedding 模型
        if ("SEMANTIC".equals(chunkStrategy)) {
            if (embeddingModelId == null || embeddingModelId.isEmpty()) {
                // 尝试使用默认 Embedding 模型
                embeddingModelId = modelConfigRepository.findByIsDefaultEmbeddingTrueAndIsActiveTrue()
                        .map(ModelConfig::getId)
                        .orElseThrow(() -> new IllegalArgumentException("Semantic chunking requires an embedding model. No default model configured."));
            }
        }

        // 根据 chunkStrategy 决定初始状态
        Document.DocumentStatus initialStatus = "SEMANTIC".equals(chunkStrategy)
                ? Document.DocumentStatus.SEMANTIC_PROCESSING
                : Document.DocumentStatus.PROCESSING;

        Document document = Document.builder()
                .id(idGenerator.generateId())
                .name(name != null ? name : file.getOriginalFilename())
                .contentType(contentType)
                .fileSize(file.getSize())
                .knowledgeBaseId(knowledgeBaseId)
                .chunkStrategy(chunkStrategy != null ? chunkStrategy : "FIXED_SIZE")
                .chunkSize(chunkSize != null ? chunkSize : 500)
                .chunkOverlap(chunkOverlap != null ? chunkOverlap : 100)
                .chunksCreated(0)
                .chunksEmbedded(0)
                .status(initialStatus)
                .createdBy(createdBy)
                .build();

        document = documentRepository.save(document);

        // 在事务提交前先读取文件内容（因为异步执行时临时文件可能已被 Tomcat 清理）
        byte[] fileContent;
        String originalFilename = file.getOriginalFilename();
        try {
            fileContent = file.getBytes();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        }

        // 发布事件，在事务提交后异步处理文档
        // TransactionalEventListener 确保只有事务成功提交后才执行异步任务
        eventPublisher.publishEvent(new DocumentUploadEvent(
                document.getId(), fileContent, contentType, originalFilename, embeddingModelId));

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

        // 发布事件，在事务提交后异步执行 Embedding
        eventPublisher.publishEvent(new EmbeddingStartEvent(documentId, embeddingConfig));

        return documentMapper.toResponse(document);
    }

    /**
     * 根据模型名称推断向量维度
     *
     * @param modelName 模型名称
     * @return 向量维度
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
     * 根据ID获取文档详情
     *
     * @param documentId 文档唯一标识
     * @return 文档响应 DTO
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
     *
     * @param createdBy 创建者标识
     * @param pageable  分页参数
     * @return 文档响应分页列表
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(String createdBy, Pageable pageable) {
        Page<Document> documents = documentRepository.findByCreatedByOrderByCreatedAtDesc(createdBy, pageable);
        return documents.map(documentMapper::toResponse);
    }

    /**
     * 删除文档及其所有分块
     *
     * @param documentId 文档唯一标识
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
     *
     * @param documentId 文档唯一标识
     * @return 分块响应列表
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
     *
     * @param documentId 文档唯一标识
     * @return 文档响应 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentStatus(String documentId) {
        return getDocument(documentId);
    }

    /**
     * 执行相似度搜索（支持多种策略）
     * <p>
     * 根据请求中的 strategy 参数选择检索方式：
     * <ul>
     *   <li>VECTOR: 向量检索（语义相似度）</li>
     *   <li>BM25: 关键词检索（精确匹配）</li>
     *   <li>HYBRID: 混合检索（RRF 融合向量 + BM25）</li>
     * </ul>
     * </p>
     *
     * @param request 向量搜索请求
     * @return 搜索结果列表
     */
    @Override
    public List<VectorSearchResult> searchSimilar(VectorSearchRequest request) {
        String strategy = request.getStrategy() != null ? request.getStrategy() : "VECTOR";
        int topK = request.getTopK() != null ? request.getTopK() : 5;

        switch (strategy) {
            case "BM25":
                return bm25SearchService.searchBM25(
                        request.getQuery(),
                        request.getKnowledgeBaseId(),
                        request.getDocumentId(),
                        topK);
            case "HYBRID":
                return searchHybrid(request);
            case "VECTOR":
            default:
                return searchVector(request);
        }
    }

    /**
     * 执行向量相似度检索
     * <p>
     * 使用 pgvector 进行向量检索：
     * <ol>
     *   <li>获取 embedding 模型配置（默认或指定）</li>
     *   <li>计算查询文本的向量</li>
     *   <li>在对应的维度表中进行检索</li>
     *   <li>补充分块内容信息</li>
     * </ol>
     * </p>
     *
     * @param request 向量搜索请求
     * @return 搜索结果列表
     */
    private List<VectorSearchResult> searchVector(VectorSearchRequest request) {
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
        String knowledgeBaseId = request.getKnowledgeBaseId();

        // 如果启用 rerank，需要获取更多候选结果
        int searchTopK = topK;
        if (Boolean.TRUE.equals(request.getEnableRerank()) && request.getRerankModelId() != null) {
            searchTopK = topK * 4; // 获取 4 倍候选结果供 rerank 筛选
        }

        List<VectorSearchResult> results = embeddingStorageService.searchSimilar(
                queryEmbedding, embeddingConfig, request.getDocumentId(), knowledgeBaseId, searchTopK, threshold);

        // 补充分块内容信息
        results.forEach(result -> {
            documentChunkRepository.findById(result.getChunkId()).ifPresent(chunk -> {
                result.setChunkIndex(chunk.getChunkIndex());
                result.setContent(chunk.getContent());
                result.setMetadata(chunk.getMetadata());
            });
        });

        // 如果启用 rerank，进行二次排序
        if (Boolean.TRUE.equals(request.getEnableRerank()) && request.getRerankModelId() != null && !results.isEmpty()) {
            ModelConfig rerankConfig = modelConfigRepository.findById(request.getRerankModelId())
                    .orElseThrow(() -> new EntityNotFoundException("Rerank model not found: " + request.getRerankModelId()));

            results = rerankService.rerank(request.getQuery(), results, rerankConfig, topK);
            log.info("Rerank applied: {} candidates -> {} results", searchTopK, results.size());
        }

        return results;
    }

    /**
     * 执行混合检索（HYBRID）
     * <p>
     * 使用 RRF (Reciprocal Rank Fusion) 算法融合向量检索和 BM25 检索结果：
     * <ol>
     *   <li>并行执行向量检索和 BM25 检索</li>
     *   <li>对每个结果计算 RRF 分数：1/(k + rank)</li>
     *   <li>按融合分数排序，返回 topK 结果</li>
     * </ol>
     * </p>
     * <p>
     * RRF 公式：score = Σ 1/(k + rank_i)，其中 k 通常为 60。
     * </p>
     *
     * @param request 向量搜索请求
     * @return 融合后的搜索结果列表
     */
    private List<VectorSearchResult> searchHybrid(VectorSearchRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        int rrfK = 60; // RRF 常数

        // 1. 执行向量检索和 BM25 检索
        List<VectorSearchResult> vectorResults = searchVector(request);
        List<VectorSearchResult> bm25Results = bm25SearchService.searchBM25(
                request.getQuery(),
                request.getKnowledgeBaseId(),
                request.getDocumentId(),
                topK);

        // 2. 使用 RRF 融合排名
        java.util.Map<String, Double> rrfScores = new java.util.HashMap<>();
        java.util.Map<String, VectorSearchResult> resultMap = new java.util.HashMap<>();

        // 向量检索结果计分
        for (int i = 0; i < vectorResults.size(); i++) {
            String chunkId = vectorResults.get(i).getChunkId();
            rrfScores.merge(chunkId, 1.0 / (rrfK + i + 1), Double::sum);
            resultMap.put(chunkId, vectorResults.get(i));
        }

        // BM25 检索结果计分
        for (int i = 0; i < bm25Results.size(); i++) {
            String chunkId = bm25Results.get(i).getChunkId();
            rrfScores.merge(chunkId, 1.0 / (rrfK + i + 1), Double::sum);
            // 如果向量检索中没有该结果，添加到 map
            if (!resultMap.containsKey(chunkId)) {
                resultMap.put(chunkId, bm25Results.get(i));
            }
        }

        // 3. 按融合分数排序，返回 topK
        List<VectorSearchResult> fusedResults = rrfScores.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    VectorSearchResult result = resultMap.get(e.getKey());
                    // 更新分数为融合分数
                    result.setScore(e.getValue());
                    return result;
                })
                .collect(Collectors.toList());

        log.debug("Hybrid search: vector={}, bm25={}, fused={}", vectorResults.size(), bm25Results.size(), fusedResults.size());
        return fusedResults;
    }

    /**
     * 获取系统支持的文件类型列表
     *
     * @return 支持的文件类型列表
     */
    @Override
    public List<String> getSupportedContentTypes() {
        return new ArrayList<>(SUPPORTED_CONTENT_TYPES);
    }

    /**
     * 获取支持的文件类型详细信息（用于前端展示）
     *
     * @return 支持的文件类型详细信息列表
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
     *
     * @param contentType 文件内容类型
     * @return 是否支持
     */
    private boolean isSupportedContentTypes(String contentType) {
        return contentType != null && SUPPORTED_CONTENT_TYPES.contains(contentType);
    }

    /**
     * 获取语义切分进度
     * <p>
     * 返回文档的语义切分处理进度，包含已处理句子数、总句子数和百分比。
     * 仅对 SEMANTIC 策略的文档有意义，其他策略返回空进度。
     * </p>
     *
     * @param documentId 文档唯一标识
     * @return 语义切分进度响应 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public SemanticProgressResponse getSemanticProgress(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));

        Integer current = document.getSemanticProgressCurrent() != null ? document.getSemanticProgressCurrent() : 0;
        Integer total = document.getSemanticProgressTotal() != null ? document.getSemanticProgressTotal() : 0;
        Integer percentage = total > 0 ? (int) ((current * 100.0) / total) : 0;

        return SemanticProgressResponse.builder()
                .status(document.getStatus())
                .current(current)
                .total(total)
                .percentage(percentage)
                .errorMessage(document.getErrorMessage())
                .build();
    }
}