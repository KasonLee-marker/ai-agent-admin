package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.service.HealthCheckService;
import com.aiagent.admin.service.ModelConfigService;
import com.aiagent.admin.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "Model Management", description = "APIs for managing AI models")
public class ModelController {

    private final ModelConfigService modelConfigService;
    private final ModelService modelService;
    private final HealthCheckService healthCheckService;

    @GetMapping
    @Operation(summary = "List all models with optional filters")
    public ResponseEntity<List<ModelResponse>> listModels(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(modelConfigService.findByFilters(provider, isActive, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get model by ID")
    public ResponseEntity<ModelResponse> getModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        return modelConfigService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new model configuration")
    public ResponseEntity<ModelResponse> createModel(
            @Valid @RequestBody CreateModelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modelConfigService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update model configuration")
    public ResponseEntity<ModelResponse> updateModel(
            @Parameter(description = "Model ID") @PathVariable String id,
            @Valid @RequestBody UpdateModelRequest request) {
        return ResponseEntity.ok(modelConfigService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete model configuration")
    public ResponseEntity<Void> deleteModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        modelConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Test model connectivity (health check)")
    public ResponseEntity<HealthCheckResponse> testModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        boolean isHealthy = healthCheckService.healthCheck(id);
        return ResponseEntity.ok(new HealthCheckResponse(id, isHealthy));
    }

    @PostMapping("/{id}/default")
    @Operation(summary = "Set model as default")
    public ResponseEntity<Void> setDefaultModel(
            @Parameter(description = "Model ID") @PathVariable String id) {
        modelConfigService.setDefault(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/default")
    @Operation(summary = "Get current default model")
    public ResponseEntity<ModelResponse> getDefaultModel() {
        return modelConfigService.findDefault()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/providers")
    @Operation(summary = "List all supported providers")
    public ResponseEntity<List<ProviderResponse>> listProviders() {
        List<ProviderResponse> providers = Arrays.stream(ModelProvider.values())
                .map(p -> new ProviderResponse(
                        p.name(),
                        p.getDisplayName(),
                        p.getDefaultBaseUrl(),
                        p.getBuiltinModels()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/providers/{provider}/builtin")
    @Operation(summary = "Get built-in models for a provider")
    public ResponseEntity<List<ModelProvider.BuiltinModel>> getBuiltinModels(
            @Parameter(description = "Provider name") @PathVariable String provider) {
        try {
            ModelProvider p = ModelProvider.fromString(provider);
            return ResponseEntity.ok(p.getBuiltinModels());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Get currently active model (for runtime switching)")
    public ResponseEntity<ActiveModelResponse> getActiveModel() {
        String activeId = modelService.getActiveModelId();
        if (activeId == null) {
            return ResponseEntity.notFound().build();
        }
        return modelConfigService.findById(activeId)
                .map(model -> ResponseEntity.ok(new ActiveModelResponse(activeId, model)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/switch")
    @Operation(summary = "Switch to a different model at runtime")
    public ResponseEntity<Void> switchModel(
            @Valid @RequestBody SwitchModelRequest request) {
        modelService.switchModel(request.getModelId());
        return ResponseEntity.ok().build();
    }

    // Response DTOs
    public record HealthCheckResponse(String modelId, boolean healthy) {}
    public record ActiveModelResponse(String modelId, ModelResponse model) {}
}