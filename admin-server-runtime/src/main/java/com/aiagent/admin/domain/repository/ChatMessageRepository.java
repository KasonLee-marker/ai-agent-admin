package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ChatMessage> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.role IN ('USER', 'ASSISTANT') " +
           "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findConversationHistory(@Param("sessionId") String sessionId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.role = 'USER'")
    long countUserMessages(@Param("sessionId") String sessionId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.sessionId = :sessionId")
    long countBySessionId(@Param("sessionId") String sessionId);

    void deleteBySessionId(String sessionId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.role = :role " +
           "ORDER BY cm.createdAt DESC")
    List<ChatMessage> findBySessionIdAndRoleOrderByCreatedAtDesc(@Param("sessionId") String sessionId,
                                                                  @Param("role") String role);
}
