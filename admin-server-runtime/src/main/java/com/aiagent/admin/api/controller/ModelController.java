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

    @GetMapping
    @Operation(summary = "List all models with optional filters")
    public ApiResponse<List<ModelResponse>> listModels(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(modelConfigService.findByFilters(provider, isActive, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get model by ID")
    public ApiResponse<ModelResponse> getModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        return modelConfigService.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Model not found"));
    }

    @PostMapping
    @Operation(summary = "Create a new model configuration")
    public ApiResponse<ModelResponse> createModel(
            @Valid @RequestBody CreateModelRequest request) {
        return ApiResponse.success(modelConfigService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update model configuration")
    public ApiResponse<ModelResponse> updateModel(
            @Parameter(description = "Model ID") @PathVariable String id,
            @Valid @RequestBody UpdateModelRequest request) {
        return ApiResponse.success(modelConfigService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete model configuration")
    public ApiResponse<Void> deleteModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        modelConfigService.delete(id);
        return ApiResponse.success();
    }

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

    @PostMapping("/{id}/default")
    @Operation(summary = "Set model as default")
    public ApiResponse<Void> setDefaultModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        modelConfigService.setDefault(id);
        return ApiResponse.success();
    }

    @GetMapping("/default")
    @Operation(summary = "Get current default model")
    public ApiResponse<ModelResponse> getDefaultModel() {
        return modelConfigService.findDefault()
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "No default model configured"));
    }

    @GetMapping("/providers")
    @Operation(summary = "List all supported providers")
    public ApiResponse<List<ProviderResponse>> listProviders() {
        List<ProviderResponse> providers = Arrays.stream(ModelProvider.values())
                .map(p -> new ProviderResponse(
                        p.name(),
                        p.getDisplayName(),
                        p.getDefaultBaseUrl(),
                        p.getBuiltinModels()
                ))
                .collect(Collectors.toList());
        return ApiResponse.success(providers);
    }

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

    @PostMapping("/switch")
    @Operation(summary = "Switch to a different model at runtime")
    public ApiResponse<Void> switchModel(
            @Valid @RequestBody SwitchModelRequest request) {
        modelService.switchModel(request.getModelId());
        return ApiResponse.success();
    }

    // Response DTOs
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
    public record ActiveModelResponse(String modelId, ModelResponse model) {}
}