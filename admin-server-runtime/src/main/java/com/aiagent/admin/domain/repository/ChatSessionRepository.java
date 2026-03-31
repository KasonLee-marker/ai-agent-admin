package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findByCreatedByOrderByUpdatedAtDesc(String createdBy);

    Page<ChatSession> findByCreatedByOrderByUpdatedAtDesc(String createdBy, Pageable pageable);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.createdBy = :createdBy AND " +
           "(:keyword IS NULL OR LOWER(cs.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY cs.updatedAt DESC")
    Page<ChatSession> findByCreatedByAndKeyword(@Param("createdBy") String createdBy,
                                                @Param("keyword") String keyword,
                                                Pageable pageable);

    Optional<ChatSession> findByIdAndCreatedBy(String id, String createdBy);

    boolean existsByIdAndCreatedBy(String id, String createdBy);

    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.createdBy = :createdBy AND cs.isActive = true")
    long countActiveByCreatedBy(@Param("createdBy") String createdBy);
}
