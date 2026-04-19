package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.ChatRequest;
import com.aiagent.admin.api.dto.ChatResponse;
import com.aiagent.admin.api.dto.ChatSessionDTO;
import jakarta.persistence.EntityNotFoundException;
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
     * 创建新的聊天会话
     * <p>
     * 初始化会话时设置标题、关联模型和提示词模板，以及系统消息。
     * 会话创建后消息计数为 0，状态为活跃。
     * </p>
     *
     * @param request   创建会话请求，包含标题、模型ID、提示词ID等
     * @param createdBy 会话创建者标识
     * @return 创建成功的会话 DTO
     */
    ChatSessionDTO createSession(ChatRequest.CreateSessionRequest request, String createdBy);

    /**
     * 更新会话信息
     * <p>
     * 支持更新标题、模型ID、提示词ID、系统消息和 RAG 配置。
     * 如果指定了新的 promptId 且未手动指定 systemMessage，则从模板加载内容。
     * </p>
     *
     * @param sessionId 会话唯一标识
     * @param request   更新请求，包含可更新的字段
     * @return 更新后的会话 DTO
     * @throws EntityNotFoundException 会话不存在时抛出
     */
    ChatSessionDTO updateSession(String sessionId, ChatRequest.UpdateSessionRequest request);

    /**
     * 根据会话 ID 获取会话详情
     *
     * @param sessionId 会话唯一标识
     * @return 会话 DTO
     * @throws EntityNotFoundException 会话不存在时抛出
     */
    ChatSessionDTO getSession(String sessionId);

    /**
     * 分页查询用户的聊天会话列表
     * <p>
     * 按更新时间倒序排列，返回最近的会话优先。
     * </p>
     *
     * @param createdBy 会话创建者标识
     * @param pageable  分页参数
     * @return 会话 DTO 列表
     */
    List<ChatSessionDTO> listSessions(String createdBy, Pageable pageable);

    /**
     * 根据关键词搜索用户的聊天会话
     * <p>
     * 支持在会话标题和消息内容中搜索匹配关键词。
     * </p>
     *
     * @param createdBy 会话创建者标识
     * @param keyword   搜索关键词
     * @param pageable  分页参数
     * @return 匹配的会话 DTO 列表
     */
    List<ChatSessionDTO> listSessionsByKeyword(String createdBy, String keyword, Pageable pageable);

    /**
     * 删除聊天会话及其所有消息
     * <p>
     * 同时删除会话记录和关联的所有消息记录。
     * </p>
     *
     * @param sessionId 会话唯一标识
     * @throws EntityNotFoundException 会话不存在时抛出
     */
    void deleteSession(String sessionId);

    /**
     * 发送消息并获取 AI 响应（同步模式）
     * <p>
     * 执行流程：
     * <ol>
     *   <li>获取会话和模型配置</li>
     *   <li>保存用户消息</li>
     *   <li>构建包含系统消息、历史消息和当前消息的 Prompt</li>
     *   <li>调用 AI 模型获取响应</li>
     *   <li>保存助手消息并更新会话统计</li>
     * </ol>
     * </p>
     *
     * @param request 消息请求，包含会话ID、消息内容、可选模型ID
     * @return 助手响应消息 DTO
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
