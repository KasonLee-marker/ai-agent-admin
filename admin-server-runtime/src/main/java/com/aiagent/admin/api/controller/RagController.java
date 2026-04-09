package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.ApiResponse;
import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.RagChatResponse;
import com.aiagent.admin.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}