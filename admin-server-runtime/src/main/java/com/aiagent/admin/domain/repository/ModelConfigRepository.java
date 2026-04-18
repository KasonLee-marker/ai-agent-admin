package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.ModelConfig;
import com.aiagent.admin.domain.enums.ModelProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型配置数据访问接口
 * <p>
 * 提供模型配置实体的 CRUD 操作和自定义查询方法，支持按 Provider、活跃状态等条件查询，
 * 以及默认模型和默认 Embedding 模型的设置与清除。
 * </p>
 *
 * @see ModelConfig
 * @see ModelProvider
 */
@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, String>, JpaSpecificationExecutor<ModelConfig> {

    /**
     * 查找默认模型配置
     *
     * @return 默认模型 Optional
     */
    Optional<ModelConfig> findByIsDefaultTrue();

    /**
     * 查找默认 Embedding 模型配置
     *
     * @return 默认 Embedding 模型 Optional
     */
    Optional<ModelConfig> findByIsDefaultEmbeddingTrue();

    /**
     * 查找默认且激活的 Embedding 模型配置
     *
     * @return 默认激活的 Embedding 模型 Optional
     */
    Optional<ModelConfig> findByIsDefaultEmbeddingTrueAndIsActiveTrue();

    /**
     * 查找默认且激活的模型配置
     *
     * @return 默认激活的模型 Optional
     */
    Optional<ModelConfig> findByIsDefaultTrueAndIsActiveTrue();

    /**
     * 查找指定 Provider 列表中激活的模型配置
     *
     * @param providers Provider 列表
     * @return 激活的模型配置列表
     */
    List<ModelConfig> findByProviderInAndIsActiveTrue(List<ModelProvider> providers);

    /**
     * 查找指定 Provider 的模型配置
     *
     * @param provider Provider
     * @return 模型配置列表
     */
    List<ModelConfig> findByProvider(ModelProvider provider);

    /**
     * 查找所有激活的模型配置
     *
     * @return 激活的模型配置列表
     */
    List<ModelConfig> findByIsActiveTrue();

    /**
     * 清除所有默认模型标记
     * <p>
     * 设置新默认模型前需先清除现有默认标记。
     * </p>
     */
    @Modifying
    @Query("UPDATE ModelConfig m SET m.isDefault = false WHERE m.isDefault = true")
    void clearDefaultModel();

    /**
     * 设置指定模型为默认模型
     *
     * @param id 模型 ID
     */
    @Modifying
    @Query("UPDATE ModelConfig m SET m.isDefault = true WHERE m.id = :id")
    void setDefaultModel(@Param("id") String id);

    /**
     * 清除所有默认 Embedding 模型标记
     */
    @Modifying
    @Query("UPDATE ModelConfig m SET m.isDefaultEmbedding = false WHERE m.isDefaultEmbedding = true")
    void clearDefaultEmbeddingModel();

    /**
     * 设置指定模型为默认 Embedding 模型
     *
     * @param id 模型 ID
     */
    @Modifying
    @Query("UPDATE ModelConfig m SET m.isDefaultEmbedding = true WHERE m.id = :id")
    void setDefaultEmbeddingModel(@Param("id") String id);

    /**
     * 检查模型名称是否已存在
     *
     * @param name 模型名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 按条件筛选模型配置
     * <p>
     * 支持 Provider、活跃状态、关键词（匹配名称或模型名）筛选。
     * </p>
     *
     * @param provider Provider（可为空）
     * @param isActive 活跃状态（可为空）
     * @param keyword  搜索关键词（可为空）
     * @return 筛选后的模型配置列表
     */
    @Query("SELECT m FROM ModelConfig m WHERE " +
           "(:provider IS NULL OR m.provider = :provider) AND " +
           "(:isActive IS NULL OR m.isActive = :isActive) AND " +
            "(:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(m.modelName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    List<ModelConfig> findByFilters(@Param("provider") ModelProvider provider,
                                    @Param("isActive") Boolean isActive,
                                    @Param("keyword") String keyword);
}
