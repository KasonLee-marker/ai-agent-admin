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

@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, String>, JpaSpecificationExecutor<ModelConfig> {

    Optional<ModelConfig> findByIsDefaultTrue();

    /**
     * 查找默认且激活的模型配置
     */
    Optional<ModelConfig> findByIsDefaultTrueAndIsActiveTrue();

    /**
     * 查找指定 provider 列表中激活的模型配置
     */
    List<ModelConfig> findByProviderInAndIsActiveTrue(List<ModelProvider> providers);

    List<ModelConfig> findByProvider(ModelProvider provider);

    List<ModelConfig> findByIsActiveTrue();

    @Modifying
    @Query("UPDATE ModelConfig m SET m.isDefault = false WHERE m.isDefault = true")
    void clearDefaultModel();

    @Modifying
    @Query("UPDATE ModelConfig m SET m.isDefault = true WHERE m.id = :id")
    void setDefaultModel(@Param("id") String id);

    boolean existsByName(String name);

    @Query("SELECT m FROM ModelConfig m WHERE " +
           "(:provider IS NULL OR m.provider = :provider) AND " +
           "(:isActive IS NULL OR m.isActive = :isActive) AND " +
            "(:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(m.modelName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    List<ModelConfig> findByFilters(@Param("provider") ModelProvider provider,
                                    @Param("isActive") Boolean isActive,
                                    @Param("keyword") String keyword);
}
