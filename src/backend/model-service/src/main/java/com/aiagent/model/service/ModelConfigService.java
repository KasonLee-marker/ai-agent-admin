package com.aiagent.model.service;

import com.aiagent.model.domain.entity.ModelConfig;
import com.aiagent.model.domain.entity.ModelProvider;
import com.aiagent.model.domain.repository.ModelConfigRepository;
import com.aiagent.model.service.dto.CreateModelRequest;
import com.aiagent.model.service.dto.ModelResponse;
import com.aiagent.model.service.dto.UpdateModelRequest;
import com.aiagent.model.service.mapper.ModelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigService {

    private final ModelConfigRepository repository;
    private final EncryptionService encryptionService;
    private final IdGenerator idGenerator;
    private final ModelMapper mapper;

    @Transactional(readOnly = true)
    public List<ModelResponse> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ModelResponse> findById(String id) {
        return repository.findById(id)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ModelConfig> findEntityById(String id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ModelResponse> findDefault() {
        return repository.findByIsDefaultTrue()
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ModelConfig> findDefaultEntity() {
        return repository.findByIsDefaultTrue();
    }

    @Transactional(readOnly = true)
    public List<ModelResponse> findByFilters(String provider, Boolean isActive, String keyword) {
        ModelProvider providerEnum = provider != null ? ModelProvider.fromString(provider) : null;
        return repository.findByFilters(providerEnum, isActive, keyword).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ModelResponse create(CreateModelRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Model name already exists: " + request.getName());
        }

        ModelConfig entity = mapper.toEntity(request);
        entity.setId(idGenerator.generateId());

        // Encrypt API key
        if (entity.getApiKey() != null && !entity.getApiKey().isEmpty()) {
            entity.setApiKey(encryptionService.encrypt(entity.getApiKey()));
        }

        // Set default base URL if not provided
        if (entity.getBaseUrl() == null || entity.getBaseUrl().isEmpty()) {
            entity.setBaseUrl(entity.getProvider().getDefaultBaseUrl());
        }

        // If this is the first model or marked as default, set it as default
        if (Boolean.TRUE.equals(entity.getIsDefault()) || repository.count() == 0) {
            repository.clearDefaultModel();
            entity.setIsDefault(true);
        }

        ModelConfig saved = repository.save(entity);
        log.info("Created model config: {}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Transactional
    public ModelResponse update(String id, UpdateModelRequest request) {
        ModelConfig existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));

        // Check name uniqueness if changed
        if (!existing.getName().equals(request.getName()) && repository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Model name already exists: " + request.getName());
        }

        mapper.updateEntityFromRequest(request, existing);

        // Handle API key update
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            if (!encryptionService.isEncrypted(request.getApiKey())) {
                existing.setApiKey(encryptionService.encrypt(request.getApiKey()));
            }
        }

        ModelConfig saved = repository.save(existing);
        log.info("Updated model config: {}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String id) {
        ModelConfig entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));

        if (Boolean.TRUE.equals(entity.getIsDefault())) {
            throw new IllegalArgumentException("Cannot delete default model. Please set another model as default first.");
        }

        repository.deleteById(id);
        log.info("Deleted model config: {}", id);
    }

    @Transactional
    public void setDefault(String id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Model not found: " + id);
        }

        repository.clearDefaultModel();
        repository.setDefaultModel(id);
        log.info("Set model {} as default", id);
    }

    @Transactional
    public void updateHealthStatus(String id, ModelConfig.HealthStatus status) {
        repository.findById(id).ifPresent(entity -> {
            entity.setHealthStatus(status);
            entity.setLastHealthCheck(java.time.LocalDateTime.now());
            repository.save(entity);
        });
    }

    @Transactional(readOnly = true)
    public String getDecryptedApiKey(String id) {
        return repository.findById(id)
                .map(ModelConfig::getApiKey)
                .map(encryptionService::decrypt)
                .orElse(null);
    }
}
