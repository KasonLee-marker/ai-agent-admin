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

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document API", description = "文档管理接口")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传文档并自动处理（提取文本、分块、向量化）")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Parameter(description = "文档文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "文档名称") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "创建人") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String createdBy) {

        DocumentResponse response = documentService.uploadDocument(file, name, createdBy);
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
    public ResponseEntity<ApiResponse<List<String>>> getSupportedContentTypes() {
        List<String> response = documentService.getSupportedContentTypes();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}