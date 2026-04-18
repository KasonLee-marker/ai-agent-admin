package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.HealthCheckService;
import com.aiagent.admin.service.ModelConfigService;
import com.aiagent.admin.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 模型配置管理 REST 控制器
 * <p>
 * 提供模型配置管理和运行时切换的 API：
 * <ul>
 *   <li>模型配置的 CRUD 操作</li>
 *   <li>模型连通性测试（健康检查）</li>
 *   <li>默认模型设置</li>
 *   <li>支持的 Provider 和内置模型查询</li>
 *   <li>运行时模型切换</li>
 * </ul>
 * </p>
 *
 * @see ModelConfigService
 * @see ModelService
 * @see HealthCheckService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "Model Management", description = "APIs for managing AI models")
public class ModelController {

    private final ModelConfigService modelConfigService;
    private final ModelService modelService;
    private final HealthCheckService healthCheckService;
    private final EncryptionService encryptionService;

    /**
     * 分页查询模型配置列表
     * <p>
     * 支持按 Provider、激活状态、关键词筛选。
     * </p>
     *
     * @param provider Provider筛选（可选，如 OPENAI、DASHSCOPE）
     * @param isActive 激活状态筛选（可选）
     * @param keyword  搜索关键词（可选，匹配模型名称）
     * @return 模型配置列表
     */
    @GetMapping
    @Operation(summary = "List all models with optional filters")
    public ApiResponse<List<ModelResponse>> listModels(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(modelConfigService.findByFilters(provider, isActive, keyword));
    }

    /**
     * 根据ID获取模型配置详情
     *
     * @param id 模型配置ID
     * @return 模型配置详情信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get model by ID")
    public ApiResponse<ModelResponse> getModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        return modelConfigService.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Model not found"));
    }

    /**
     * 创建新的模型配置
     *
     * @param request 创建请求，包含 Provider、模型名称、API Key 等
     * @return 创建成功的模型配置信息
     */
    @PostMapping
    @Operation(summary = "Create a new model configuration")
    public ApiResponse<ModelResponse> createModel(
            @Valid @RequestBody CreateModelRequest request) {
        return ApiResponse.success(modelConfigService.create(request));
    }

    /**
     * 更新模型配置
     *
     * @param id      模型配置ID
     * @param request 更新请求
     * @return 更新后的模型配置信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update model configuration")
    public ApiResponse<ModelResponse> updateModel(
            @Parameter(description = "Model ID") @PathVariable String id,
            @Valid @RequestBody UpdateModelRequest request) {
        return ApiResponse.success(modelConfigService.update(id, request));
    }

    /**
     * 删除模型配置
     *
     * @param id 模型配置ID
     * @return 成功响应（无数据）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete model configuration")
    public ApiResponse<Void> deleteModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        modelConfigService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 测试模型连通性（健康检查）
     * <p>
     * 使用配置的 API Key 和端点测试模型是否可访问。
     * </p>
     *
     * @param id 模型配置ID
     * @return 健康检查结果，包含连接状态和详情
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "Test model connectivity (health check)")
    public ApiResponse<HealthCheckResponse> testModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        try {
            ModelConfig config = modelConfigService.findEntityById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found"));

            String apiKey = encryptionService.decrypt(config.getApiKey());
            String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : config.getProvider().getDefaultBaseUrl();

            log.info("Testing model: name={}, model={}, baseUrl={}, provider={}",
                    config.getName(), config.getModelName(), baseUrl, config.getProvider());

            boolean isHealthy = healthCheckService.healthCheck(id);

            HealthCheckResponse response = new HealthCheckResponse();
            response.setModelId(id);
            response.setHealthy(isHealthy);
            response.setModelName(config.getModelName());
            response.setBaseUrl(baseUrl);
            response.setDetails(isHealthy ? "Connection successful" : "Connection failed - check model name and API endpoint compatibility");

            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Health check error: {}", e.getMessage());
            return ApiResponse.error(500, "Health check failed: " + e.getMessage());
        }
    }

    /**
     * 设置模型为默认聊天模型
     * <p>
     * 用于对话功能的默认模型选择。
     * </p>
     *
     * @param id 模型配置ID
     * @return 成功响应（无数据）
     */
    @PostMapping("/{id}/default")
    @Operation(summary = "Set model as default chat model")
    public ApiResponse<Void> setDefaultModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        modelConfigService.setDefault(id);
        return ApiResponse.success();
    }

    /**
     * 设置模型为默认 Embedding 模型
     * <p>
     * 用于向量计算和文档检索的默认模型选择。
     * </p>
     *
     * @param id 模型配置ID
     * @return 成功响应（无数据）
     */
    @PostMapping("/{id}/default-embedding")
    @Operation(summary = "Set model as default embedding model")
    public ApiResponse<Void> setDefaultEmbeddingModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        modelConfigService.setDefaultEmbedding(id);
        return ApiResponse.success();
    }

    /**
     * 获取当前默认聊天模型
     *
     * @return 默认聊天模型配置信息
     */
    @GetMapping("/default")
    @Operation(summary = "Get current default model")
    public ApiResponse<ModelResponse> getDefaultModel() {
        return modelConfigService.findDefault()
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "No default model configured"));
    }

    /**
     * 获取当前默认 Embedding 模型
     *
     * @return 默认 Embedding 模型配置信息
     */
    @GetMapping("/default-embedding")
    @Operation(summary = "Get current default embedding model")
    public ApiResponse<ModelResponse> getDefaultEmbeddingModel() {
        return modelConfigService.findDefaultEmbedding()
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "No default embedding model configured"));
    }

    /**
     * 获取所有支持的 Provider 列表
     * <p>
     * 返回系统支持的 AI 模型提供商及其默认配置。
     * </p>
     *
     * @return Provider 列表，包含名称、显示名称、默认端点、内置模型等
     */
    @GetMapping("/providers")
    @Operation(summary = "List all supported providers")
    public ApiResponse<List<ProviderResponse>> listProviders() {
        List<ProviderResponse> providers = Arrays.stream(ModelProvider.values())
                .map(p -> new ProviderResponse(
                        p.name(),
                        p.getDisplayName(),
                        p.getDefaultBaseUrl(),
                        p.getModelType().name(),
                        p.getBuiltinModels()
                ))
                .collect(Collectors.toList());
        return ApiResponse.success(providers);
    }

    /**
     * 获取指定 Provider 的内置模型列表
     * <p>
     * 返回 Provider 预配置的常用模型及其能力信息。
     * </p>
     *
     * @param provider Provider 名称（如 OPENAI、DASHSCOPE）
     * @return 内置模型列表
     */
    @GetMapping("/providers/{provider}/builtin")
    @Operation(summary = "Get built-in models for a provider")
    public ApiResponse<List<ModelProvider.BuiltinModel>> getBuiltinModels(
            @Parameter(description = "Provider name") @PathVariable String provider) {
        try {
            ModelProvider p = ModelProvider.fromString(provider);
            return ApiResponse.success(p.getBuiltinModels());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, "Provider not found: " + provider);
        }
    }

    /**
     * 获取当前激活的模型
     * <p>
     * 用于运行时模型切换功能，返回当前正在使用的模型信息。
     * </p>
     *
     * @return 当前激活的模型信息
     */
    @GetMapping("/active")
    @Operation(summary = "Get currently active model (for runtime switching)")
    public ApiResponse<ActiveModelResponse> getActiveModel() {
        String activeId = modelService.getActiveModelId();
        if (activeId == null) {
            return ApiResponse.error(404, "No active model");
        }
        return modelConfigService.findById(activeId)
                .map(model -> ApiResponse.success(new ActiveModelResponse(activeId, model)))
                .orElse(ApiResponse.error(404, "Active model not found"));
    }

    /**
     * 运行时切换模型
     * <p>
     * 无需重启服务即可切换当前使用的 AI 模型。
     * </p>
     *
     * @param request 切换请求，包含目标模型ID
     * @return 成功响应（无数据）
     */
    @PostMapping("/switch")
    @Operation(summary = "Switch to a different model at runtime")
    public ApiResponse<Void> switchModel(
            @Valid @RequestBody SwitchModelRequest request) {
        modelService.switchModel(request.getModelId());
        return ApiResponse.success();
    }

    // Response DTOs

    /**
     * 健康检查响应 DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class HealthCheckResponse {
        private String modelId;
        private boolean healthy;
        private String modelName;
        private String baseUrl;
        private String details;
    }

    /**
     * 激活模型响应 DTO
     */
    public record ActiveModelResponse(String modelId, ModelResponse model) {}
}