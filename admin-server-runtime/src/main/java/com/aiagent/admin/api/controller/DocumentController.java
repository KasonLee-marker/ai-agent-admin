package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理 REST 控制器
 * <p>
 * 提供文档上传和管理的 API：
 * <ul>
 *   <li>文档上传（自动提取文本、分块、向量化）</li>
 *   <li>文档查询、删除</li>
 *   <li>文档分块列表查询</li>
 *   <li>处理状态查询</li>
 *   <li>支持的文件类型查询</li>
 * </ul>
 * </p>
 * <p>
 * 文档上传后自动处理：
 * <ol>
 *   <li>提取文档文本内容</li>
 *   <li>将文本分块（按段落或固定大小）</li>
 *   <li>调用 Embedding 模型生成向量</li>
 *   <li>存储向量到 PostgreSQL pgvector</li>
 * </ol>
 * </p>
 *
 * @see DocumentService
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document API", description = "文档管理接口")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 上传文档
     * <p>
     * 上传后自动处理文档：
     * <ol>
     *   <li>提取文档文本内容</li>
     *   <li>按指定策略分块</li>
     *   <li>异步调用 Embedding 模型生成向量</li>
     * </ol>
     * </p>
     *
     * @param file             上传的文档文件
     * @param name             文档名称（可选，默认使用文件名）
     * @param knowledgeBaseId  知识库ID（可选）
     * @param chunkStrategy    分块策略（FIXED_SIZE/PARAGRAPH/SENTENCE/RECURSIVE/SEMANTIC）
     * @param chunkSize        分块大小（字符数，固定大小分块时使用）
     * @param chunkOverlap     分块重叠（字符数）
     * @param embeddingModelId Embedding模型ID（语义分块时必填）
     * @param createdBy        创建人
     * @return 上传成功的文档信息
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传文档并自动处理（提取文本、分块）")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Parameter(description = "文档文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "文档名称") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "知识库ID") @RequestParam(value = "knowledgeBaseId", required = false) String knowledgeBaseId,
            @Parameter(description = "分块策略（FIXED_SIZE/PARAGRAPH/SENTENCE/RECURSIVE/SEMANTIC）") @RequestParam(value = "chunkStrategy", defaultValue = "FIXED_SIZE") String chunkStrategy,
            @Parameter(description = "分块大小（字符数）") @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
            @Parameter(description = "分块重叠（字符数）") @RequestParam(value = "chunkOverlap", required = false) Integer chunkOverlap,
            @Parameter(description = "Embedding模型ID（语义分块时必填）") @RequestParam(value = "embeddingModelId", required = false) String embeddingModelId,
            @Parameter(description = "创建人") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String createdBy) {

        DocumentResponse response = documentService.uploadDocument(file, name, knowledgeBaseId, chunkStrategy, chunkSize, chunkOverlap, embeddingModelId, createdBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 根据ID获取文档详情
     *
     * @param id 文档ID
     * @return 文档详情信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "根据ID获取文档详情")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @Parameter(description = "文档ID") @PathVariable String id) {

        DocumentResponse response = documentService.getDocument(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 分页获取文档列表
     * <p>
     * 结果按创建时间倒序排列。
     * </p>
     *
     * @param page      页码（从0开始）
     * @param size      每页数量
     * @param createdBy 创建人（用于筛选用户自己的文档）
     * @return 分页的文档列表
     */
    @GetMapping
    @Operation(summary = "获取文档列表", description = "分页获取文档列表")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> listDocuments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "创建人") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String createdBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DocumentResponse> response = documentService.listDocuments(createdBy, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 删除文档
     * <p>
     * 同时删除文档的所有分块和向量数据。
     * </p>
     *
     * @param id 文档ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "删除文档及其分块")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable String id) {

        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取文档的所有分块
     *
     * @param id 文档ID
     * @return 分块列表
     */
    @GetMapping("/{id}/chunks")
    @Operation(summary = "获取文档分块", description = "获取文档的所有分块")
    public ResponseEntity<ApiResponse<List<DocumentChunkResponse>>> getDocumentChunks(
            @Parameter(description = "文档ID") @PathVariable String id) {

        List<DocumentChunkResponse> response = documentService.getDocumentChunks(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取文档处理状态
     * <p>
     * 返回文档当前的处理状态（上传、分块、向量化等）。
     * </p>
     *
     * @param id 文档ID
     * @return 文档状态信息
     */
    @GetMapping("/{id}/status")
    @Operation(summary = "获取文档状态", description = "获取文档处理状态")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentStatus(
            @Parameter(description = "文档ID") @PathVariable String id) {

        DocumentResponse response = documentService.getDocumentStatus(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取系统支持的文件类型
     * <p>
     * 返回允许上传的文档格式列表及其说明。
     * </p>
     *
     * @return 支持的文件类型列表
     */
    @GetMapping("/supported-types")
    @Operation(summary = "获取支持的文件类型", description = "获取系统支持的文档类型列表")
    public ResponseEntity<ApiResponse<List<SupportedTypeResponse>>> getSupportedContentTypes() {
        List<SupportedTypeResponse> response = documentService.getSupportedTypesInfo();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 开始文档向量化（Embedding）
     * <p>
     * 对已分块但未生成向量的文档，调用 Embedding 模型计算向量。
     * </p>
     *
     * @param id               文档ID
     * @param embeddingModelId Embedding模型ID（可选，默认使用知识库绑定的模型）
     * @return 开始向量化后的文档信息
     */
    @PostMapping("/{id}/embed")
    @Operation(summary = "开始Embedding", description = "对已分块的文档开始计算向量")
    public ResponseEntity<ApiResponse<DocumentResponse>> startEmbedding(
            @Parameter(description = "文档ID") @PathVariable String id,
            @Parameter(description = "Embedding模型ID") @RequestParam(value = "embeddingModelId", required = false) String embeddingModelId) {

        DocumentResponse response = documentService.startEmbedding(id, embeddingModelId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取语义切分处理进度
     * <p>
     * 仅当文档使用 SEMANTIC 分块策略时有效，
     * 返回当前已处理的段落数和总段落数。
     * </p>
     *
     * @param id 文档ID
     * @return 语义切分进度信息
     */
    @GetMapping("/{id}/semantic-progress")
    @Operation(summary = "获取语义切分进度", description = "获取文档的语义切分处理进度（仅SEMANTIC策略）")
    public ResponseEntity<ApiResponse<SemanticProgressResponse>> getSemanticProgress(
            @Parameter(description = "文档ID") @PathVariable String id) {

        SemanticProgressResponse response = documentService.getSemanticProgress(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}