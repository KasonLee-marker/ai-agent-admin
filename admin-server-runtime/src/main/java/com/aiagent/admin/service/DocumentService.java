package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.DocumentChunkResponse;
import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.api.dto.VectorSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    /**
     * 上传文档并处理（提取文本、分块、向量化）
     */
    DocumentResponse uploadDocument(MultipartFile file, String name, String createdBy);

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
     * 获取支持的文件类型
     */
    List<String> getSupportedContentTypes();
}