package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
     * Embedding 需要单独调用 startEmbedding 方法触发。
     * </p>
     *
     * @param file         上传的文件
     * @param name         文档名称（可选）
     * @param chunkStrategy 分块策略（FIXED_SIZE 或 PARAGRAPH）
     * @param chunkSize    分块大小（字符数，仅 FIXED_SIZE 策略有效）
     * @param chunkOverlap 分块重叠（字符数，仅 FIXED_SIZE 策略有效）
     * @param createdBy    创建者标识
     * @return 文档响应 DTO（状态为 PROCESSING）
     */
    DocumentResponse uploadDocument(MultipartFile file, String name,
                                    String chunkStrategy, Integer chunkSize, Integer chunkOverlap,
                                    String createdBy);

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
     */
    DocumentResponse getDocument(String documentId);

    /**
     * 分页查询文档列表
     */
    Page<DocumentResponse> listDocuments(String createdBy, Pageable pageable);

    /**
     * 删除文档
     */
    void deleteDocument(String documentId);

    /**
     * 获取文档分块列表
     */
    List<DocumentChunkResponse> getDocumentChunks(String documentId);

    /**
     * 获取文档处理状态
     */
    DocumentResponse getDocumentStatus(String documentId);

    /**
     * 向量相似度搜索
     */
    List<VectorSearchResult> searchSimilar(VectorSearchRequest request);

    /**
     * 获取支持的文件类型列表
     */
    List<String> getSupportedContentTypes();

    /**
     * 获取支持的文件类型详细信息（用于前端展示）
     */
    List<SupportedTypeResponse> getSupportedTypesInfo();
}