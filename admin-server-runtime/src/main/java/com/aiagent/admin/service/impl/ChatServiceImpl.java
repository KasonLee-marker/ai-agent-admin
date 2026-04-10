package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.ChatRequest;
import com.aiagent.admin.api.dto.ChatResponse;
import com.aiagent.admin.api.dto.ChatSessionDTO;
import com.aiagent.admin.domain.entity.ChatMessage;
import com.aiagent.admin.domain.entity.ChatSession;
import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.MessageRole;
import com.aiagent.admin.domain.repository.ChatMessageRepository;
import com.aiagent.admin.domain.repository.ChatSessionRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.service.ChatService;
import com.aiagent.admin.service.EncryptionService;
import com.aiagent.admin.service.IdGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final EncryptionService encryptionService;
    private final IdGenerator idGenerator;

    @Override
    @Transactional
    public ChatSessionDTO createSession(ChatRequest.CreateSessionRequest request, String createdBy) {
        ChatSession session = ChatSession.builder()
                .id(idGenerator.generateId())
                .title(request.getTitle())
                .modelId(request.getModelId())
                .promptId(request.getPromptId())
                .systemMessage(request.getSystemMessage())
                .messageCount(0)
                .isActive(true)
                .createdBy(createdBy)
                .build();

        ChatSession saved = chatSessionRepository.save(session);
        return toSessionDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSessionDTO getSession(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Chat session not found with id: " + sessionId));
        return toSessionDTO(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionDTO> listSessions(String createdBy, Pageable pageable) {
        Page<ChatSession> page = chatSessionRepository.findByCreatedByOrderByUpdatedAtDesc(createdBy, pageable);
        return page.getContent().stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionDTO> listSessionsByKeyword(String createdBy, String keyword, Pageable pageable) {
        Page<ChatSession> page = chatSessionRepository.findByCreatedByAndKeyword(createdBy, keyword, pageable);
        return page.getContent().stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new EntityNotFoundException("Chat session not found with id: " + sessionId);
        }
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        ChatSession session = chatSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new EntityNotFoundException("Chat session not found with id: " + request.getSessionId()));

        final String modelId = request.getModelId() != null ? request.getModelId() : session.getModelId();
        final ModelConfig modelConfig;
        if (modelId == null) {
            modelConfig = modelConfigRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new IllegalStateException("No default model configured"));
        } else {
            modelConfig = modelConfigRepository.findById(modelId)
                    .orElseThrow(() -> new EntityNotFoundException("Model not found with id: " + modelId));
        }

        ChatMessage userMessage = ChatMessage.builder()
                .id(idGenerator.generateId())
                .sessionId(session.getId())
                .role(MessageRole.USER)
                .content(request.getContent())
                .build();
        chatMessageRepository.save(userMessage);

        ChatMessage assistantMessage;
        try {
            String aiResponse = callAiModel(session, request.getContent(), modelConfig);
            long latency = System.currentTimeMillis() - startTime;

            assistantMessage = ChatMessage.builder()
                    .id(idGenerator.generateId())
                    .sessionId(session.getId())
                    .role(MessageRole.ASSISTANT)
                    .content(aiResponse)
                    .modelName(modelConfig.getModelName())
                    .latencyMs(latency)
                    .isError(false)
                    .build();
        } catch (Exception e) {
            log.error("Error calling AI model: {}", e.getMessage(), e);

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Unknown error";
            }

            if (errorMsg.contains("404")) {
                errorMsg = "模型可能不存在或 API endpoint 不正确: " + modelConfig.getModelName();
            }

            assistantMessage = ChatMessage.builder()
                    .id(idGenerator.generateId())
                    .sessionId(session.getId())
                    .role(MessageRole.ASSISTANT)
                    .content("")
                    .modelName(modelConfig.getModelName())
                    .isError(true)
                    .errorMessage(errorMsg)
                    .build();
        }

        chatMessageRepository.save(assistantMessage);

        session.setMessageCount((int) chatMessageRepository.countBySessionId(session.getId()));
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        return toMessageDTO(assistantMessage);
    }

    private String callAiModel(ChatSession session, String userContent, ModelConfig modelConfig) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        if (session.getSystemMessage() != null && !session.getSystemMessage().isEmpty()) {
            messages.add(new SystemMessage(session.getSystemMessage()));
        }

        List<ChatMessage> history = chatMessageRepository.findConversationHistory(session.getId());
        for (ChatMessage msg : history) {
            if (msg.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        messages.add(new UserMessage(userContent));

        Prompt prompt = new Prompt(messages);

        try {
            log.info("Calling AI model: name={}, modelName={}, baseUrl={}",
                    modelConfig.getName(), modelConfig.getModelName(), modelConfig.getBaseUrl());

            OpenAiChatClient chatClient = buildChatClient(modelConfig);
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("AI model call failed for model {} ({}): {}",
                    modelConfig.getName(), modelConfig.getModelName(), e.getMessage(), e);

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Unknown error";
            }

            throw new RuntimeException("模型调用失败: " + errorMsg +
                    " [模型: " + modelConfig.getModelName() +
                    ", URL: " + modelConfig.getBaseUrl() + "]");
        }
    }

    private OpenAiChatClient buildChatClient(ModelConfig config) {
        String apiKey = encryptionService.decrypt(config.getApiKey());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() :
            config.getProvider().getDefaultBaseUrl();

        log.debug("Building chat client: model={}, baseUrl={}", config.getModelName(), baseUrl);

        OpenAiApi api = new OpenAiApi(baseUrl, apiKey);

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .withModel(config.getModelName());

        if (config.getTemperature() != null) {
            optionsBuilder.withTemperature(config.getTemperature().floatValue());
        }
        if (config.getMaxTokens() != null) {
            optionsBuilder.withMaxTokens(config.getMaxTokens());
        }
        if (config.getTopP() != null) {
            optionsBuilder.withTopP(config.getTopP().floatValue());
        }

        return new OpenAiChatClient(api, optionsBuilder.build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getSessionMessages(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getConversationHistory(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findConversationHistory(sessionId);
        return messages.stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    private ChatSessionDTO toSessionDTO(ChatSession session) {
        return ChatSessionDTO.builder()
                .id(session.getId())
                .title(session.getTitle())
                .modelId(session.getModelId())
                .promptId(session.getPromptId())
                .systemMessage(session.getSystemMessage())
                .messageCount(session.getMessageCount())
                .isActive(session.getIsActive())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .createdBy(session.getCreatedBy())
                .build();
    }

    private ChatResponse toMessageDTO(ChatMessage message) {
        return ChatResponse.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .modelName(message.getModelName())
                .tokenCount(message.getTokenCount())
                .latencyMs(message.getLatencyMs())
                .isError(message.getIsError())
                .errorMessage(message.getErrorMessage())
                .createdAt(message.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public Flux<String> sendMessageStream(ChatRequest request) {
        ChatSession session = chatSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new EntityNotFoundException("Chat session not found with id: " + request.getSessionId()));

        final String modelId = request.getModelId() != null ? request.getModelId() : session.getModelId();
        final ModelConfig modelConfig;
        if (modelId == null) {
            modelConfig = modelConfigRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new IllegalStateException("No default model configured"));
        } else {
            modelConfig = modelConfigRepository.findById(modelId)
                    .orElseThrow(() -> new EntityNotFoundException("Model not found with id: " + modelId));
        }

        // 保存用户消息
        ChatMessage userMessage = ChatMessage.builder()
                .id(idGenerator.generateId())
                .sessionId(session.getId())
                .role(MessageRole.USER)
                .content(request.getContent())
                .build();
        chatMessageRepository.save(userMessage);

        // 构建消息历史
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        if (session.getSystemMessage() != null && !session.getSystemMessage().isEmpty()) {
            messages.add(new SystemMessage(session.getSystemMessage()));
        }
        List<ChatMessage> history = chatMessageRepository.findConversationHistory(session.getId());
        for (ChatMessage msg : history) {
            if (msg.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        messages.add(new UserMessage(request.getContent()));

        Prompt prompt = new Prompt(messages);
        OpenAiChatClient chatClient = buildChatClient(modelConfig);

        StringBuilder fullResponse = new StringBuilder();

        return chatClient.stream(prompt)
                .map(response -> {
                    String content = response.getResult().getOutput().getContent();
                    if (content != null) {
                        fullResponse.append(content);
                        return content;
                    }
                    return "";
                })
                .doOnComplete(() -> {
                    // 流式完成后保存完整响应
                    saveAssistantMessage(session, modelConfig, fullResponse.toString());
                })
                .doOnError(e -> {
                    log.error("Stream error: {}", e.getMessage());
                    saveAssistantMessage(session, modelConfig, "");
                });
    }

    private void saveAssistantMessage(ChatSession session, ModelConfig modelConfig, String content) {
        ChatMessage assistantMessage = ChatMessage.builder()
                .id(idGenerator.generateId())
                .sessionId(session.getId())
                .role(MessageRole.ASSISTANT)
                .content(content)
                .modelName(modelConfig.getModelName())
                .isError(content.isEmpty())
                .errorMessage(content.isEmpty() ? "Stream failed" : null)
                .build();
        chatMessageRepository.save(assistantMessage);

        session.setMessageCount((int) chatMessageRepository.countBySessionId(session.getId()));
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
    }
}