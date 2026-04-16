package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.KnowledgeBaseService;
import com.aiagent.admin.service.ReIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理 REST 控制器
 * <p>
 * 提供知识库管理的 API：
 * <ul>
 *   <li>创建、查询、更新、删除知识库</li>
 *   <li>分页获取知识库列表</li>
 *   <li>获取所有知识库列表（用于下拉选择）</li>
 * </ul>
 * </p>
 *
 * @see KnowledgeBaseService
 */
@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base API", description = "知识库管理接口")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ReIndexService reIndexService;

    /**
     * 创建知识库
     *
     * @param request   知识库请求
     * @param createdBy 创建人（从请求头获取）
     * @return 创建的知识库响应
     */
    @PostMapping
    @Operation(summary = "创建知识库", description = "创建一个新的知识库")
    public ResponseEntity<ApiResponse<KnowledgeBaseResponse>> createKnowledgeBase(
            @Valid @RequestBody KnowledgeBaseRequest request,
            @Parameter(description = "创建人") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String createdBy) {

        KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(request, createdBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取知识库详情
     *
     * @param id 知识库 ID
     * @return 知识库响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情", description = "根据ID获取知识库详情")
    public ResponseEntity<ApiResponse<KnowledgeBaseResponse>> getKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable String id) {

        KnowledgeBaseResponse response = knowledgeBaseService.getKnowledgeBase(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 分页获取知识库列表
     *
     * @param page      页码
     * @param size      每页数量
     * @param createdBy 创建人（从请求头获取）
     * @return 知识库分页列表
     */
    @GetMapping
    @Operation(summary = "获取知识库列表", description = "分页获取知识库列表")
    public ResponseEntity<ApiResponse<Page<KnowledgeBaseResponse>>> listKnowledgeBases(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "创建人") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String createdBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KnowledgeBaseResponse> response = knowledgeBaseService.listKnowledgeBases(createdBy, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取所有知识库列表（不分页）
     * <p>
     * 用于下拉选择等场景。
     * </p>
     *
     * @return 知识库列表
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有知识库", description = "获取所有知识库列表（不分页，用于下拉选择）")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseResponse>>> listAllKnowledgeBases() {

        List<KnowledgeBaseResponse> response = knowledgeBaseService.listAllKnowledgeBases();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新知识库
     *
     * @param id      知识库 ID
     * @param request 知识库请求
     * @return 更新后的知识库响应
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新知识库", description = "更新知识库信息")
    public ResponseEntity<ApiResponse<KnowledgeBaseResponse>> updateKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable String id,
            @Valid @RequestBody KnowledgeBaseRequest request) {

        KnowledgeBaseResponse response = knowledgeBaseService.updateKnowledgeBase(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 删除知识库
     * <p>
     * 如果知识库下有文档，将返回错误。
     * </p>
     *
     * @param id 知识库 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库", description = "删除知识库（如果存在文档则不允许删除）")
    public ResponseEntity<ApiResponse<Void>> deleteKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable String id) {

        knowledgeBaseService.deleteKnowledgeBase(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 启动知识库重索引
     * <p>
     * 切换 Embedding 模型后，批量重新计算所有分块的向量。
     * 异步执行，可通过进度 API 查询进度。
     * </p>
     *
     * @param id      知识库 ID
     * @param request 重索引请求
     * @return 重索引进度响应
     */
    @PostMapping("/{id}/reindex")
    @Operation(summary = "启动知识库重索引", description = "切换 Embedding 模型后批量重新计算向量")
    public ResponseEntity<ApiResponse<ReindexProgressResponse>> startReindex(
            @Parameter(description = "知识库ID") @PathVariable String id,
            @Valid @RequestBody ReindexRequest request) {

        ReindexProgressResponse response = reIndexService.startReindex(id, request.getNewEmbeddingModelId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取重索引进度
     *
     * @param id 知识库 ID
     * @return 重索引进度响应
     */
    @GetMapping("/{id}/reindex/progress")
    @Operation(summary = "获取重索引进度", description = "查询知识库重索引的当前进度")
    public ResponseEntity<ApiResponse<ReindexProgressResponse>> getReindexProgress(
            @Parameter(description = "知识库ID") @PathVariable String id) {

        ReindexProgressResponse response = reIndexService.getReindexProgress(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 取消重索引
     * <p>
     * 仅当重索引正在进行时可以取消。
     * </p>
     *
     * @param id 知识库 ID
     * @return 成功响应
     */
    @PostMapping("/{id}/reindex/cancel")
    @Operation(summary = "取消重索引", description = "取消正在进行的知识库重索引")
    public ResponseEntity<ApiResponse<Void>> cancelReindex(
            @Parameter(description = "知识库ID") @PathVariable String id) {

        reIndexService.cancelReindex(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}