package com.aiagent.admin.service.impl;

import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.domain.entity.DocumentChunk;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.repository.DocumentChunkRepository;
import com.aiagent.admin.domain.repository.DocumentRepository;
import com.aiagent.admin.domain.repository.KnowledgeBaseRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.*;
import com.aiagent.admin.service.event.EmbeddingStartEvent;
import com.hankcs.hanlp.utility.SentencesUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档异步处理服务
 * <p>
 * 专门处理文档的异步任务，避免 {@code @Async} 自调用问题。
 * 当 {@link DocumentServiceImpl} 需要异步执行文档处理时，调用此服务的方法。
 * </p>
 *
 * <p><b>为什么需要单独的服务类？</b></p>
 * <p>
 * Spring 的 {@code @Async} 是通过 AOP 代理实现的。
 * 如果在同一个类中调用带有 {@code @Async} 的方法（自调用），代理不会拦截，
 * 方法会在当前线程中同步执行。因此需要将异步方法提取到单独的类中。
 * </p>
 *
 * @see DocumentServiceImpl
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAsyncService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final EmbeddingService embeddingService;
    private final EmbeddingStorageService embeddingStorageService;
    private final VectorTableService vectorTableService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ApplicationEventPublisher eventPublisher;
    private final IdGenerator idGenerator;

    /**
     * 异步处理文档（提取文本 + 分块）
     * <p>
     * 此方法在独立线程中执行，不影响上传接口的响应时间。
     * 对于 SEMANTIC 策略，会调用 Embedding API 进行语义分块，
     * 并实时更新处理进度到数据库，供前端轮询显示。
     * </p>
     *
     * @param documentId       文档唯一标识
     * @param file             上传的文件
     * @param contentType      文件内容类型
     * @param embeddingModelId Embedding模型ID（语义分块时使用）
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDocumentAsync(String documentId, MultipartFile file, String contentType, String embeddingModelId) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found"));

            // 提取文本
            String text = extractText(file.getInputStream(), contentType);

            List<String> chunks;

            // 根据策略分块
            if ("SEMANTIC".equals(document.getChunkStrategy())) {
                // 真正的语义分块 - 调用 Embedding API，带进度更新
                ModelConfig embeddingConfig = modelConfigRepository.findById(embeddingModelId)
                        .orElseThrow(() -> new IllegalStateException("Embedding model not found: " + embeddingModelId));

                chunks = splitBySemanticWithEmbeddingWithProgress(text, embeddingConfig, document.getChunkSize(), documentId);

                // 记录 embedding 模型信息
                document.setEmbeddingModelId(embeddingConfig.getId());
                document.setEmbeddingModelName(embeddingConfig.getName());
                document.setEmbeddingDimension(embeddingConfig.getEmbeddingDimension());
            } else {
                // 其他策略使用启发式分块
                chunks = splitText(text, document.getChunkStrategy(),
                        document.getChunkSize(), document.getChunkOverlap());
            }

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
            // 清除进度信息（已完成）
            document.setSemanticProgressCurrent(null);
            document.setSemanticProgressTotal(null);
            documentRepository.save(document);

            log.info("Document chunked successfully: {} with {} chunks using strategy {}", documentId, chunks.size(), document.getChunkStrategy());

            // 更新知识库统计数据（如果文档属于某个知识库）
            if (document.getKnowledgeBaseId() != null) {
                knowledgeBaseService.updateStatistics(document.getKnowledgeBaseId());
            }

            // 对于非语义分块策略，如果文档属于知识库，自动触发 embedding
            if (!"SEMANTIC".equals(document.getChunkStrategy()) && document.getKnowledgeBaseId() != null) {
                // 获取知识库的默认 embedding 模型
                knowledgeBaseRepository.findById(document.getKnowledgeBaseId()).ifPresent(kb -> {
                    if (kb.getDefaultEmbeddingModelId() != null) {
                        modelConfigRepository.findById(kb.getDefaultEmbeddingModelId()).ifPresent(embeddingConfig -> {
                            log.info("Auto-starting embedding for document {} using knowledge base default model {}", documentId, embeddingConfig.getName());
                            // 设置 embedding 模型信息（dimension和tableName会在embedChunksAsync中动态获取）
                            document.setEmbeddingModelId(embeddingConfig.getId());
                            document.setEmbeddingModelName(embeddingConfig.getName());
                            document.setStatus(Document.DocumentStatus.EMBEDDING);
                            documentRepository.save(document);
                            // 通过事件发布异步执行 embedding（避免 @Async 自调用问题）
                            eventPublisher.publishEvent(new EmbeddingStartEvent(documentId, embeddingConfig));
                        });
                    }
                });
            }

        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(Document.DocumentStatus.FAILED);
                doc.setErrorMessage(e.getMessage());
                // 清除进度信息
                doc.setSemanticProgressCurrent(null);
                doc.setSemanticProgressTotal(null);
                documentRepository.save(doc);
            });
        }
    }

    /**
     * 根据文件类型提取文本内容
     *
     * @param inputStream 文件输入流
     * @param contentType 文件内容类型
     * @return 提取的文本内容
     * @throws IOException 文件读取失败时抛出
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
     * 将文本分割成块（使用 HanLP 进行准确的中文句子分割）
     * <p>
     * 所有策略都使用 HanLP 的 {@link SentencesUtil#toSentenceList(String)} 进行句子分割，
     * 确保 overlap 参数对中文文本有效。
     * </p>
     * <p>
     * 支持四种策略（不含 SEMANTIC，SEMANTIC 单独处理）：
     * <ul>
     *   <li>FIXED_SIZE: 按字符数分块，在句子边界处分割</li>
     *   <li>PARAGRAPH: 以双换行分隔段落，过长段落会进一步分割</li>
     *   <li>SENTENCE: 按句子分块，合并到目标大小</li>
     *   <li>RECURSIVE: 递归分块（段落→句子→字符）</li>
     * </ul>
     * </p>
     *
     * @param text         要分割的文本
     * @param strategy     分块策略
     * @param chunkSize    分块大小（字符数）
     * @param chunkOverlap 分块重叠（字符数）
     * @return 文本块列表
     */
    private List<String> splitText(String text, String strategy, Integer chunkSize, Integer chunkOverlap) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        int size = chunkSize != null ? chunkSize : 500;
        int overlap = chunkOverlap != null ? chunkOverlap : 100;

        // 使用 HanLP 进行句子分割（对中文准确）
        List<String> sentences = SentencesUtil.toSentenceList(text);
        sentences = sentences.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (sentences.isEmpty()) {
            return new ArrayList<>();
        }

        // 根据策略进行分块，都使用 HanLP 句子分割确保 overlap 有效
        return switch (strategy) {
            case "PARAGRAPH" -> splitByParagraphWithHanLP(text, size, overlap);
            case "SENTENCE" -> splitBySentenceWithOverlap(sentences, size, overlap);
            case "RECURSIVE" -> splitRecursiveWithHanLP(text, sentences, size, overlap);
            default -> splitBySentenceWithOverlap(sentences, size, overlap); // FIXED_SIZE 也用句子分割
        };
    }

    /**
     * 按段落分块（使用 HanLP 处理过长段落）
     * <p>
     * 先按双换行分割段落，如果段落超过 chunkSize，则使用 HanLP 按句子进一步分割。
     * </p>
     */
    private List<String> splitByParagraphWithHanLP(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        // 按段落分割
        String[] paragraphs = text.split("\\n\\n+");
        List<String> currentParagraphSentences = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        String overlapContent = "";

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // 如果段落超过 chunkSize，用 HanLP 拆成句子
            if (paragraph.length() > chunkSize) {
                List<String> paragraphSentences = SentencesUtil.toSentenceList(paragraph);
                for (String sentence : paragraphSentences) {
                    sentence = sentence.trim();
                    if (sentence.isEmpty()) continue;

                    if (currentChunk.length() + sentence.length() > chunkSize && !currentChunk.isEmpty()) {
                        // 提交当前分块
                        chunks.add(overlapContent + currentChunk.toString().trim());
                        // 计算 overlap
                        overlapContent = getOverlapFromChunk(overlapContent + currentChunk, overlap);
                        currentChunk = new StringBuilder();
                    }
                    currentChunk.append(sentence).append(" ");
                }
            } else {
                // 段落未超过限制，直接添加
                if (currentChunk.length() + paragraph.length() > chunkSize && !currentChunk.isEmpty()) {
                    chunks.add(overlapContent + currentChunk.toString().trim());
                    overlapContent = getOverlapFromChunk(overlapContent + currentChunk, overlap);
                    currentChunk = new StringBuilder();
                }
                currentChunk.append(paragraph).append(" ");
            }
        }

        // 处理最后一个分块
        if (!currentChunk.isEmpty()) {
            chunks.add(overlapContent + currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 按句子分块，带 overlap（基于 HanLP）
     * <p>
     * 使用 HanLP 分割的句子，按目标大小合并，相邻分块有 overlap。
     * </p>
     */
    private List<String> splitBySentenceWithOverlap(List<String> sentences, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        String overlapContent = "";

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && !currentChunk.isEmpty()) {
                // 提交当前分块
                chunks.add(overlapContent + currentChunk.toString().trim());
                // 计算 overlap（从当前分块末尾取）
                overlapContent = getOverlapFromChunk(overlapContent + currentChunk, overlap);
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }

        // 处理最后一个分块
        if (currentChunk.length() > 0) {
            chunks.add(overlapContent + currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 递归分块（段落→句子→字符），基于 HanLP
     * <p>
     * 先尝试按段落，过长段落按句子，过长句子按字符截取。
     * </p>
     */
    private List<String> splitRecursiveWithHanLP(String text, List<String> sentences, int chunkSize, int overlap) {
        // 递归分块本质上和句子分块相同，因为 HanLP 已经按最小单位分割
        return splitBySentenceWithOverlap(sentences, chunkSize, overlap);
    }

    /**
     * 从分块末尾提取 overlap 内容
     * <p>
     * 尝试从末尾取完整的句子作为 overlap，如果句子过长则按字符截取。
     * </p>
     *
     * @param chunk       当前分块内容
     * @param overlapSize 目标 overlap 大小
     * @return overlap 内容
     */
    private String getOverlapFromChunk(String chunk, int overlapSize) {
        if (chunk == null || chunk.length() <= overlapSize) {
            return chunk != null ? chunk : "";
        }

        // 从末尾截取 overlapSize 字符
        return chunk.substring(chunk.length() - overlapSize);
    }

    /**
     * 基于 Embedding 的语义分块（带进度更新）
     * <p>
     * 流程：
     * <ol>
     *   <li>使用 HanLP 将文本拆成句子，更新总句子数到数据库</li>
     *   <li>分批计算句子的 embedding，每批完成后更新进度</li>
     *   <li>计算相邻句子的余弦相似度</li>
     *   <li>找到相似度断点（低于百分位数阈值）</li>
     *   <li>在断点处分割，合并句子形成语义块</li>
     * </ol>
     * </p>
     * <p>
     * 使用 HanLP 的 {@link SentencesUtil#toSentenceList(String)} 进行准确的中文句子分割，
     * 相比简单的正则分割，能正确处理引号内的句号等复杂情况。
     * </p>
     *
     * @param text            要分割的文本
     * @param embeddingConfig Embedding模型配置
     * @param maxSize         最大分块大小（防止过长）
     * @param documentId      文档ID（用于更新进度）
     * @return 文本块列表
     */
    private List<String> splitBySemanticWithEmbeddingWithProgress(String text, ModelConfig embeddingConfig, Integer maxSize, String documentId) {
        List<String> chunks = new ArrayList<>();

        // 清理文本
        text = text.replaceAll("[ \\t]+", " ").trim();

        // 使用 HanLP 按句子分割（比正则更准确）
        List<String> sentences = SentencesUtil.toSentenceList(text);

        // 过滤空句子
        sentences = sentences.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (sentences.size() <= 1) {
            // 更新进度：快速完成
            updateSemanticProgress(documentId, 1, !sentences.isEmpty() ? sentences.size() : 1);
            chunks.add(text);
            return chunks;
        }

        int totalSentences = sentences.size();
        log.info("Semantic chunking: {} sentences total, computing embeddings using model {}", totalSentences, embeddingConfig.getName());

        // 初始化进度：设置总数
        updateSemanticProgress(documentId, 0, totalSentences);

        // 分批计算 embedding（DashScope 限制每批最多 10 个）
        int batchSize = 10;
        List<float[]> embeddings = new ArrayList<>();

        for (int i = 0; i < sentences.size(); i += batchSize) {
            int end = Math.min(i + batchSize, sentences.size());
            List<String> batch = sentences.subList(i, end);

            try {
                List<float[]> batchEmbeddings = embeddingService.embedBatchWithModel(batch, embeddingConfig);
                embeddings.addAll(batchEmbeddings);

                // 更新进度
                updateSemanticProgress(documentId, end, totalSentences);

                log.debug("Embedded batch {}-{} of {} sentences", i, end, totalSentences);
            } catch (Exception e) {
                log.error("Failed to embed batch {}-{}: {}", i, end, e.getMessage());
                throw new RuntimeException("Semantic chunking embedding failed: " + e.getMessage(), e);
            }
        }

        // 计算相邻句子的余弦相似度
        double[] similarities = new double[embeddings.size() - 1];
        for (int i = 0; i < embeddings.size() - 1; i++) {
            similarities[i] = cosineSimilarity(embeddings.get(i), embeddings.get(i + 1));
        }

        // 计算相似度阈值（使用百分位数，低于此值的视为断点）
        double threshold = calculatePercentile(similarities, 30); // 第30百分位数

        log.info("Semantic chunking: similarity threshold = {}, avg similarity = {}", threshold, calculateAverage(similarities));

        // 根据相似度断点分块
        StringBuilder currentChunk = new StringBuilder();
        int maxChunkSize = maxSize != null ? maxSize : 1000; // 默认最大1000字符

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);

            // 检查是否需要在当前句子前分割
            boolean shouldBreak = i > 0 && similarities[i - 1] < threshold;
            // 相似度低于阈值，语义断点

            // 或者当前块超过最大大小
            if (currentChunk.length() + sentence.length() > maxChunkSize && !currentChunk.isEmpty()) {
                shouldBreak = true;
            }

            if (shouldBreak && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }

            currentChunk.append(sentence).append(" ");
        }

        // 添加最后一个块
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("Semantic chunking completed: {} chunks created", chunks.size());
        return chunks;
    }

    /**
     * 更新语义切分进度到数据库
     * <p>
     * 用于前端轮询显示实时进度。
     * </p>
     *
     * @param documentId 文档ID
     * @param current    已处理句子数
     * @param total      总句子数
     */
    private void updateSemanticProgress(String documentId, int current, int total) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setSemanticProgressCurrent(current);
            doc.setSemanticProgressTotal(total);
            documentRepository.save(doc);
        });
    }

    /**
     * 计算余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度（0-1）
     */
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 计算百分位数
     *
     * @param values     数值数组
     * @param percentile 百分位（如30表示第30百分位）
     * @return 百分位数值
     */
    private double calculatePercentile(double[] values, int percentile) {
        if (values.length == 0) return 0.0;

        // 复制并排序
        double[] sorted = values.clone();
        java.util.Arrays.sort(sorted);

        // 计算百分位位置
        int index = (int) Math.ceil(percentile / 100.0 * sorted.length) - 1;
        index = Math.max(0, Math.min(index, sorted.length - 1));

        return sorted[index];
    }

    /**
     * 计算平均值
     *
     * @param values 数值数组
     * @return 平均值
     */
    private double calculateAverage(double[] values) {
        if (values.length == 0) return 0.0;
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    /**
     * 异步执行 Embedding 计算
     * <p>
     * 执行流程：
     * <ol>
     *   <li>获取文档分块列表</li>
     *   <li>如果缺少dimension和tableName，先动态获取</li>
     *   <li>分批调用 Embedding API</li>
     *   <li>将向量存储到对应的维度表（pgvector）</li>
     *   <li>更新进度</li>
     * </ol>
     * </p>
     *
     * @param documentId      文档唯一标识
     * @param embeddingConfig Embedding 模型配置
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void embedChunksAsync(String documentId, ModelConfig embeddingConfig) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found"));

            List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);

            log.info("Starting embedding for document {} with {} chunks using model {}",
                    documentId, chunks.size(), embeddingConfig.getName());

            // 如果缺少dimension和tableName，动态获取
            Integer dimension = embeddingConfig.getEmbeddingDimension();
            String tableName = embeddingConfig.getEmbeddingTableName();

            if (dimension == null || tableName == null) {
                log.info("Embedding model {} has no dimension/table configured, will dynamically determine", embeddingConfig.getName());
                // 用第一个分块的内容调用一次embedding来获取dimension
                if (!chunks.isEmpty()) {
                    float[] sampleEmbedding = embeddingService.embedWithModel(chunks.get(0).getContent(), embeddingConfig);
                    dimension = sampleEmbedding.length;
                    tableName = vectorTableService.ensureTableExists(dimension);

                    // 更新模型配置（供下次使用）
                    embeddingConfig.setEmbeddingDimension(dimension);
                    embeddingConfig.setEmbeddingTableName(tableName);
                    modelConfigRepository.save(embeddingConfig);

                    log.info("Determined embedding dimension {} for model {}, created table {}", dimension, embeddingConfig.getName(), tableName);
                } else {
                    throw new IllegalStateException("Document has no chunks to determine embedding dimension");
                }
            } else {
                // 确保向量表存在
                vectorTableService.ensureTableExists(dimension);
            }

            // 更新文档的dimension信息
            document.setEmbeddingDimension(dimension);
            documentRepository.save(document);

            // 分批处理 embedding（每批 10 个，避免 API 限流）
            int batchSize = 10;
            int embeddedCount = 0;

            // 如果dimension是从第一个chunk获取的，跳过第一个（已经计算过了）
            int startIndex = embeddingConfig.getEmbeddingDimension() == null ? 1 : 0;

            for (int i = startIndex; i < chunks.size(); i += batchSize) {
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

                // 如果是从第一个chunk获取dimension，需要额外存储第一个chunk的向量
                if (startIndex == 1 && i == startIndex) {
                    // 存储第一个chunk的向量
                    DocumentChunk firstChunk = chunks.get(0);
                    embeddingStorageService.storeVectorsBatch(List.of(new EmbeddingStorageService.VectorData(
                            firstChunk.getId(),
                            documentId,
                            embeddingService.embedWithModel(firstChunk.getContent(), embeddingConfig)
                    )), dimension, tableName);
                    embeddedCount = 1 + batch.size();
                }

                // 更新进度
                document.setChunksEmbedded(embeddedCount);
                documentRepository.save(document);

                log.info("Embedded {} / {} chunks for document {}", embeddedCount, chunks.size(), documentId);
            }

            // 完成
            document.setStatus(Document.DocumentStatus.COMPLETED);
            documentRepository.save(document);

            // 更新知识库统计数据（如果文档属于某个知识库）
            if (document.getKnowledgeBaseId() != null) {
                knowledgeBaseService.updateStatistics(document.getKnowledgeBaseId());
            }

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
}