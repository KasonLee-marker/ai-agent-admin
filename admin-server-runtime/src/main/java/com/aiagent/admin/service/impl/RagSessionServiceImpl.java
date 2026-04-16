package com.aiagent.admin.service.impl;

import com.aiagent.admin.api.dto.RagMessageDTO;
import com.aiagent.admin.api.dto.RagSessionDTO;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.domain.entity.RagMessage;
import com.aiagent.admin.domain.entity.RagSession;
import com.aiagent.admin.domain.enums.MessageRole;
import com.aiagent.admin.domain.repository.KnowledgeBaseRepository;
import com.aiagent.admin.domain.repository.ModelConfigRepository;
import com.aiagent.admin.domain.repository.RagMessageRepository;
import com.aiagent.admin.domain.repository.RagSessionRepository;
import com.aiagent.admin.service.IdGenerator;
import com.aiagent.admin.service.RagSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 会话服务实现类
 * <p>
 * 管理 RAG 对话会话和消息的完整生命周期：
 * <ul>
 *   <li>会话创建、查询、删除</li>
 *   <li>消息保存（用户问题和 AI 回答）</li>
 *   <li>历史消息查询（用于构建多轮对话上下文）</li>
 * </ul>
 * </p>
 *
 * @see RagSessionService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagSessionServiceImpl implements RagSessionService {

    private final RagSessionRepository ragSessionRepository;
    private final RagMessageRepository ragMessageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    /**
     * 创建新的 RAG 会话
     *
     * @param knowledgeBaseId  知识库 ID（可选）
     * @param modelId          对话模型 ID（可选）
     * @param embeddingModelId Embedding 模型 ID（可选）
     * @param createdBy        创建者标识
     * @return 创建的会话 DTO
     */
    @Override
    @Transactional
    public RagSessionDTO createSession(String knowledgeBaseId, String modelId, String embeddingModelId, String createdBy) {
        RagSession session = RagSession.builder()
                .id(idGenerator.generateId())
                .knowledgeBaseId(knowledgeBaseId)
                .modelId(modelId)
                .embeddingModelId(embeddingModelId)
                .messageCount(0)
                .createdBy(createdBy)
                .build();

        RagSession saved = ragSessionRepository.save(session);
        log.info("Created RAG session: {}", saved.getId());

        return toSessionDTO(saved);
    }

    /**
     * 获取会话详情
     *
     * @param sessionId 会话 ID
     * @return 会话 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public RagSessionDTO getSession(String sessionId) {
        RagSession session = ragSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("RAG session not found: " + sessionId));
        return toSessionDTO(session);
    }

    /**
     * 查询用户的 RAG 会话列表
     *
     * @param createdBy 创建者标识
     * @param pageable  分页参数
     * @return 会话列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<RagSessionDTO> listSessions(String createdBy, Pageable pageable) {
        Page<RagSession> page = ragSessionRepository.findByCreatedByOrderByUpdatedAtDesc(createdBy, pageable);
        return page.getContent().stream()
                .map(this::toSessionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 删除会话及其消息
     *
     * @param sessionId 会话 ID
     */
    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        if (!ragSessionRepository.existsById(sessionId)) {
            throw new EntityNotFoundException("RAG session not found: " + sessionId);
        }
        ragMessageRepository.deleteBySessionId(sessionId);
        ragSessionRepository.deleteById(sessionId);
        log.info("Deleted RAG session: {}", sessionId);
    }

    /**
     * 保存用户消息
     *
     * @param sessionId 会话 ID
     * @param content   用户问题内容
     * @return 消息 DTO
     */
    @Override
    @Transactional
    public RagMessageDTO saveUserMessage(String sessionId, String content) {
        RagSession session = ragSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("RAG session not found: " + sessionId));

        RagMessage message = RagMessage.builder()
                .id(idGenerator.generateId())
                .sessionId(sessionId)
                .role(MessageRole.USER)
                .content(content)
                .isError(false)
                .build();

        RagMessage saved = ragMessageRepository.save(message);

        // 更新会话消息计数
        session.setMessageCount((int) ragMessageRepository.countBySessionId(sessionId));
        session.setUpdatedAt(LocalDateTime.now());

        // 如果是第一条消息且没有标题，设置标题
        if (session.getTitle() == null || session.getTitle().isEmpty()) {
            String title = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            session.setTitle(title);
        }

        ragSessionRepository.save(session);

        return toMessageDTO(saved);
    }

    /**
     * 保存助手消息
     *
     * @param sessionId 会话 ID
     * @param content   AI 回答内容
     * @param sources   检索来源列表
     * @param modelName 模型名称
     * @param latencyMs 响应延迟（毫秒）
     * @return 消息 DTO
     */
    @Override
    @Transactional
    public RagMessageDTO saveAssistantMessage(String sessionId, String content, List<VectorSearchResult> sources,
                                              String modelName, Long latencyMs) {
        RagSession session = ragSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("RAG session not found: " + sessionId));

        // 将 sources 序列化为 JSON
        String sourcesJson = null;
        if (sources != null && !sources.isEmpty()) {
            try {
                sourcesJson = objectMapper.writeValueAsString(sources);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize sources: {}", e.getMessage());
            }
        }

        RagMessage message = RagMessage.builder()
                .id(idGenerator.generateId())
                .sessionId(sessionId)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .sources(sourcesJson)
                .modelName(modelName)
                .latencyMs(latencyMs)
                .isError(content == null || content.isEmpty())
                .errorMessage(content == null || content.isEmpty() ? "Empty response" : null)
                .build();

        RagMessage saved = ragMessageRepository.save(message);

        // 更新会话消息计数
        session.setMessageCount((int) ragMessageRepository.countBySessionId(sessionId));
        session.setUpdatedAt(LocalDateTime.now());
        ragSessionRepository.save(session);

        return toMessageDTO(saved);
    }

    /**
     * 获取会话的对话历史
     *
     * @param sessionId 会话 ID
     * @param limit     最大消息数量（可选，null 表示全部）
     * @return 消息列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<RagMessageDTO> getHistory(String sessionId, Integer limit) {
        List<RagMessage> messages;
        if (limit != null && limit > 0) {
            messages = ragMessageRepository.findRecentMessages(sessionId, limit);
            // 反转顺序，使消息按时间升序排列
            messages = messages.stream()
                    .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                    .collect(Collectors.toList());
        } else {
            messages = ragMessageRepository.findConversationHistory(sessionId);
        }
        return messages.stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取会话的所有消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<RagMessageDTO> getSessionMessages(String sessionId) {
        List<RagMessage> messages = ragMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新会话标题
     *
     * @param sessionId 会话 ID
     * @param title     新标题
     */
    @Override
    @Transactional
    public void updateSessionTitle(String sessionId, String title) {
        RagSession session = ragSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("RAG session not found: " + sessionId));
        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());
        ragSessionRepository.save(session);
    }

    /**
     * 将会话实体转换为 DTO
     */
    private RagSessionDTO toSessionDTO(RagSession session) {
        RagSessionDTO dto = RagSessionDTO.builder()
                .id(session.getId())
                .title(session.getTitle())
                .knowledgeBaseId(session.getKnowledgeBaseId())
                .modelId(session.getModelId())
                .embeddingModelId(session.getEmbeddingModelId())
                .messageCount(session.getMessageCount())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .createdBy(session.getCreatedBy())
                .build();

        // 补充知识库名称
        if (session.getKnowledgeBaseId() != null) {
            knowledgeBaseRepository.findById(session.getKnowledgeBaseId())
                    .ifPresent(kb -> dto.setKnowledgeBaseName(kb.getName()));
        }

        // 补充模型名称
        if (session.getModelId() != null) {
            modelConfigRepository.findById(session.getModelId())
                    .ifPresent(model -> dto.setModelName(model.getName()));
        }

        if (session.getEmbeddingModelId() != null) {
            modelConfigRepository.findById(session.getEmbeddingModelId())
                    .ifPresent(model -> dto.setEmbeddingModelName(model.getName()));
        }

        return dto;
    }

    /**
     * 将消息实体转换为 DTO
     */
    private RagMessageDTO toMessageDTO(RagMessage message) {
        RagMessageDTO dto = RagMessageDTO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .modelName(message.getModelName())
                .latencyMs(message.getLatencyMs())
                .isError(message.getIsError())
                .errorMessage(message.getErrorMessage())
                .createdAt(message.getCreatedAt())
                .build();

        // 解析 sources JSON
        if (message.getSources() != null) {
            try {
                List<VectorSearchResult> sources = objectMapper.readValue(
                        message.getSources(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, VectorSearchResult.class));
                dto.setSources(sources);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize sources: {}", e.getMessage());
            }
        }

        return dto;
    }
}