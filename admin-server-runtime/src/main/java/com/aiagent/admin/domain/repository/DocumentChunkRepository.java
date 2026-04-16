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

    /**
     * 批量查询多个文档的分块（按文档 ID 和分块索引排序）
     * <p>
     * 用于知识库重索引时获取所有分块。
     * </p>
     *
     * @param documentIds 文档 ID 列表
     * @return 分块列表
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.documentId IN :documentIds ORDER BY dc.documentId ASC, dc.chunkIndex ASC")
    List<DocumentChunk> findByDocumentIdInOrderByDocumentIdAscChunkIndexAsc(@Param("documentIds") List<String> documentIds);
}