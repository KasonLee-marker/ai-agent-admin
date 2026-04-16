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

    // ========== 知识库关联查询 ==========

    /**
     * 查询知识库下的所有文档
     *
     * @param knowledgeBaseId 知识库 ID
     * @return 文档列表
     */
    List<Document> findByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * 统计知识库下的文档数量
     *
     * @param knowledgeBaseId 知识库 ID
     * @return 文档数量
     */
    long countByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * 检查知识库下是否有文档
     *
     * @param knowledgeBaseId 知识库 ID
     * @return 是否存在文档
     */
    boolean existsByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * 查询知识库下指定状态的文档
     *
     * @param knowledgeBaseId 知识库 ID
     * @param status          文档状态
     * @return 文档列表
     */
    List<Document> findByKnowledgeBaseIdAndStatus(String knowledgeBaseId, DocumentStatus status);
}