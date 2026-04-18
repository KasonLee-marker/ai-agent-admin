package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口
 * <p>
 * 提供文档管理和处理的核心功能：
 * <ul>
 *   <li>文档上传和文本提取</li>
 *   <li>多种分块策略（固定大小、段落、句子、递归、语义）</li>
 *   <li>Embedding 向量计算和存储</li>
 *   <li>向量相似度检索</li>
 *   <li>BM25 关键词检索</li>
 * </ul>
 * </p>
 *
 * @see DocumentResponse
 * @see DocumentChunkResponse
 * @see VectorSearchResult
 */
public interface DocumentService {

    /**
     * 上传文档并处理（提取文本、分块）
     * <p>
     * 文档处理流程：
     * <ol>
     *   <li>验证文件类型</li>
     *   <li>创建文档记录（状态：PROCESSING）</li>
     *   <li>异步提取文本内容</li>
     *   <li>按指定策略分块</li>
     *   <li>保存分块记录（状态：CHUNKED）</li>
     * </ol>
     * Embedding 需要单独调用 startEmbedding 方法触发（除 SEMANTIC 策略外）。
     * </p>
     *
     * @param file             上传的文件
     * @param name             文档名称（可选）
     * @param knowledgeBaseId  知识库ID（可选）
     * @param chunkStrategy    分块策略（FIXED_SIZE/PARAGRAPH/SENTENCE/RECURSIVE/SEMANTIC）
     * @param chunkSize        分块大小（字符数，仅部分策略需要）
     * @param chunkOverlap     分块重叠（字符数）
     * @param embeddingModelId Embedding模型ID（语义分块时必填）
     * @param createdBy        创建者标识
     * @return 文档响应 DTO（状态为 PROCESSING）
     */
    DocumentResponse uploadDocument(MultipartFile file, String name, String knowledgeBaseId,
                                    String chunkStrategy, Integer chunkSize, Integer chunkOverlap,
                                    String embeddingModelId, String createdBy);

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
    DocumentResponse startEmbedding(String documentId, String embeddingModelId);

    /**
     * 获取文档详情
     *
     * @param documentId 文档唯一标识
     * @return 文档响应 DTO
     */
    DocumentResponse getDocument(String documentId);

    /**
     * 分页查询文档列表
     *
     * @param createdBy 创建者标识
     * @param pageable  分页参数
     * @return 文档分页列表
     */
    Page<DocumentResponse> listDocuments(String createdBy, Pageable pageable);

    /**
     * 删除文档
     * <p>
     * 同时删除文档的所有分块和向量数据。
     * </p>
     *
     * @param documentId 文档唯一标识
     */
    void deleteDocument(String documentId);

    /**
     * 获取文档分块列表
     *
     * @param documentId 文档唯一标识
     * @return 分块响应 DTO 列表
     */
    List<DocumentChunkResponse> getDocumentChunks(String documentId);

    /**
     * 获取文档处理状态
     *
     * @param documentId 文档唯一标识
     * @return 文档响应 DTO（包含状态信息）
     */
    DocumentResponse getDocumentStatus(String documentId);

    /**
     * 向量相似度搜索
     * <p>
     * 计算查询文本的 Embedding，在向量表中检索最相似的文档分块。
     * </p>
     *
     * @param request 向量搜索请求，包含查询文本、知识库ID等参数
     * @return 相似度检索结果列表
     */
    List<VectorSearchResult> searchSimilar(VectorSearchRequest request);

    /**
     * 获取支持的文件类型列表
     *
     * @return 支持的 MIME 类型列表
     */
    List<String> getSupportedContentTypes();

    /**
     * 获取支持的文件类型详细信息（用于前端展示）
     */
    List<SupportedTypeResponse> getSupportedTypesInfo();

    /**
     * 获取语义切分进度
     * <p>
     * 返回文档的语义切分处理进度，包含已处理句子数、总句子数和百分比。
     * 仅对 SEMANTIC 策略的文档有意义。
     * </p>
     *
     * @param documentId 文档唯一标识
     * @return 语义切分进度响应 DTO
     */
    SemanticProgressResponse getSemanticProgress(String documentId);
}