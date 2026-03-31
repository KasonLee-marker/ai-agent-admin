package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.ChatRequest;
import com.aiagent.admin.api.dto.ChatResponse;
import com.aiagent.admin.api.dto.ChatSessionDTO;
import com.aiagent.admin.domain.enums.MessageRole;
import com.aiagent.admin.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@ContextConfiguration(classes = ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    private ChatSessionDTO testSession;
    private ChatResponse testMessage;

    @BeforeEach
    void setUp() {
        testSession = ChatSessionDTO.builder()
                .id("session-123")
                .title("Test Session")
                .modelId("model-123")
                .messageCount(2)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy("test-user")
                .build();

        testMessage = ChatResponse.builder()
                .id("msg-1")
                .sessionId("session-123")
                .role(MessageRole.ASSISTANT)
                .content("Hello! How can I help you?")
                .modelName("qwen-turbo")
                .latencyMs(150L)
                .isError(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createSession_Success() throws Exception {
        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        request.setTitle("New Session");
        request.setModelId("model-123");
        request.setSystemMessage("You are a helpful assistant");

        when(chatService.createSession(any(ChatRequest.CreateSessionRequest.class), anyString()))
                .thenReturn(testSession);

        mockMvc.perform(post("/api/v1/chat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("session-123"))
                .andExpect(jsonPath("$.data.title").value("Test Session"));

        verify(chatService).createSession(any(ChatRequest.CreateSessionRequest.class), anyString());
    }

    @Test
    void createSession_ValidationError() throws Exception {
        ChatRequest.CreateSessionRequest request = new ChatRequest.CreateSessionRequest();
        // Title is empty, should fail validation

        mockMvc.perform(post("/api/v1/chat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listSessions_Success() throws Exception {
        when(chatService.listSessions(anyString(), any()))
                .thenReturn(Collections.singletonList(testSession));

        mockMvc.perform(get("/api/v1/chat/sessions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value("session-123"));
    }

    @Test
    void listSessions_WithKeyword() throws Exception {
        when(chatService.listSessionsByKeyword(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(testSession));

        mockMvc.perform(get("/api/v1/chat/sessions")
                        .param("keyword", "Test")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getSession_Success() throws Exception {
        when(chatService.getSession("session-123")).thenReturn(testSession);

        mockMvc.perform(get("/api/v1/chat/sessions/session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("session-123"))
                .andExpect(jsonPath("$.data.title").value("Test Session"));
    }

    @Test
    void deleteSession_Success() throws Exception {
        doNothing().when(chatService).deleteSession("session-123");

        mockMvc.perform(delete("/api/v1/chat/sessions/session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(chatService).deleteSession("session-123");
    }

    @Test
    void sendMessage_Success() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-123");
        request.setContent("Hello!");

        when(chatService.sendMessage(any(ChatRequest.class))).thenReturn(testMessage);

        mockMvc.perform(post("/api/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("Hello! How can I help you?"))
                .andExpect(jsonPath("$.data.role").value("ASSISTANT"));
    }

    @Test
    void sendMessage_ValidationError() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-123");
        // Content is empty, should fail validation

        mockMvc.perform(post("/api/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSessionMessages_Success() throws Exception {
        when(chatService.getSessionMessages("session-123"))
                .thenReturn(Arrays.asList(
                        ChatResponse.builder()
                                .id("msg-1")
                                .sessionId("session-123")
                                .role(MessageRole.USER)
                                .content("Hello")
                                .build(),
                        ChatResponse.builder()
                                .id("msg-2")
                                .sessionId("session-123")
                                .role(MessageRole.ASSISTANT)
                                .content("Hi there!")
                                .build()
                ));

        mockMvc.perform(get("/api/v1/chat/sessions/session-123/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").value("session-123"))
                .andExpect(jsonPath("$.data.messages").isArray())
                .andExpect(jsonPath("$.data.messages.length()").value(2));
    }

    @Test
    void getConversationHistory_Success() throws Exception {
        when(chatService.getConversationHistory("session-123"))
                .thenReturn(Collections.singletonList(testMessage));

        mockMvc.perform(get("/api/v1/chat/sessions/session-123/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.messages[0].role").value("ASSISTANT"));
    }
}
