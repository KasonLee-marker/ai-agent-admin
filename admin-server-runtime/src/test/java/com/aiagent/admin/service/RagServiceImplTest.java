package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.impl.RagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceImplTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private ModelConfigRepository modelConfigRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private RagServiceImpl ragService;

    private ModelConfig testModelConfig;

    @BeforeEach
    void setUp() {
        testModelConfig = ModelConfig.builder()
                .id("model-123")
                .name("Test Model")
                .provider(ModelProvider.DASHSCOPE)
                .modelName("qwen-turbo")
                .apiKey("encrypted-key")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .isDefault(true)
                .isActive(true)
                .build();
    }

    @Test
    void testChat_NoDefaultModel() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("What is AI?");

        when(modelConfigRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> ragService.chat(request));
    }

    @Test
    void testChat_ModelNotFound() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("What is AI?");
        request.setModelId("nonexistent");

        when(modelConfigRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> ragService.chat(request));
    }
}