package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.RagMessageDTO;
import com.aiagent.admin.api.dto.RagSessionDTO;
import com.aiagent.admin.api.dto.VectorSearchResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * RAG 会话服务接口
 * <p>
 * 管理 RAG 对话会话和消息：
 * <ul>
 *   <li>会话创建、查询、删除</li>
 *   <li>消息保存（用户问题和 AI 回答）</li>
 *   <li>历史消息查询</li>
 * </ul>
 * </p>
 *
 * @see RagService
 */
public interface RagSessionService {

    /**
     * 创建新的 RAG 会话
     *
     * @param knowledgeBaseId  知识库 ID（可选）
     * @param modelId          对话模型 ID（可选）
     * @param embeddingModelId Embedding 模型 ID（可选）
     * @param createdBy        创建者标识
     * @return 创建的会话 DTO
     */
    RagSessionDTO createSession(String knowledgeBaseId, String modelId, String embeddingModelId, String createdBy);

    /**
     * 获取会话详情
     *
     * @param sessionId 会话 ID
     * @return 会话 DTO
     */
    RagSessionDTO getSession(String sessionId);

    /**
     * 查询用户的 RAG 会话列表
     *
     * @param createdBy 创建者标识
     * @param pageable  分页参数
     * @return 会话列表
     */
    List<RagSessionDTO> listSessions(String createdBy, Pageable pageable);

    /**
     * 删除会话及其消息
     *
     * @param sessionId 会话 ID
     */
    void deleteSession(String sessionId);

    /**
     * 保存用户消息
     *
     * @param sessionId 会话 ID
     * @param content   用户问题内容
     * @return 消息 DTO
     */
    RagMessageDTO saveUserMessage(String sessionId, String content);

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
    RagMessageDTO saveAssistantMessage(String sessionId, String content, List<VectorSearchResult> sources,
                                       String modelName, Long latencyMs);

    /**
     * 获取会话的对话历史
     *
     * @param sessionId 会话 ID
     * @param limit     最大消息数量（可选，null 表示全部）
     * @return 消息列表
     */
    List<RagMessageDTO> getHistory(String sessionId, Integer limit);

    /**
     * 获取会话的所有消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<RagMessageDTO> getSessionMessages(String sessionId);

    /**
     * 更新会话标题
     * <p>
     * 通常在首条用户消息后设置标题。
     * </p>
     *
     * @param sessionId 会话 ID
     * @param title     新标题
     */
    void updateSessionTitle(String sessionId, String title);
}