package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.PromptTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Prompt 模板数据访问接口
 * <p>
 * 提供 Prompt 模板实体的 CRUD 操作和自定义查询方法，支持按分类、标签、关键词等条件查询。
 * </p>
 *
 * @see PromptTemplate
 */
@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, String> {

    /**
     * 按条件筛选 Prompt 模板
     * <p>
     * 支持分类、标签（模糊匹配）、关键词（匹配名称或描述）筛选。
     * </p>
     *
     * @param category 分类（可为空）
     * @param tag      标签（可为空，模糊匹配）
     * @param keyword  搜索关键词（可为空）
     * @param pageable 分页参数
     * @return 模板分页列表
     */
    @Query("SELECT p FROM PromptTemplate p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:tag IS NULL OR p.tags LIKE %:tag%) AND " +
           "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.description LIKE %:keyword%)")
    Page<PromptTemplate> findByFilters(@Param("category") String category,
                                       @Param("tag") String tag,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    /**
     * 查询指定分类的 Prompt 模板
     *
     * @param category 分类
     * @return 模板列表
     */
    List<PromptTemplate> findByCategory(String category);

    /**
     * 检查模板名称是否已存在
     *
     * @param name 模板名称
     * @return 是否存在
     */
    boolean existsByName(String name);
}
