package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.RagMessage;
import com.aiagent.admin.domain.enums.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * RAG 消息 Repository
 */
@Repository
public interface RagMessageRepository extends JpaRepository<RagMessage, String> {

    /**
     * 查询会话的所有消息（按时间升序）
     */
    List<RagMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 查询会话的对话历史（仅 USER 和 ASSISTANT 角色）
     */
    @Query("SELECT rm FROM RagMessage rm WHERE rm.sessionId = :sessionId AND rm.role IN ('USER', 'ASSISTANT') " +
            "ORDER BY rm.createdAt ASC")
    List<RagMessage> findConversationHistory(@Param("sessionId") String sessionId);

    /**
     * 查询会话的最近 N 条消息
     */
    @Query("SELECT rm FROM RagMessage rm WHERE rm.sessionId = :sessionId " +
            "ORDER BY rm.createdAt DESC LIMIT :limit")
    List<RagMessage> findRecentMessages(@Param("sessionId") String sessionId, @Param("limit") int limit);

    /**
     * 统计会话消息数量
     */
    long countBySessionId(String sessionId);

    /**
     * 删除会话的所有消息
     */
    void deleteBySessionId(String sessionId);

    /**
     * 查询会话中指定角色的消息
     */
    @Query("SELECT rm FROM RagMessage rm WHERE rm.sessionId = :sessionId AND rm.role = :role " +
            "ORDER BY rm.createdAt DESC")
    List<RagMessage> findBySessionIdAndRoleOrderByCreatedAtDesc(
            @Param("sessionId") String sessionId, @Param("role") MessageRole role);
}