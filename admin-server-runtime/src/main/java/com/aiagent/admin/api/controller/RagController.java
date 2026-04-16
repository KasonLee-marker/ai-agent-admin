package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.ApiResponse;
import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.RagChatResponse;
import com.aiagent.admin.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 检索增强生成（RAG）REST 控制器
 * <p>
 * 提供基于文档检索的对话 API：
 * <ul>
 *   <li>RAG 对话：先检索相关文档片段，再生成回复</li>
 *   <li>RAG 流式对话：SSE 格式的流式响应</li>
 * </ul>
 * </p>
 * <p>
 * RAG 流程：
 * <ol>
 *   <li>将用户问题转换为向量</li>
 *   <li>在向量数据库中检索相似文档片段</li>
 *   <li>将检索结果作为上下文构建提示词</li>
 *   <li>调用 AI 模型生成回复</li>
 * </ol>
 * </p>
 *
 * @see RagService
 */
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG API", description = "检索增强生成接口")
public class RagController {

    private final RagService ragService;

    @PostMapping("/chat")
    @Operation(summary = "RAG对话", description = "基于检索增强生成的对话接口")
    public ResponseEntity<ApiResponse<RagChatResponse>> chat(
            @Valid @RequestBody RagChatRequest request) {

        RagChatResponse response = ragService.chat(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG流式对话", description = "基于检索增强生成的流式对话接口（SSE格式）")
    public Flux<String> chatStream(@Valid @RequestBody RagChatRequest request) {
        return ragService.chatStream(request);
    }
}