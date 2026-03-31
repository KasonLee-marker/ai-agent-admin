package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.PromptTemplate;
import com.aiagent.admin.domain.entity.PromptVersion;
import com.aiagent.admin.domain.repository.PromptTemplateRepository;
import com.aiagent.admin.domain.repository.PromptVersionRepository;
import com.aiagent.admin.service.PromptService;
import com.aiagent.admin.service.mapper.PromptMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptServiceImpl implements PromptService {

    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptVersionRepository promptVersionRepository;
    private final PromptMapper promptMapper;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    @Override
    @Transactional
    public PromptTemplateResponse createPrompt(PromptTemplateCreateRequest request) {
        if (promptTemplateRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Prompt with name '" + request.getName() + "' already exists");
        }

        PromptTemplate entity = promptMapper.toEntity(request);
        
        List<String> variables = extractVariables(request.getContent());
        entity.setVariables(String.join(",", variables));
        
        PromptTemplate saved = promptTemplateRepository.save(entity);
        
        PromptVersion version = PromptVersion.builder()
                .promptId(saved.getId())
                .version(1)
                .content(saved.getContent())
                .changeLog("Initial version")
                .build();
        promptVersionRepository.save(version);
        
        return promptMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PromptTemplateResponse updatePrompt(String id, PromptTemplateUpdateRequest request) {
        PromptTemplate existing = promptTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found with id: " + id));
        
        if (!existing.getName().equals(request.getName()) && promptTemplateRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Prompt with name '" + request.getName() + "' already exists");
        }

        PromptVersion version = PromptVersion.builder()
                .promptId(id)
                .version(existing.getVersion() + 1)
                .content(request.getContent())
                .changeLog(request.getChangeLog() != null ? request.getChangeLog() : "Updated version")
                .build();
        promptVersionRepository.save(version);

        existing.setName(request.getName());
        existing.setContent(request.getContent());
        existing.setDescription(request.getDescription());
        existing.setCategory(request.getCategory());
        existing.setTags(promptMapper.listToString(request.getTags()));
        existing.setVersion(existing.getVersion() + 1);
        
        List<String> variables = extractVariables(request.getContent());
        existing.setVariables(String.join(",", variables));
        
        PromptTemplate updated = promptTemplateRepository.save(existing);
        return promptMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deletePrompt(String id) {
        if (!promptTemplateRepository.existsById(id)) {
            throw new EntityNotFoundException("Prompt not found with id: " + id);
        }
        promptVersionRepository.deleteAll(promptVersionRepository.findByPromptIdOrderByVersionDesc(id));
        promptTemplateRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PromptTemplateResponse getPrompt(String id) {
        PromptTemplate entity = promptTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found with id: " + id));
        return promptMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromptTemplateResponse> listPrompts(String category, String tag, String keyword, Pageable pageable) {
        Page<PromptTemplate> page = promptTemplateRepository.findByFilters(category, tag, keyword, pageable);
        Page<PromptTemplateResponse> responsePage = page.map(promptMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromptVersionResponse> getPromptVersions(String promptId) {
        if (!promptTemplateRepository.existsById(promptId)) {
            throw new EntityNotFoundException("Prompt not found with id: " + promptId);
        }
        List<PromptVersion> versions = promptVersionRepository.findByPromptIdOrderByVersionDesc(promptId);
        return promptMapper.toVersionResponseList(versions);
    }

    @Override
    @Transactional
    public PromptTemplateResponse rollbackPrompt(String promptId, RollbackRequest request) {
        PromptTemplate existing = promptTemplateRepository.findById(promptId)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found with id: " + promptId));
        
        PromptVersion targetVersion = promptVersionRepository.findByPromptIdAndVersion(promptId, request.getVersion())
                .orElseThrow(() -> new EntityNotFoundException("Version " + request.getVersion() + " not found for prompt: " + promptId));
        
        int newVersion = existing.getVersion() + 1;
        
        PromptVersion rollbackVersion = PromptVersion.builder()
                .promptId(promptId)
                .version(newVersion)
                .content(targetVersion.getContent())
                .changeLog("Rollback to version " + request.getVersion() + ": " + 
                          (request.getChangeLog() != null ? request.getChangeLog() : "Rolled back"))
                .build();
        promptVersionRepository.save(rollbackVersion);

        existing.setContent(targetVersion.getContent());
        existing.setVersion(newVersion);
        
        List<String> variables = extractVariables(targetVersion.getContent());
        existing.setVariables(String.join(",", variables));
        
        PromptTemplate updated = promptTemplateRepository.save(existing);
        return promptMapper.toResponse(updated);
    }

    @Override
    public List<String> extractVariables(String content) {
        List<String> variables = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return variables;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String variable = matcher.group(1).trim();
            if (!variable.isEmpty() && !variables.contains(variable)) {
                variables.add(variable);
            }
        }
        return variables;
    }
}