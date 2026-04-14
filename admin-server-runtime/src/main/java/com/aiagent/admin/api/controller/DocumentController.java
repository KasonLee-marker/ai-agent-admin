package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.ApiResponse;
import com.aiagent.admin.api.dto.DocumentChunkResponse;
import com.aiagent.admin.api.dto.DocumentResponse;
import com.aiagent.admin.api.dto.SupportedTypeResponse;
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

    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传文档并自动处理（提取文本、分块）")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Parameter(description = "文档文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "文档名称") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "分块策略") @RequestParam(value = "chunkStrategy", defaultValue = "FIXED_SIZE") String chunkStrategy,
            @Parameter(description = "分块大小") @RequestParam(value = "chunkSize", defaultValue = "500") Integer chunkSize,
            @Parameter(description = "分块重叠") @RequestParam(value = "chunkOverlap", defaultValue = "50") Integer chunkOverlap,
            @Parameter(description = "创建人") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String createdBy) {

        DocumentResponse response = documentService.uploadDocument(file, name, chunkStrategy, chunkSize, chunkOverlap, createdBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "根据ID获取文档详情")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @Parameter(description = "文档ID") @PathVariable String id) {

        DocumentResponse response = documentService.getDocument(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

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

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "删除文档及其分块")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable String id) {

        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/chunks")
    @Operation(summary = "获取文档分块", description = "获取文档的所有分块")
    public ResponseEntity<ApiResponse<List<DocumentChunkResponse>>> getDocumentChunks(
            @Parameter(description = "文档ID") @PathVariable String id) {

        List<DocumentChunkResponse> response = documentService.getDocumentChunks(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "获取文档状态", description = "获取文档处理状态")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentStatus(
            @Parameter(description = "文档ID") @PathVariable String id) {

        DocumentResponse response = documentService.getDocumentStatus(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/supported-types")
    @Operation(summary = "获取支持的文件类型", description = "获取系统支持的文档类型列表")
    public ResponseEntity<ApiResponse<List<SupportedTypeResponse>>> getSupportedContentTypes() {
        List<SupportedTypeResponse> response = documentService.getSupportedTypesInfo();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/embed")
    @Operation(summary = "开始Embedding", description = "对已分块的文档开始计算向量")
    public ResponseEntity<ApiResponse<DocumentResponse>> startEmbedding(
            @Parameter(description = "文档ID") @PathVariable String id,
            @Parameter(description = "Embedding模型ID") @RequestParam(value = "embeddingModelId", required = false) String embeddingModelId) {

        DocumentResponse response = documentService.startEmbedding(id, embeddingModelId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}