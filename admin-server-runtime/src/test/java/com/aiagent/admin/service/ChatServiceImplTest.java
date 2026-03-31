package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.ChatRequest;
import com.aiagent.admin.api.dto.ChatResponse;
import com.aiagent.admin.api.dto.ChatSessionDTO;
import com.aiagent.admin.domain.entity.ChatMessage;
import com.aiagent.admin.domain.entity.ChatSession;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.MessageRole;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.ChatMessageRepository;
import com.aiagent.admin.domain.repository.ChatSessionRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.impl.ChatServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ModelConfigRepository modelConfigRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private ChatServiceImpl chatService;

    private ChatSession testSession;
    private ModelConfig testModel;
    private ChatMessage testUserMessage;
    private ChatMessage testAssistantMessage;

    @BeforeEach
    void setUp() {
        testSession = ChatSession.builder()
                .id("session-123")
                .title("Test Session")
                .modelId("model-123")
                .messageCount(0)
                .isActive(true)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testModel = ModelConfig.builder()
                .id("model-123")
                .name("Test Model")
                .provider(ModelProvider.DASHSCOPE)
                .modelName("qwen-turbo")
                .apiKey("encrypted-key")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .isActive(true)
                .build();

        testUserMessage = ChatMessage.builder()
                .id("msg-1")
                .sessionId("session-123")
                .role(MessageRole.USER)
                .content("Hello")
                .createdAt(LocalDateTime.now())
                .build();

        testAssistantMessage = ChatMessage.builder()
                .id("msg-2")
                .sessionId("session-123")
                .role(MessageRole.ASSISTANT)
                .content("Hi there!")
                .modelName("qwen-turbo")
                .latencyMs(100L)
                .isError(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createSession_Success() {
        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("New Session");
        request.setModelId("model-123");
        request.setSystemMessage("You are a helpful assistant");

        when(idGenerator.generateId()).thenReturn("new-session-id");
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            return session;
        });

        ChatSessionDTO result = chatService.createSession(request, "test-user");

        assertNotNull(result);
        assertEquals("new-session-id", result.getId());
        assertEquals("New Session", result.getTitle());
        assertEquals("model-123", result.getModelId());
        assertEquals("test-user", result.getCreatedBy());
        verify(chatSessionRepository).save(any(ChatSession.class));
    }

    @Test
    void getSession_Success() {
        when(chatSessionRepository.findById("session-123")).thenReturn(Optional.of(testSession));

        ChatSessionDTO result = chatService.getSession("session-123");

        assertNotNull(result);
        assertEquals("session-123", result.getId());
        assertEquals("Test Session", result.getTitle());
    }

    @Test
    void getSession_NotFound_ThrowsException() {
        when(chatSessionRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> chatService.getSession("non-existent"));
    }

    @Test
    void listSessions_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ChatSession> page = new PageImpl<>(Collections.singletonList(testSession), pageable, 1);

        when(chatSessionRepository.findByCreatedByOrderByUpdatedAtDesc("test-user", pageable)).thenReturn(page);

        var result = chatService.listSessions("test-user", pageable);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("session-123", result.get(0).getId());
    }

    @Test
    void listSessionsByKeyword_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ChatSession> page = new PageImpl<>(Collections.singletonList(testSession), pageable, 1);

        when(chatSessionRepository.findByCreatedByAndKeyword("test-user", "Test", pageable)).thenReturn(page);

        var result = chatService.listSessionsByKeyword("test-user", "Test", pageable);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void deleteSession_Success() {
        when(chatSessionRepository.existsById("session-123")).thenReturn(true);

        chatService.deleteSession("session-123");

        verify(chatMessageRepository).deleteBySessionId("session-123");
        verify(chatSessionRepository).deleteById("session-123");
    }

    @Test
    void deleteSession_NotFound_ThrowsException() {
        when(chatSessionRepository.existsById("non-existent")).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> chatService.deleteSession("non-existent"));
    }

    @Test
    void getSessionMessages_Success() {
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-123"))
                .thenReturn(Collections.singletonList(testUserMessage));

        var result = chatService.getSessionMessages("session-123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("msg-1", result.get(0).getId());
        assertEquals(MessageRole.USER, result.get(0).getRole());
    }

    @Test
    void getConversationHistory_Success() {
        when(chatMessageRepository.findConversationHistory("session-123"))
                .thenReturn(Collections.singletonList(testAssistantMessage));

        var result = chatService.getConversationHistory("session-123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(MessageRole.ASSISTANT, result.get(0).getRole());
    }

    @Test

    void toSessionDTO_MapsAllFields() {
        when(chatSessionRepository.findById("session-123")).thenReturn(Optional.of(testSession));

        ChatSessionDTO result = chatService.getSession("session-123");

        assertEquals(testSession.getId(), result.getId());
        assertEquals(testSession.getTitle(), result.getTitle());
        assertEquals(testSession.getModelId(), result.getModelId());
        assertEquals(testSession.getMessageCount(), result.getMessageCount());
        assertEquals(testSession.getIsActive(), result.getIsActive());
        assertEquals(testSession.getCreatedBy(), result.getCreatedBy());
    }

    @Test
    void toMessageDTO_MapsAllFields() {
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-123"))
                .thenReturn(Collections.singletonList(testAssistantMessage));

        var result = chatService.getSessionMessages("session-123");

        assertEquals(1, result.size());
        ChatResponse dto = result.get(0);
        assertEquals(testAssistantMessage.getId(), dto.getId());
        assertEquals(testAssistantMessage.getSessionId(), dto.getSessionId());
        assertEquals(testAssistantMessage.getRole(), dto.getRole());
        assertEquals(testAssistantMessage.getContent(), dto.getContent());
        assertEquals(testAssistantMessage.getModelName(), dto.getModelName());
        assertEquals(testAssistantMessage.getLatencyMs(), dto.getLatencyMs());
        assertEquals(testAssistantMessage.getIsError(), dto.getIsError());
    }
}
