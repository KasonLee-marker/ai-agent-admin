package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.ChatRequest;
import com.aiagent.admin.api.dto.ChatResponse;
import com.aiagent.admin.api.dto.ChatSessionDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天服务接口
 * <p>
 * 提供聊天会话管理和消息处理的核心功能：
 * <ul>
 *   <li>会话创建、查询、更新、删除</li>
 *   <li>消息发送（同步和流式）</li>
 *   <li>AI 模型调用与响应处理</li>
 *   <li>对话历史记录管理</li>
 * </ul>
 * </p>
 *
 * @see ChatRequest
 * @see ChatResponse
 * @see ChatSessionDTO
 */
public interface ChatService {

    /**
     * 创建新的对话会话
     *
     * @param request   创建会话请求，包含会话标题等信息
     * @param createdBy 创建者标识
     * @return 创建成功的会话 DTO
     */
    ChatSessionDTO createSession(ChatRequest.CreateSessionRequest request, String createdBy);

    /**
     * 更新会话信息
     *
     * @param sessionId 会话ID
     * @param request   更新请求
     * @return 更新后的会话 DTO
     */
    ChatSessionDTO updateSession(String sessionId, ChatRequest.UpdateSessionRequest request);

    /**
     * 根据会话ID获取会话详情
     *
     * @param sessionId 会话ID
     * @return 会话 DTO
     */
    ChatSessionDTO getSession(String sessionId);

    /**
     * 分页查询会话列表
     *
     * @param createdBy 创建者标识
     * @param pageable  分页参数
     * @return 会话 DTO 列表
     */
    List<ChatSessionDTO> listSessions(String createdBy, Pageable pageable);

    /**
     * 根据关键字分页查询会话列表
     *
     * @param createdBy 创建者标识
     * @param keyword   搜索关键字（匹配会话标题等）
     * @param pageable  分页参数
     * @return 会话 DTO 列表
     */
    List<ChatSessionDTO> listSessionsByKeyword(String createdBy, String keyword, Pageable pageable);

    /**
     * 删除指定会话及其关联消息
     *
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId);

    /**
     * 发送消息并获取同步响应
     *
     * @param request 消息请求，包含会话ID、用户消息内容等
     * @return AI 助手的响应
     */
    ChatResponse sendMessage(ChatRequest request);

    /**
     * 流式发送消息（SSE 流式响应）
     *
     * @param request 消息请求，包含会话ID、用户消息内容等
     * @return 流式响应，逐步返回 AI 生成的内容片段
     */
    Flux<String> sendMessageStream(ChatRequest request);

    /**
     * 获取指定会话的所有消息记录
     *
     * @param sessionId 会话ID
     * @return 消息响应列表，按时间顺序排列
     */
    List<ChatResponse> getSessionMessages(String sessionId);

    /**
     * 获取指定会话的对话历史（用于构建上下文）
     *
     * @param sessionId 会话ID
     * @return 对话历史列表，包含用户消息和AI回复
     */
    List<ChatResponse> getConversationHistory(String sessionId);
}
