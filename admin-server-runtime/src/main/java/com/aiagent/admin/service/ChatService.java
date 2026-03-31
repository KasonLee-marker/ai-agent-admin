package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.ChatRequest;
import com.aiagent.admin.api.dto.ChatResponse;
import com.aiagent.admin.api.dto.ChatSessionDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {

    ChatSessionDTO createSession(ChatRequest.CreateSessionRequest request, String createdBy);

    ChatSessionDTO getSession(String sessionId);

    List<ChatSessionDTO> listSessions(String createdBy, Pageable pageable);

    List<ChatSessionDTO> listSessionsByKeyword(String createdBy, String keyword, Pageable pageable);

    void deleteSession(String sessionId);

    ChatResponse sendMessage(ChatRequest request);

    List<ChatResponse> getSessionMessages(String sessionId);

    List<ChatResponse> getConversationHistory(String sessionId);
}
