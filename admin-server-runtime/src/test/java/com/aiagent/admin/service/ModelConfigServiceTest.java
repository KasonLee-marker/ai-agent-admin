package com.aiagent.admin.service;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.api.dto.CreateModelRequest;
import com.aiagent.admin.api.dto.ModelResponse;
import com.aiagent.admin.api.dto.UpdateModelRequest;
import com.aiagent.admin.service.mapper.ModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceTest {

    @Mock
    private ModelConfigRepository repository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private ModelConfigService modelConfigService;

    private ModelConfig testConfig;
    private ModelResponse testResponse;

    @BeforeEach
    void setUp() {
        testConfig = ModelConfig.builder()
                .id("mdl_test123")
                .name("Test Model")
                .provider(ModelProvider.OPENAI)
                .modelName("gpt-4")
                .apiKey("ENC(encrypted)")
                .baseUrl("https://api.openai.com/v1")
                .temperature(0.7)
                .maxTokens(2048)
                .topP(1.0)
                .isDefault(false)
                .isActive(true)
                .healthStatus(ModelConfig.HealthStatus.HEALTHY)
                .build();

        testResponse = new ModelResponse();
        testResponse.setId("mdl_test123");
        testResponse.setName("Test Model");
        testResponse.setProvider(ModelProvider.OPENAI);
        testResponse.setModelName("gpt-4");
    }

    @Test
    void testFindAll() {
        when(repository.findAll()).thenReturn(Arrays.asList(testConfig));
        when(mapper.toResponse(testConfig)).thenReturn(testResponse);

        List<ModelResponse> result = modelConfigService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Model", result.get(0).getName());
    }

    @Test
    void testFindById() {
        when(repository.findById("mdl_test123")).thenReturn(Optional.of(testConfig));
        when(mapper.toResponse(testConfig)).thenReturn(testResponse);

        Optional<ModelResponse> result = modelConfigService.findById("mdl_test123");

        assertTrue(result.isPresent());
        assertEquals("Test Model", result.get().getName());
    }

    @Test
    void testFindByIdNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<ModelResponse> result = modelConfigService.findById("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void testCreate() {
        CreateModelRequest request = new CreateModelRequest();
        request.setName("New Model");
        request.setProvider(ModelProvider.DASHSCOPE);
        request.setModelName("qwen-turbo");
        request.setApiKey("plain-api-key");

        ModelConfig newConfig = ModelConfig.builder()
                .name("New Model")
                .provider(ModelProvider.DASHSCOPE)
                .modelName("qwen-turbo")
                .apiKey("plain-api-key")
                .build();

        when(repository.existsByName("New Model")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(newConfig);
        when(idGenerator.generateId()).thenReturn("mdl_new123");
        when(encryptionService.encrypt("plain-api-key")).thenReturn("ENC(encrypted-key)");
        when(repository.count()).thenReturn(0L);
        when(repository.save(any(ModelConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(ModelConfig.class))).thenReturn(testResponse);

        ModelResponse result = modelConfigService.create(request);

        assertNotNull(result);
        verify(encryptionService).encrypt("plain-api-key");
        verify(repository).clearDefaultModel();
    }

    @Test
    void testCreateDuplicateName() {
        CreateModelRequest request = new CreateModelRequest();
        request.setName("Existing Model");

        when(repository.existsByName("Existing Model")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> modelConfigService.create(request));
    }

    @Test
    void testUpdate() {
        UpdateModelRequest request = new UpdateModelRequest();
        request.setName("Updated Model");
        request.setProvider(ModelProvider.OPENAI);
        request.setModelName("gpt-4-turbo");

        when(repository.findById("mdl_test123")).thenReturn(Optional.of(testConfig));
        when(repository.existsByName("Updated Model")).thenReturn(false);
        doNothing().when(mapper).updateEntityFromRequest(request, testConfig);
        when(repository.save(any(ModelConfig.class))).thenReturn(testConfig);
        when(mapper.toResponse(testConfig)).thenReturn(testResponse);

        ModelResponse result = modelConfigService.update("mdl_test123", request);

        assertNotNull(result);
        verify(mapper).updateEntityFromRequest(request, testConfig);
    }

    @Test
    void testUpdateNotFound() {
        UpdateModelRequest request = new UpdateModelRequest();
        request.setName("Updated Model");
        request.setProvider(ModelProvider.OPENAI);
        request.setModelName("gpt-4");

        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> modelConfigService.update("nonexistent", request));
    }

    @Test
    void testDelete() {
        testConfig.setIsDefault(false);
        when(repository.findById("mdl_test123")).thenReturn(Optional.of(testConfig));

        modelConfigService.delete("mdl_test123");

        verify(repository).deleteById("mdl_test123");
    }

    @Test
    void testDeleteDefaultModel() {
        testConfig.setIsDefault(true);
        when(repository.findById("mdl_test123")).thenReturn(Optional.of(testConfig));

        assertThrows(IllegalArgumentException.class, () -> modelConfigService.delete("mdl_test123"));
    }

    @Test
    void testSetDefault() {
        when(repository.existsById("mdl_test123")).thenReturn(true);

        modelConfigService.setDefault("mdl_test123");

        verify(repository).clearDefaultModel();
        verify(repository).setDefaultModel("mdl_test123");
    }

    @Test
    void testSetDefaultNotFound() {
        when(repository.existsById("nonexistent")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> modelConfigService.setDefault("nonexistent"));
    }

    @Test
    void testGetDecryptedApiKey() {
        when(repository.findById("mdl_test123")).thenReturn(Optional.of(testConfig));
        when(encryptionService.decrypt("ENC(encrypted)")).thenReturn("decrypted-key");

        String result = modelConfigService.getDecryptedApiKey("mdl_test123");

        assertEquals("decrypted-key", result);
    }

    @Test
    void testFindByFilters() {
        when(repository.findByFilters(eq(ModelProvider.OPENAI), eq(true), eq("test")))
                .thenReturn(Arrays.asList(testConfig));
        when(mapper.toResponse(testConfig)).thenReturn(testResponse);

        List<ModelResponse> result = modelConfigService.findByFilters("OPENAI", true, "test");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateHealthStatus() {
        when(repository.findById("mdl_test123")).thenReturn(Optional.of(testConfig));
        when(repository.save(any(ModelConfig.class))).thenReturn(testConfig);

        modelConfigService.updateHealthStatus("mdl_test123", ModelConfig.HealthStatus.HEALTHY);

        assertEquals(ModelConfig.HealthStatus.HEALTHY, testConfig.getHealthStatus());
        assertNotNull(testConfig.getLastHealthCheck());
    }
}
