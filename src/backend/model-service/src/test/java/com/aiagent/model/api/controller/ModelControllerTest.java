package com.aiagent.model.api.controller;

import com.aiagent.model.domain.entity.ModelConfig;
import com.aiagent.model.domain.entity.ModelProvider;
import com.aiagent.model.service.HealthCheckService;
import com.aiagent.model.service.ModelConfigService;
import com.aiagent.model.service.ModelService;
import com.aiagent.model.service.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModelController.class)
class ModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModelConfigService modelConfigService;

    @MockBean
    private ModelService modelService;

    @MockBean
    private HealthCheckService healthCheckService;

    private ModelResponse testResponse;

    @BeforeEach
    void setUp() {
        testResponse = new ModelResponse();
        testResponse.setId("mdl_test123");
        testResponse.setName("Test Model");
        testResponse.setProvider(ModelProvider.OPENAI);
        testResponse.setModelName("gpt-4");
        testResponse.setIsDefault(false);
        testResponse.setIsActive(true);
        testResponse.setHealthStatus(ModelConfig.HealthStatus.HEALTHY);
    }

    @Test
    void testListModels() throws Exception {
        when(modelConfigService.findByFilters(any(), any(), any()))
                .thenReturn(Arrays.asList(testResponse));

        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("mdl_test123"))
                .andExpect(jsonPath("$[0].name").value("Test Model"));
    }

    @Test
    void testGetModel() throws Exception {
        when(modelConfigService.findById("mdl_test123")).thenReturn(Optional.of(testResponse));

        mockMvc.perform(get("/api/v1/models/mdl_test123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mdl_test123"))
                .andExpect(jsonPath("$.name").value("Test Model"));
    }

    @Test
    void testGetModelNotFound() throws Exception {
        when(modelConfigService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/models/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateModel() throws Exception {
        CreateModelRequest request = new CreateModelRequest();
        request.setName("New Model");
        request.setProvider(ModelProvider.DASHSCOPE);
        request.setModelName("qwen-turbo");

        when(modelConfigService.create(any(CreateModelRequest.class))).thenReturn(testResponse);

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("mdl_test123"));
    }

    @Test
    void testCreateModelValidationError() throws Exception {
        CreateModelRequest request = new CreateModelRequest();
        // Missing required fields

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateModel() throws Exception {
        UpdateModelRequest request = new UpdateModelRequest();
        request.setName("Updated Model");
        request.setProvider(ModelProvider.OPENAI);
        request.setModelName("gpt-4-turbo");

        when(modelConfigService.update(eq("mdl_test123"), any(UpdateModelRequest.class)))
                .thenReturn(testResponse);

        mockMvc.perform(put("/api/v1/models/mdl_test123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mdl_test123"));
    }

    @Test
    void testDeleteModel() throws Exception {
        doNothing().when(modelConfigService).delete("mdl_test123");

        mockMvc.perform(delete("/api/v1/models/mdl_test123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testTestModel() throws Exception {
        when(healthCheckService.healthCheck("mdl_test123")).thenReturn(true);

        mockMvc.perform(post("/api/v1/models/mdl_test123/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value("mdl_test123"))
                .andExpect(jsonPath("$.healthy").value(true));
    }

    @Test
    void testSetDefaultModel() throws Exception {
        doNothing().when(modelConfigService).setDefault("mdl_test123");

        mockMvc.perform(post("/api/v1/models/mdl_test123/default"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetDefaultModel() throws Exception {
        when(modelConfigService.findDefault()).thenReturn(Optional.of(testResponse));

        mockMvc.perform(get("/api/v1/models/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mdl_test123"));
    }

    @Test
    void testGetDefaultModelNotFound() throws Exception {
        when(modelConfigService.findDefault()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/models/default"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListProviders() throws Exception {
        mockMvc.perform(get("/api/v1/models/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ModelProvider.values().length));
    }

    @Test
    void testGetBuiltinModels() throws Exception {
        mockMvc.perform(get("/api/v1/models/providers/OPENAI/builtin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ModelProvider.OPENAI.getBuiltinModels().size()));
    }

    @Test
    void testGetBuiltinModelsInvalidProvider() throws Exception {
        mockMvc.perform(get("/api/v1/models/providers/INVALID/builtin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetActiveModel() throws Exception {
        when(modelService.getActiveModelId()).thenReturn("mdl_test123");
        when(modelConfigService.findById("mdl_test123")).thenReturn(Optional.of(testResponse));

        mockMvc.perform(get("/api/v1/models/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value("mdl_test123"));
    }

    @Test
    void testSwitchModel() throws Exception {
        SwitchModelRequest request = new SwitchModelRequest();
        request.setModelId("mdl_test123");

        doNothing().when(modelService).switchModel("mdl_test123");

        mockMvc.perform(post("/api/v1/models/switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}