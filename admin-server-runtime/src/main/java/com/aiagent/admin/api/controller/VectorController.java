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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vector")
@RequiredArgsConstructor
@Tag(name = "Vector API", description = "向量检索接口")
public class VectorController {

    private final DocumentService documentService;

    @PostMapping("/search")
    @Operation(summary = "向量相似度搜索", description = "根据查询文本搜索相似的文档片段")
    public ResponseEntity<ApiResponse<List<VectorSearchResult>>> search(
            @Valid @RequestBody VectorSearchRequest request) {

        List<VectorSearchResult> response = documentService.searchSimilar(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}