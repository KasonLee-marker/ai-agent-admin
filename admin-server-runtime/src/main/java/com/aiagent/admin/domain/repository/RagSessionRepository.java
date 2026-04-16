package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.RagSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RAG 会话 Repository
 */
@Repository
public interface RagSessionRepository extends JpaRepository<RagSession, String> {

    /**
     * 查询用户的 RAG 会话列表（按更新时间倒序）
     */
    Page<RagSession> findByCreatedByOrderByUpdatedAtDesc(String createdBy, Pageable pageable);

    /**
     * 查询用户在指定知识库下的 RAG 会话列表
     */
    Page<RagSession> findByCreatedByAndKnowledgeBaseIdOrderByUpdatedAtDesc(
            String createdBy, String knowledgeBaseId, Pageable pageable);

    /**
     * 查询最近活跃的会话
     */
    Optional<RagSession> findFirstByCreatedByOrderByUpdatedAtDesc(String createdBy);

    /**
     * 统计用户会话数量
     */
    long countByCreatedBy(String createdBy);
}