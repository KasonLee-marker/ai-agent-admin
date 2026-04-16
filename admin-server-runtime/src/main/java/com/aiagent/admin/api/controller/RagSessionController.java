package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.ApiResponse;
import com.aiagent.admin.api.dto.RagMessageDTO;
import com.aiagent.admin.api.dto.RagSessionDTO;
import com.aiagent.admin.service.RagSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 会话管理 REST 控制器
 * <p>
 * 提供 RAG 会话管理的 API：
 * <ul>
 *   <li>创建、查询、删除会话</li>
 *   <li>获取会话消息历史</li>
 * </ul>
 * </p>
 *
 * @see RagSessionService
 */
@RestController
@RequestMapping("/api/v1/rag/sessions")
@RequiredArgsConstructor
@Tag(name = "RAG Session API", description = "RAG会话管理接口")
public class RagSessionController {

    private final RagSessionService ragSessionService;

    /**
     * 创建新的 RAG 会话
     *
     * @param knowledgeBaseId  知识库 ID（可选）
     * @param modelId          对话模型 ID（可选）
     * @param embeddingModelId Embedding 模型 ID（可选）
     * @param createdBy        创建者（从请求头获取）
     * @return 创建的会话响应
     */
    @PostMapping
    @Operation(summary = "创建RAG会话", description = "创建一个新的RAG对话会话")
    public ResponseEntity<ApiResponse<RagSessionDTO>> createSession(
            @Parameter(description = "知识库ID") @RequestParam(required = false) String knowledgeBaseId,
            @Parameter(description = "对话模型ID") @RequestParam(required = false) String modelId,
            @Parameter(description = "Embedding模型ID") @RequestParam(required = false) String embeddingModelId,
            @Parameter(description = "创建者") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String createdBy) {

        RagSessionDTO response = ragSessionService.createSession(knowledgeBaseId, modelId, embeddingModelId, createdBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取会话详情
     *
     * @param id 会话 ID
     * @return 会话响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取会话详情", description = "根据ID获取RAG会话详情")
    public ResponseEntity<ApiResponse<RagSessionDTO>> getSession(
            @Parameter(description = "会话ID") @PathVariable String id) {

        RagSessionDTO response = ragSessionService.getSession(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取会话列表
     *
     * @param page      页码
     * @param size      每页数量
     * @param createdBy 创建者（从请求头获取）
     * @return 会话列表
     */
    @GetMapping
    @Operation(summary = "获取会话列表", description = "分页获取用户的RAG会话列表")
    public ResponseEntity<ApiResponse<List<RagSessionDTO>>> listSessions(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "创建者") @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String createdBy) {

        Pageable pageable = PageRequest.of(page, size);
        List<RagSessionDTO> response = ragSessionService.listSessions(createdBy, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 删除会话
     *
     * @param id 会话 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话", description = "删除RAG会话及其消息")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @Parameter(description = "会话ID") @PathVariable String id) {

        ragSessionService.deleteSession(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取会话消息历史
     *
     * @param id    会话 ID
     * @param limit 最大消息数量（可选）
     * @return 消息列表
     */
    @GetMapping("/{id}/messages")
    @Operation(summary = "获取会话消息", description = "获取RAG会话的消息历史")
    public ResponseEntity<ApiResponse<List<RagMessageDTO>>> getSessionMessages(
            @Parameter(description = "会话ID") @PathVariable String id,
            @Parameter(description = "最大数量") @RequestParam(required = false) Integer limit) {

        List<RagMessageDTO> response = ragSessionService.getHistory(id, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}