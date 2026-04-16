package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.RagSessionDTO;
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
    private RagSessionService ragSessionService;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private RagServiceImpl ragService;

    private ModelConfig testModelConfig;
    private RagSessionDTO testSession;

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

        testSession = RagSessionDTO.builder()
                .id("session-123")
                .messageCount(0)
                .build();

        // 不在 setUp 中 mock，因为测试设置了 sessionId 会跳过创建
    }

    @Test
    void testChat_NoDefaultModel() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("What is AI?");
        request.setSessionId("existing-session"); // 使用现有会话，跳过创建

        when(modelConfigRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> ragService.chat(request));
    }

    @Test
    void testChat_ModelNotFound() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("What is AI?");
        request.setModelId("nonexistent");
        request.setSessionId("existing-session"); // 使用现有会话，跳过创建

        when(modelConfigRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> ragService.chat(request));
    }
}