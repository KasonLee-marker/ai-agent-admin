package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.domain.entity.Document.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    Page<Document> findByStatusOrderByCreatedAtDesc(DocumentStatus status, Pageable pageable);

    Page<Document> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

    Page<Document> findByStatusAndCreatedByOrderByCreatedAtDesc(DocumentStatus status, String createdBy, Pageable pageable);

    List<Document> findByStatus(DocumentStatus status);

    boolean existsByName(String name);
}