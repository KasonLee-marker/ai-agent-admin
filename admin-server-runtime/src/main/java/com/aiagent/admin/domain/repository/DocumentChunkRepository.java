package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.DocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(String documentId);

    Page<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(String documentId, Pageable pageable);

    void deleteByDocumentId(String documentId);

    long countByDocumentId(String documentId);

    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.documentId = :documentId ORDER BY dc.chunkIndex ASC")
    List<DocumentChunk> findAllByDocumentId(@Param("documentId") String documentId);
}