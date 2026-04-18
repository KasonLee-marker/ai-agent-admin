package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.Document;
import com.aiagent.admin.domain.entity.Document.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档数据访问接口
 * <p>
 * 提供文档实体的 CRUD 操作和自定义查询方法，支持按状态、创建人、知识库等条件查询。
 * </p>
 *
 * @see Document
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    /**
     * 分页查询指定状态的文档（按创建时间倒序）
     *
     * @param status   文档状态
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    Page<Document> findByStatusOrderByCreatedAtDesc(DocumentStatus status, Pageable pageable);

    /**
     * 分页查询用户创建的文档（按创建时间倒序）
     *
     * @param createdBy 创建人
     * @param pageable  分页参数
     * @return 文档分页列表
     */
    Page<Document> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

    /**
     * 分页查询指定状态和创建人的文档（按创建时间倒序）
     *
     * @param status    文档状态
     * @param createdBy 创建人
     * @param pageable  分页参数
     * @return 文档分页列表
     */
    Page<Document> findByStatusAndCreatedByOrderByCreatedAtDesc(DocumentStatus status, String createdBy, Pageable pageable);

    /**
     * 查询指定状态的所有文档
     *
     * @param status 文档状态
     * @return 文档列表
     */
    List<Document> findByStatus(DocumentStatus status);

    /**
     * 检查文档名称是否已存在
     *
     * @param name 文档名称
     * @return 是否存在
     */
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