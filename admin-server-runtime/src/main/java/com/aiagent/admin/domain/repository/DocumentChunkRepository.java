package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.DocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档分块数据访问接口
 * <p>
 * 提供文档分块实体的 CRUD 操作和自定义查询方法，支持按文档 ID 查询分块列表，
 * 以及批量查询多个文档的分块（用于知识库重索引）。
 * </p>
 *
 * @see DocumentChunk
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {

    /**
     * 查询文档的所有分块（按分块索引升序）
     *
     * @param documentId 文档 ID
     * @return 分块列表
     */
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(String documentId);

    /**
     * 分页查询文档的分块（按分块索引升序）
     *
     * @param documentId 文档 ID
     * @param pageable   分页参数
     * @return 分块分页列表
     */
    Page<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(String documentId, Pageable pageable);

    /**
     * 删除文档的所有分块
     *
     * @param documentId 文档 ID
     */
    void deleteByDocumentId(String documentId);

    /**
     * 统计文档的分块数量
     *
     * @param documentId 文档 ID
     * @return 分块数量
     */
    long countByDocumentId(String documentId);

    /**
     * 查询文档的所有分块（按分块索引升序）
     * <p>
     * 与 findByDocumentIdOrderByChunkIndexAsc 功能相同，提供明确的 JPQL 查询。
     * </p>
     *
     * @param documentId 文档 ID
     * @return 分块列表
     */
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