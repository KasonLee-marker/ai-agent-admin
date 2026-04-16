package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库数据访问接口
 * <p>
 * 提供知识库实体的 CRUD 操作和自定义查询方法。
 * </p>
 *
 * @see KnowledgeBase
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {

    /**
     * 按创建人分页查询知识库
     *
     * @param createdBy 创建人
     * @param pageable  分页参数
     * @return 知识库分页列表
     */
    Page<KnowledgeBase> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * 按创建人查询所有知识库
     *
     * @param createdBy 创建人
     * @return 知识库列表
     */
    List<KnowledgeBase> findByCreatedBy(String createdBy);

    /**
     * 查询所有知识库（不分页）
     *
     * @return 所有知识库列表
     */
    List<KnowledgeBase> findAllByOrderByCreatedAtDesc();

    /**
     * 检查知识库名称是否已存在
     *
     * @param name      知识库名称
     * @param createdBy 创建人
     * @return 是否存在
     */
    boolean existsByNameAndCreatedBy(String name, String createdBy);

    /**
     * 根据默认 Embedding 模型查询知识库
     *
     * @param embeddingModelId Embedding 模型 ID
     * @return 使用该模型的知识库列表
     */
    List<KnowledgeBase> findByDefaultEmbeddingModelId(String embeddingModelId);
}