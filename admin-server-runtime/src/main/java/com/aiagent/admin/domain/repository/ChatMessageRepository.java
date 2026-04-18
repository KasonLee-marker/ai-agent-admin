package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天消息数据访问接口
 * <p>
 * 提供聊天消息实体的 CRUD 操作和自定义查询方法，支持按会话、角色等条件查询。
 * </p>
 *
 * @see ChatMessage
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    /**
     * 查询会话的所有消息（按创建时间升序）
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 查询会话的所有消息（按创建时间倒序）
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * 查询会话的对话历史（仅 USER 和 ASSISTANT 角色，按时间升序）
     * <p>
     * 用于构建 AI 对话的上下文历史。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 对话消息列表
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.role IN ('USER', 'ASSISTANT') " +
           "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findConversationHistory(@Param("sessionId") String sessionId);

    /**
     * 统计会话中的用户消息数量
     *
     * @param sessionId 会话 ID
     * @return 用户消息数量
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.role = 'USER'")
    long countUserMessages(@Param("sessionId") String sessionId);

    /**
     * 统计会话中的所有消息数量
     *
     * @param sessionId 会话 ID
     * @return 消息数量
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.sessionId = :sessionId")
    long countBySessionId(@Param("sessionId") String sessionId);

    /**
     * 删除会话的所有消息
     *
     * @param sessionId 会话 ID
     */
    void deleteBySessionId(String sessionId);

    /**
     * 查询会话中指定角色的消息（按时间倒序）
     *
     * @param sessionId 会话 ID
     * @param role      消息角色（USER、ASSISTANT、SYSTEM）
     * @return 消息列表
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.role = :role " +
           "ORDER BY cm.createdAt DESC")
    List<ChatMessage> findBySessionIdAndRoleOrderByCreatedAtDesc(@Param("sessionId") String sessionId,
                                                                  @Param("role") String role);
}
