package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PromptService {

    PromptTemplateResponse createPrompt(PromptTemplateCreateRequest request);

    PromptTemplateResponse updatePrompt(String id, PromptTemplateUpdateRequest request);

    void deletePrompt(String id);

    PromptTemplateResponse getPrompt(String id);

    PageResponse<PromptTemplateResponse> listPrompts(String category, String tag, String keyword, Pageable pageable);

    List<PromptVersionResponse> getPromptVersions(String promptId);

    PromptTemplateResponse rollbackPrompt(String promptId, RollbackRequest request);

    List<String> extractVariables(String content);
}
