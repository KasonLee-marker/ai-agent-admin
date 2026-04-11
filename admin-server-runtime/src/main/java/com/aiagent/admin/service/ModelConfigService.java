package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.CreateModelRequest;
import com.aiagent.admin.api.dto.ModelResponse;
import com.aiagent.admin.api.dto.UpdateModelRequest;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.mapper.ModelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模型配置服务
 * <p>
 * 提供 AI 模型配置的管理功能：
 * <ul>
 *   <li>模型配置的 CRUD 操作</li>
 *   <li>默认模型管理</li>
 *   <li>模型筛选（按 Provider、状态、关键词）</li>
 *   <li>健康状态更新</li>
 *   <li>API Key 加密存储</li>
 * </ul>
 * </p>
 * <p>
 * 支持多种 AI Provider（OpenAI、DashScope、Azure 等），
 * API Key 使用 EncryptionService 加密后存储。
 * </p>
 *
 * @see ModelConfig
 * @see ModelProvider
 * @see EncryptionService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigService {

    private final ModelConfigRepository repository;
    private final EncryptionService encryptionService;
    private final IdGenerator idGenerator;
    private final ModelMapper mapper;

    /**
     * 查询所有模型配置
     *
     * @return 所有模型配置响应 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<ModelResponse> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID查询模型配置
     *
     * @param id 模型配置唯一标识
     * @return 模型配置响应 DTO（Optional）
     */
    @Transactional(readOnly = true)
    public Optional<ModelResponse> findById(String id) {
        return repository.findById(id)
                .map(mapper::toResponse);
    }

    /**
     * 根据ID查询模型配置实体（用于内部调用）
     *
     * @param id 模型配置唯一标识
     * @return 模型配置实体（Optional）
     */
    @Transactional(readOnly = true)
    public Optional<ModelConfig> findEntityById(String id) {
        return repository.findById(id);
    }

    /**
     * 查询默认模型配置
     *
     * @return 默认模型配置响应 DTO（Optional）
     */
    @Transactional(readOnly = true)
    public Optional<ModelResponse> findDefault() {
        return repository.findByIsDefaultTrue()
                .map(mapper::toResponse);
    }

    /**
     * 查询默认模型配置实体（用于内部调用）
     *
     * @return 默认模型配置实体（Optional）
     */
    @Transactional(readOnly = true)
    public Optional<ModelConfig> findDefaultEntity() {
        return repository.findByIsDefaultTrue();
    }

    /**
     * 根据筛选条件查询模型配置列表
     * <p>
     * 支持按 Provider、活跃状态、关键词进行筛选。
     * </p>
     *
     * @param provider Provider 名称（可选）
     * @param isActive 活跃状态（可选）
     * @param keyword  搜索关键词（可选）
     * @return 筛选后的模型配置响应 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<ModelResponse> findByFilters(String provider, Boolean isActive, String keyword) {
        ModelProvider providerEnum = provider != null ? ModelProvider.fromString(provider) : null;
        return repository.findByFilters(providerEnum, isActive, keyword).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建新的模型配置
     * <p>
     * 创建流程：
     * <ol>
     *   <li>检查名称唯一性</li>
     *   <li>生成 ID</li>
     *   <li>加密 API Key</li>
     *   <li>设置默认 baseUrl（如果未提供）</li>
     *   <li>如果是第一个模型或标记为默认，设置为默认模型</li>
     * </ol>
     * </p>
     *
     * @param request 创建模型配置请求
     * @return 创建成功的模型配置响应 DTO
     * @throws IllegalArgumentException 模型名称已存在时抛出
     */
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

    /**
     * 更新模型配置
     * <p>
     * 支持更新名称、模型名称、baseUrl、API Key 等配置。
     * API Key 如果未加密则自动加密。
     * </p>
     *
     * @param id      模型配置唯一标识
     * @param request 更新请求
     * @return 更新后的模型配置响应 DTO
     * @throws IllegalArgumentException 模型不存在或新名称已被占用时抛出
     */
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

    /**
     * 删除模型配置
     * <p>
     * 不允许删除默认模型，需要先设置其他模型为默认。
     * </p>
     *
     * @param id 模型配置唯一标识
     * @throws IllegalArgumentException 模型不存在或为默认模型时抛出
     */
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

    /**
     * 设置指定模型为默认模型
     * <p>
     * 先清除当前默认模型标记，再设置新的默认模型。
     * </p>
     *
     * @param id 模型配置唯一标识
     * @throws IllegalArgumentException 模型不存在时抛出
     */
    @Transactional
    public void setDefault(String id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Model not found: " + id);
        }

        repository.clearDefaultModel();
        repository.setDefaultModel(id);
        log.info("Set model {} as default", id);
    }

    /**
     * 更新模型的健康检查状态
     * <p>
     * 记录健康状态和最后检查时间。
     * </p>
     *
     * @param id     模型配置唯一标识
     * @param status 健康状态（HEALTHY、UNHEALTHY、UNKNOWN）
     */
    @Transactional
    public void updateHealthStatus(String id, ModelConfig.HealthStatus status) {
        repository.findById(id).ifPresent(entity -> {
            entity.setHealthStatus(status);
            entity.setLastHealthCheck(java.time.LocalDateTime.now());
            repository.save(entity);
        });
    }

    /**
     * 获取解密后的 API Key
     * <p>
     * 用于内部调用 AI 模型时获取真实的 API Key。
     * </p>
     *
     * @param id 模型配置唯一标识
     * @return 解密后的 API Key，如果模型不存在则返回 null
     */
    @Transactional(readOnly = true)
    public String getDecryptedApiKey(String id) {
        return repository.findById(id)
                .map(ModelConfig::getApiKey)
                .map(encryptionService::decrypt)
                .orElse(null);
    }
}
