package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.ApiResponse;
import com.aiagent.admin.api.dto.VectorSearchRequest;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 向量检索 REST 控制器
 * <p>
 * 提供向量相似度搜索 API：
 * <ul>
 *   <li>将查询文本转换为向量并搜索相似文档片段</li>
 * </ul>
 * </p>
 * <p>
 * 使用 PostgreSQL pgvector 进行向量存储和检索。
 * 返回结果包含文档片段内容和相似度分数。
 * </p>
 *
 * @see DocumentService
 */
@RestController
@RequestMapping("/api/v1/vector")
@RequiredArgsConstructor
@Tag(name = "Vector API", description = "向量检索接口")
public class VectorController {

    private final DocumentService documentService;

    /**
     * 向量相似度搜索
     * <p>
     * 执行流程：
     * <ol>
     *   <li>将查询文本转换为向量</li>
     *   <li>使用 PostgreSQL pgvector 进行余弦相似度搜索</li>
     *   <li>返回相似度分数和文档片段内容</li>
     * </ol>
     * </p>
     *
     * @param request 向量搜索请求，包含查询文本、知识库ID、Embedding模型ID、返回数量等
     * @return 搜索结果列表，包含文档片段内容和相似度分数
     */
    @PostMapping("/search")
    @Operation(summary = "向量相似度搜索", description = "根据查询文本搜索相似的文档片段")
    public ResponseEntity<ApiResponse<List<VectorSearchResult>>> search(
            @Valid @RequestBody VectorSearchRequest request) {

        List<VectorSearchResult> response = documentService.searchSimilar(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}