package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.PromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
@Tag(name = "Prompt Management", description = "APIs for managing prompt templates")
public class PromptController {

    private final PromptService promptService;

    @PostMapping
    @Operation(summary = "Create a new prompt template")
    public ApiResponse<PromptTemplateResponse> createPrompt(
            @Valid @RequestBody PromptTemplateCreateRequest request) {
        return ApiResponse.success(promptService.createPrompt(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prompt template by ID")
    public ApiResponse<PromptTemplateResponse> getPrompt(
            @Parameter(description = "Prompt ID") @PathVariable String id) {
        return ApiResponse.success(promptService.getPrompt(id));
    }

    @GetMapping
    @Operation(summary = "List prompt templates with pagination and filters")
    public ApiResponse<PageResponse<PromptTemplateResponse>> listPrompts(
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Filter by tag") @RequestParam(required = false) String tag,
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ApiResponse.success(promptService.listPrompts(category, tag, keyword, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a prompt template (auto-creates new version)")
    public ApiResponse<PromptTemplateResponse> updatePrompt(
            @Parameter(description = "Prompt ID") @PathVariable String id,
            @Valid @RequestBody PromptTemplateUpdateRequest request) {
        return ApiResponse.success(promptService.updatePrompt(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prompt template")
    public ApiResponse<Void> deletePrompt(
            @Parameter(description = "Prompt ID") @PathVariable String id) {
        promptService.deletePrompt(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get version history of a prompt template")
    public ApiResponse<List<PromptVersionResponse>> getPromptVersions(
            @Parameter(description = "Prompt ID") @PathVariable String id) {
        return ApiResponse.success(promptService.getPromptVersions(id));
    }

    @PostMapping("/{id}/rollback")
    @Operation(summary = "Rollback a prompt template to a specific version")
    public ApiResponse<PromptTemplateResponse> rollbackPrompt(
            @Parameter(description = "Prompt ID") @PathVariable String id,
            @Valid @RequestBody RollbackRequest request) {
        return ApiResponse.success(promptService.rollbackPrompt(id, request));
    }
}
