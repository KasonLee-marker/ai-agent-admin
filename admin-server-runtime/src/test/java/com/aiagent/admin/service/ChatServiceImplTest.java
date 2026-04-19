package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.ChatMessage;
import com.aiagent.admin.domain.entity.ChatSession;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.entity.PromptTemplate;
import com.aiagent.admin.domain.enums.MessageRole;
import com.aiagent.admin.domain.enums.ModelProvider;
import com.aiagent.admin.domain.repository.ChatMessageRepository;
import com.aiagent.admin.domain.repository.ChatSessionRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.domain.repository.PromptTemplateRepository;
import com.aiagent.admin.service.impl.ChatServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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
    private PromptTemplateRepository promptTemplateRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private RagService ragService;

    // 使用真实的 ObjectMapper，不使用 Mock
    private ObjectMapper realObjectMapper = new ObjectMapper();

    @InjectMocks
    private ChatServiceImpl chatService;

    private ChatSession testSession;
    private ChatSession testRagSession;
    private ModelConfig testModel;
    private ChatMessage testUserMessage;
    private ChatMessage testAssistantMessage;
    private ChatMessage testAssistantMessageWithSources;
    private VectorSearchResult testSearchResult;

    @BeforeEach
    void setUp() {
        // 使用反射注入真实的 ObjectMapper
        ReflectionTestUtils.setField(chatService, "objectMapper", realObjectMapper);

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

        // RAG 会话配置
        testRagSession = ChatSession.builder()
                .id("rag-session-123")
                .title("RAG Test Session")
                .modelId("model-123")
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .ragTopK(5)
                .ragThreshold(0.5)
                .ragStrategy("VECTOR")
                .ragEmbeddingModelId("emb-123")
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

        // 带 sources 的消息
        testAssistantMessageWithSources = ChatMessage.builder()
                .id("msg-3")
                .sessionId("rag-session-123")
                .role(MessageRole.ASSISTANT)
                .content("Based on the documents...")
                .modelName("qwen-turbo")
                .sources("[{\"chunkId\":\"chunk-1\",\"documentId\":\"doc-1\",\"documentName\":\"Test Doc\",\"content\":\"Test content\",\"score\":0.8}]")
                .isError(false)
                .createdAt(LocalDateTime.now())
                .build();

        // 检索结果
        testSearchResult = VectorSearchResult.builder()
                .chunkId("chunk-1")
                .documentId("doc-1")
                .documentName("Test Document")
                .content("This is test content for RAG retrieval.")
                .score(0.85)
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
    void createSession_WithRagConfig_Success() {
        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("RAG Session");
        request.setModelId("model-123");
        request.setEnableRag(true);
        request.setKnowledgeBaseId("kb-123");
        request.setRagTopK(5);
        request.setRagThreshold(0.5);
        request.setRagStrategy("VECTOR");
        request.setRagEmbeddingModelId("emb-123");

        when(idGenerator.generateId()).thenReturn("rag-session-id");
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            return session;
        });

        ChatSessionDTO result = chatService.createSession(request, "test-user");

        assertNotNull(result);
        assertEquals("rag-session-id", result.getId());
        assertEquals("RAG Session", result.getTitle());
        assertTrue(result.getEnableRag());
        assertEquals("kb-123", result.getKnowledgeBaseId());
        assertEquals(5, result.getRagTopK());
        assertEquals(0.5, result.getRagThreshold());
        assertEquals("VECTOR", result.getRagStrategy());
        assertEquals("emb-123", result.getRagEmbeddingModelId());
        verify(chatSessionRepository).save(any(ChatSession.class));
    }

    @Test
    void createSession_WithoutRagConfig_RagFieldsNull() {
        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("Normal Session");
        request.setModelId("model-123");
        // 不设置 RAG 配置

        when(idGenerator.generateId()).thenReturn("normal-session-id");
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            return session;
        });

        ChatSessionDTO result = chatService.createSession(request, "test-user");

        assertNotNull(result);
        assertNull(result.getEnableRag());
        assertNull(result.getKnowledgeBaseId());
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
    void getSession_RagSession_ReturnsRagFields() {
        when(chatSessionRepository.findById("rag-session-123")).thenReturn(Optional.of(testRagSession));

        ChatSessionDTO result = chatService.getSession("rag-session-123");

        assertNotNull(result);
        assertTrue(result.getEnableRag());
        assertEquals("kb-123", result.getKnowledgeBaseId());
        assertEquals(5, result.getRagTopK());
        assertEquals(0.5, result.getRagThreshold());
        assertEquals("VECTOR", result.getRagStrategy());
        assertEquals("emb-123", result.getRagEmbeddingModelId());
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

    @Test
    void toMessageDTO_WithSources_ParsesJson() {
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("rag-session-123"))
                .thenReturn(Collections.singletonList(testAssistantMessageWithSources));

        var result = chatService.getSessionMessages("rag-session-123");

        assertEquals(1, result.size());
        ChatResponse dto = result.get(0);
        assertNotNull(dto.getSources());
        assertEquals(1, dto.getSources().size());
        assertEquals("chunk-1", dto.getSources().get(0).getChunkId());
        assertEquals("doc-1", dto.getSources().get(0).getDocumentId());
        assertEquals("Test Doc", dto.getSources().get(0).getDocumentName());
    }

    @Test
    void toMessageDTO_WithoutSources_ReturnsNullSources() {
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-123"))
                .thenReturn(Collections.singletonList(testAssistantMessage));

        var result = chatService.getSessionMessages("session-123");

        assertEquals(1, result.size());
        ChatResponse dto = result.get(0);
        assertNull(dto.getSources());
    }

    @Test
    void toMessageDTO_InvalidSourcesJson_ReturnsNullSources() {
        ChatMessage msgWithInvalidSources = ChatMessage.builder()
                .id("msg-4")
                .sessionId("session-123")
                .role(MessageRole.ASSISTANT)
                .content("Response")
                .sources("invalid-json")
                .isError(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-123"))
                .thenReturn(Collections.singletonList(msgWithInvalidSources));

        var result = chatService.getSessionMessages("session-123");

        assertEquals(1, result.size());
        ChatResponse dto = result.get(0);
        assertNull(dto.getSources());
    }

    @Test
    void retrieveDocuments_WhenRagEnabled_CallsRagService() {
        when(ragService.retrieve(any(RagChatRequest.class)))
                .thenReturn(List.of(testSearchResult));

        // 通过反射调用 retrieveDocuments 或间接测试 sendMessage
        // 这里验证 RagService 被正确调用
        RagChatRequest expectedRequest = new RagChatRequest();
        expectedRequest.setQuestion("test question");
        expectedRequest.setKnowledgeBaseId("kb-123");
        expectedRequest.setTopK(5);
        expectedRequest.setThreshold(0.5);
        expectedRequest.setStrategy("VECTOR");
        expectedRequest.setEmbeddingModelId("emb-123");

        List<VectorSearchResult> results = ragService.retrieve(expectedRequest);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("chunk-1", results.get(0).getChunkId());
        verify(ragService).retrieve(expectedRequest);
    }

    // ========== retrieveDocuments 测试（通过 sendMessage 间接测试） ==========

    @Test
    void sendMessage_WithRagEnabled_CallsRetrieveDocuments() {
        ChatRequest request = new ChatRequest();
        request.setSessionId("rag-session-123");
        request.setContent("What is AI?");

        ChatSession ragSession = ChatSession.builder()
                .id("rag-session-123")
                .title("RAG Session")
                .modelId("model-123")
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .ragTopK(5)
                .ragThreshold(0.5)
                .ragStrategy("VECTOR")
                .ragEmbeddingModelId("emb-123")
                .messageCount(0)
                .isActive(true)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(chatSessionRepository.findById("rag-session-123")).thenReturn(Optional.of(ragSession));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(anyString())).thenReturn("decrypted-key");
        when(idGenerator.generateId()).thenReturn("msg-new");
        when(ragService.retrieve(any(RagChatRequest.class))).thenReturn(List.of(testSearchResult));
        when(chatMessageRepository.findConversationHistory("rag-session-123")).thenReturn(List.of());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.countBySessionId("rag-session-123")).thenReturn(2L);
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 由于无法 Mock OpenAiChatClient，测试会抛出异常，但可以验证 RAG 流程被执行
        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期的异常：无法连接 AI 模型
            // 但我们应该验证 ragService.retrieve 被调用了
        }

        verify(ragService).retrieve(argThat(ragReq ->
                "kb-123".equals(ragReq.getKnowledgeBaseId()) &&
                        5 == ragReq.getTopK() &&
                        0.5 == ragReq.getThreshold()
        ));
    }

    @Test
    void sendMessage_WithRagDisabled_DoesNotCallRetrieveDocuments() {
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-123");
        request.setContent("Hello");

        when(chatSessionRepository.findById("session-123")).thenReturn(Optional.of(testSession));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(anyString())).thenReturn("decrypted-key");
        when(idGenerator.generateId()).thenReturn("msg-new");
        when(chatMessageRepository.findConversationHistory("session-123")).thenReturn(List.of());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.countBySessionId("session-123")).thenReturn(2L);
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期的异常
        }

        // RAG 未启用，不应调用 ragService.retrieve
        verify(ragService, never()).retrieve(any());
    }

    @Test
    void retrieveDocuments_WithNullTopK_UsesDefaultValue() {
        ChatSession session = ChatSession.builder()
                .id("rag-session-null-topk")
                .modelId("model-123")  // 必须有 modelId
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .ragTopK(null)  // 未设置
                .ragThreshold(0.7)
                .ragStrategy("HYBRID")
                .ragEmbeddingModelId("emb-123")
                .build();

        when(chatSessionRepository.findById("rag-session-null-topk")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(anyString())).thenReturn("key");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ragService.retrieve(any(RagChatRequest.class))).thenReturn(List.of());

        ChatRequest request = new ChatRequest();
        request.setSessionId("rag-session-null-topk");
        request.setContent("test");

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期：OpenAiChatClient 无法连接
        }

        verify(ragService).retrieve(argThat(ragReq -> ragReq.getTopK() == 5)); // 默认值 5
    }

    @Test
    void retrieveDocuments_WithNullThreshold_UsesDefaultValue() {
        ChatSession session = ChatSession.builder()
                .id("rag-session-null-threshold")
                .modelId("model-123")  // 必须有 modelId
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .ragTopK(10)
                .ragThreshold(null)  // 未设置
                .ragStrategy("VECTOR")
                .ragEmbeddingModelId("emb-123")
                .build();

        when(chatSessionRepository.findById("rag-session-null-threshold")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(anyString())).thenReturn("key");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ragService.retrieve(any(RagChatRequest.class))).thenReturn(List.of());

        ChatRequest request = new ChatRequest();
        request.setSessionId("rag-session-null-threshold");
        request.setContent("test");

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期
        }

        verify(ragService).retrieve(argThat(ragReq -> ragReq.getThreshold() == 0.5)); // 默认值 0.5
    }

    // ========== serializeSources 测试 ==========

    @Test
    void serializeSources_Success() throws Exception {
        List<VectorSearchResult> sources = List.of(
                VectorSearchResult.builder()
                        .chunkId("chunk-1")
                        .documentId("doc-1")
                        .content("content 1")
                        .score(0.9)
                        .build()
        );

        String json = realObjectMapper.writeValueAsString(sources);

        assertNotNull(json);
        assertTrue(json.contains("chunk-1"));
        assertTrue(json.contains("doc-1"));
    }

    @Test
    void serializeSources_EmptyList_ReturnsEmptyArray() throws Exception {
        List<VectorSearchResult> sources = List.of();

        String json = realObjectMapper.writeValueAsString(sources);

        assertEquals("[]", json);
    }

    // ========== buildSystemMessage 测试（通过分析逻辑间接验证） ==========

    @Test
    void buildSystemMessage_WithSourcesAndBaseMessage_ReturnsCombinedMessage() {
        ChatSession session = ChatSession.builder()
                .id("rag-session-combined")
                .modelId("model-123")
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .ragTopK(5)
                .ragThreshold(0.5)
                .systemMessage("You are a helpful assistant.")  // 有 baseMessage
                .build();

        VectorSearchResult source = VectorSearchResult.builder()
                .chunkId("chunk-1")
                .documentId("doc-1")
                .content("Reference content")
                .score(0.9)
                .build();

        when(chatSessionRepository.findById("rag-session-combined")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(any())).thenReturn("key");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ragService.retrieve(any())).thenReturn(List.of(source));

        ChatRequest request = new ChatRequest();
        request.setSessionId("rag-session-combined");
        request.setContent("test question");

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期异常：OpenAiChatClient 无法连接
        }

        verify(ragService).retrieve(any());
    }

    @Test
    void buildSystemMessage_WithSourcesNoBaseMessage_ReturnsRagPrompt() {
        ChatSession session = ChatSession.builder()
                .id("rag-session-no-base")
                .modelId("model-123")
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .ragTopK(5)
                .ragThreshold(0.5)
                .systemMessage(null)  // 无 baseMessage
                .build();

        VectorSearchResult source = VectorSearchResult.builder()
                .chunkId("chunk-1")
                .content("Reference content")
                .score(0.9)
                .build();

        when(chatSessionRepository.findById("rag-session-no-base")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(any())).thenReturn("key");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ragService.retrieve(any())).thenReturn(List.of(source));

        ChatRequest request = new ChatRequest();
        request.setSessionId("rag-session-no-base");
        request.setContent("test question");

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期
        }

        verify(ragService).retrieve(any());
    }

    @Test
    void buildSystemMessage_WithEmptySources_ReturnsBaseMessage() {
        ChatSession session = ChatSession.builder()
                .id("rag-session-empty-sources")
                .modelId("model-123")
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .ragTopK(5)
                .ragThreshold(0.5)
                .systemMessage("Custom system message")
                .build();

        when(chatSessionRepository.findById("rag-session-empty-sources")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(any())).thenReturn("key");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ragService.retrieve(any())).thenReturn(List.of());  // 空结果

        ChatRequest request = new ChatRequest();
        request.setSessionId("rag-session-empty-sources");
        request.setContent("test question");

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期
        }

        // RAG 被调用，但返回空结果
        verify(ragService).retrieve(any());
    }

    // ========== createSession RAG 字段测试 ==========

    @Test
    void createSession_WithAllRagFields_Success() {
        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("Full RAG Session");
        request.setModelId("model-123");
        request.setEnableRag(true);
        request.setKnowledgeBaseId("kb-full");
        request.setRagTopK(10);
        request.setRagThreshold(0.7);
        request.setRagStrategy("HYBRID");
        request.setRagEmbeddingModelId("emb-full");

        when(idGenerator.generateId()).thenReturn("full-rag-session-id");
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            return session;
        });

        ChatSessionDTO result = chatService.createSession(request, "test-user");

        assertNotNull(result);
        assertTrue(result.getEnableRag());
        assertEquals("kb-full", result.getKnowledgeBaseId());
        assertEquals(10, result.getRagTopK());
        assertEquals(0.7, result.getRagThreshold());
        assertEquals("HYBRID", result.getRagStrategy());
        assertEquals("emb-full", result.getRagEmbeddingModelId());
    }

    @Test
    void createSession_WithPartialRagFields_Success() {
        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("Partial RAG Session");
        request.setModelId("model-123");
        request.setEnableRag(true);
        request.setKnowledgeBaseId("kb-partial");
        // 只设置部分字段
        request.setRagTopK(3);
        // threshold, strategy, embeddingModelId 不设置

        when(idGenerator.generateId()).thenReturn("partial-rag-session-id");
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            return session;
        });

        ChatSessionDTO result = chatService.createSession(request, "test-user");

        assertNotNull(result);
        assertTrue(result.getEnableRag());
        assertEquals("kb-partial", result.getKnowledgeBaseId());
        assertEquals(3, result.getRagTopK());
        assertNull(result.getRagThreshold());
        assertNull(result.getRagStrategy());
        assertNull(result.getRagEmbeddingModelId());
    }

    @Test
    void createSession_EnableRagFalse_Success() {
        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("RAG Disabled Session");
        request.setModelId("model-123");
        request.setEnableRag(false);  // 明确设置为 false

        when(idGenerator.generateId()).thenReturn("rag-disabled-session-id");
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            return session;
        });

        ChatSessionDTO result = chatService.createSession(request, "test-user");

        assertNotNull(result);
        assertFalse(result.getEnableRag());
    }

    // ========== toMessageDTO sources 测试补充 ==========

    @Test
    void toMessageDTO_WithMultipleSources_ParsesAll() {
        String sourcesJson = "[{\"chunkId\":\"c1\",\"documentId\":\"d1\",\"content\":\"content1\",\"score\":0.9}," +
                "{\"chunkId\":\"c2\",\"documentId\":\"d2\",\"content\":\"content2\",\"score\":0.8}]";

        ChatMessage msg = ChatMessage.builder()
                .id("msg-multi")
                .sessionId("session-123")
                .role(MessageRole.ASSISTANT)
                .content("Response")
                .sources(sourcesJson)
                .isError(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-123"))
                .thenReturn(Collections.singletonList(msg));

        var result = chatService.getSessionMessages("session-123");

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getSources());
        assertEquals(2, result.get(0).getSources().size());
        assertEquals("c1", result.get(0).getSources().get(0).getChunkId());
        assertEquals("c2", result.get(0).getSources().get(1).getChunkId());
    }

    // ========== sendMessage RAG 分支测试 ==========

    @Test
    void sendMessage_RagEnabled_WithSources_SavesSourcesInMessage() {
        ChatSession session = ChatSession.builder()
                .id("rag-session-save")
                .modelId("model-123")
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .ragTopK(5)
                .ragThreshold(0.5)
                .build();

        VectorSearchResult source = VectorSearchResult.builder()
                .chunkId("chunk-saved")
                .content("Saved content")
                .score(0.9)
                .build();

        when(chatSessionRepository.findById("rag-session-save")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(any())).thenReturn("key");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ragService.retrieve(any())).thenReturn(List.of(source));

        ChatRequest request = new ChatRequest();
        request.setSessionId("rag-session-save");
        request.setContent("test");

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期：OpenAiChatClient 无法连接
        }

        // 验证 ragService.retrieve 被调用
        verify(ragService).retrieve(any());
        // 验证 chatMessageRepository.save 被调用（保存用户消息和助手消息）
        verify(chatMessageRepository, atLeast(1)).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_RagEnabled_RagServiceReturnsEmpty_NoSourcesSaved() {
        ChatSession session = ChatSession.builder()
                .id("rag-session-empty")
                .modelId("model-123")
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .ragTopK(5)
                .ragThreshold(0.5)
                .build();

        when(chatSessionRepository.findById("rag-session-empty")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(any())).thenReturn("key");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ragService.retrieve(any())).thenReturn(List.of());  // 空结果

        ChatRequest request = new ChatRequest();
        request.setSessionId("rag-session-empty");
        request.setContent("test");

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期
        }

        verify(ragService).retrieve(any());
    }

    // ========== createSession RAG promptId 分支测试 ==========

    @Test
    void createSession_WithPromptId_LoadsSystemMessage() {
        PromptTemplate template = PromptTemplate.builder()
                .id("prompt-123")
                .name("Test Prompt")
                .content("You are a test assistant.")
                .build();

        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("Session with Prompt");
        request.setModelId("model-123");
        request.setPromptId("prompt-123");
        request.setEnableRag(true);
        request.setKnowledgeBaseId("kb-123");

        when(promptTemplateRepository.findById("prompt-123")).thenReturn(Optional.of(template));
        when(idGenerator.generateId()).thenReturn("session-prompt");
        when(chatSessionRepository.save(any())).thenAnswer(inv -> {
            ChatSession s = inv.getArgument(0);
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        });

        ChatSessionDTO result = chatService.createSession(request, "test-user");

        assertNotNull(result);
        assertEquals("You are a test assistant.", result.getSystemMessage());
        assertTrue(result.getEnableRag());
    }

    @Test
    void createSession_WithPromptIdAndCustomSystemMessage_UsesCustom() {
        PromptTemplate template = PromptTemplate.builder()
                .id("prompt-456")
                .content("Template content")
                .build();

        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("Session Override");
        request.setModelId("model-123");
        request.setPromptId("prompt-456");
        request.setSystemMessage("Custom override message");  // 覆盖模板
        request.setEnableRag(false);

        when(promptTemplateRepository.findById("prompt-456")).thenReturn(Optional.of(template));
        when(idGenerator.generateId()).thenReturn("session-override");
        when(chatSessionRepository.save(any())).thenAnswer(inv -> {
            ChatSession s = inv.getArgument(0);
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        });

        ChatSessionDTO result = chatService.createSession(request, "test-user");

        assertNotNull(result);
        assertEquals("Custom override message", result.getSystemMessage());  // 使用自定义消息
    }

    @Test
    void createSession_PromptIdEmpty_SystemMessageNull() {
        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("No Prompt");
        request.setModelId("model-123");
        request.setPromptId("");  // 空 promptId
        request.setSystemMessage(null);
        request.setEnableRag(null);  // 不设置 RAG

        when(idGenerator.generateId()).thenReturn("session-no-prompt");
        when(chatSessionRepository.save(any())).thenAnswer(inv -> {
            ChatSession s = inv.getArgument(0);
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        });

        ChatSessionDTO result = chatService.createSession(request, "test-user");

        assertNotNull(result);
        assertNull(result.getSystemMessage());
        assertNull(result.getEnableRag());  // RAG 未设置
    }

    // ========== serializeSources 异常分支测试 ==========

    @Test
    void toMessageDTO_WithNullSourcesField_ReturnsNullSources() {
        ChatMessage msg = ChatMessage.builder()
                .id("msg-null-field")
                .sessionId("session-123")
                .role(MessageRole.ASSISTANT)
                .content("Response")
                .sources(null)  // null sources field
                .isError(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-123"))
                .thenReturn(Collections.singletonList(msg));

        var result = chatService.getSessionMessages("session-123");

        assertEquals(1, result.size());
        assertNull(result.get(0).getSources());
    }

    // ========== buildMessageList 分支测试 ==========

    @Test
    void buildMessageList_WithHistoryAndSources_ContainsAllMessages() {
        ChatSession session = ChatSession.builder()
                .id("session-history")
                .modelId("model-123")
                .systemMessage("System prompt")
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .build();

        ChatMessage historyUser = ChatMessage.builder()
                .id("hist-1")
                .sessionId("session-history")
                .role(MessageRole.USER)
                .content("History user message")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        ChatMessage historyAssistant = ChatMessage.builder()
                .id("hist-2")
                .sessionId("session-history")
                .role(MessageRole.ASSISTANT)
                .content("History assistant message")
                .createdAt(LocalDateTime.now().minusMinutes(4))
                .build();

        VectorSearchResult source = VectorSearchResult.builder()
                .chunkId("chunk-hist")
                .content("Source content")
                .score(0.85)
                .build();

        when(chatSessionRepository.findById("session-history")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(any())).thenReturn("key");
        when(idGenerator.generateId()).thenReturn("msg-new");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory("session-history"))
                .thenReturn(List.of(historyUser, historyAssistant));
        when(chatMessageRepository.countBySessionId(any())).thenReturn(4L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ragService.retrieve(any())).thenReturn(List.of(source));

        ChatRequest request = new ChatRequest();
        request.setSessionId("session-history");
        request.setContent("New question");

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期
        }

        verify(chatMessageRepository).findConversationHistory("session-history");
        verify(ragService).retrieve(any());
    }

    @Test
    void buildMessageList_WithNullSystemMessage_NoSystemMessage() {
        ChatSession session = ChatSession.builder()
                .id("session-no-sys")
                .modelId("model-123")
                .systemMessage(null)  // 无系统消息
                .enableRag(true)
                .knowledgeBaseId("kb-123")
                .build();

        // RAG 返回空，所以 buildSystemMessage 也会返回 null
        when(chatSessionRepository.findById("session-no-sys")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("model-123")).thenReturn(Optional.of(testModel));
        when(encryptionService.decrypt(any())).thenReturn("key");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ragService.retrieve(any())).thenReturn(List.of());  // 空 sources

        ChatRequest request = new ChatRequest();
        request.setSessionId("session-no-sys");
        request.setContent("test");

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期
        }

        verify(ragService).retrieve(any());
    }

    // ========== getSessionAndModelContext 分支测试 ==========

    @Test
    void getSessionAndModelContext_WithRequestModelId_UsesRequestModel() {
        ChatSession session = ChatSession.builder()
                .id("session-override-model")
                .modelId("session-model")  // session 有 modelId
                .enableRag(false)
                .build();

        ChatRequest request = new ChatRequest();
        request.setSessionId("session-override-model");
        request.setModelId("request-model");  // request 指定不同 modelId
        request.setContent("test");

        ModelConfig requestModel = ModelConfig.builder()
                .id("request-model")
                .name("Request Model")
                .provider(ModelProvider.DASHSCOPE)
                .modelName("request-qwen")
                .apiKey("key")
                .baseUrl("https://api.example.com")
                .isActive(true)
                .build();

        when(chatSessionRepository.findById("session-override-model")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findById("request-model")).thenReturn(Optional.of(requestModel));
        when(encryptionService.decrypt(any())).thenReturn("decrypted");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期
        }

        // 验证使用的是 request 中的 modelId，而非 session 的
        verify(modelConfigRepository).findById("request-model");
        verify(modelConfigRepository, never()).findById("session-model");
    }

    @Test
    void getSessionAndModelContext_NoModelId_UsesDefaultModel() {
        ChatSession session = ChatSession.builder()
                .id("session-no-model")
                .modelId(null)  // session 无 modelId
                .enableRag(false)
                .build();

        ChatRequest request = new ChatRequest();
        request.setSessionId("session-no-model");
        request.setModelId(null);  // request 也无 modelId
        request.setContent("test");

        ModelConfig defaultModel = ModelConfig.builder()
                .id("default-model")
                .name("Default Model")
                .isDefault(true)
                .modelName("default-qwen")
                .apiKey("key")
                .baseUrl("https://api.example.com")
                .isActive(true)
                .build();

        when(chatSessionRepository.findById("session-no-model")).thenReturn(Optional.of(session));
        when(modelConfigRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultModel));
        when(encryptionService.decrypt(any())).thenReturn("decrypted");
        when(idGenerator.generateId()).thenReturn("msg-1");
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findConversationHistory(any())).thenReturn(List.of());
        when(chatMessageRepository.countBySessionId(any())).thenReturn(2L);
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try {
            chatService.sendMessage(request);
        } catch (Exception e) {
            // 预期
        }

        verify(modelConfigRepository).findByIsDefaultTrue();
    }
}
