package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据集数据访问接口
 * <p>
 * 提供数据集实体的 CRUD 操作和自定义查询方法，支持按状态、分类、关键词等条件查询。
 * </p>
 *
 * @see Dataset
 */
@Repository
public interface DatasetRepository extends JpaRepository<Dataset, String> {

    /**
     * 查询指定 ID 的数据集（排除指定状态）
     * <p>
     * 用于查询有效数据集，排除已删除等状态的数据集。
     * </p>
     *
     * @param id            数据集 ID
     * @param excludeStatus 要排除的状态
     * @return 数据集 Optional
     */
    Optional<Dataset> findByIdAndStatusNot(String id, Dataset.DatasetStatus excludeStatus);

    /**
     * 分页查询数据集（排除指定状态）
     *
     * @param excludeStatus 要排除的状态
     * @param pageable      分页参数
     * @return 数据集分页列表
     */
    Page<Dataset> findByStatusNot(Dataset.DatasetStatus excludeStatus, Pageable pageable);

    /**
     * 分页查询指定分类的数据集（排除指定状态）
     *
     * @param category      分类
     * @param excludeStatus 要排除的状态
     * @param pageable      分页参数
     * @return 数据集分页列表
     */
    Page<Dataset> findByCategoryAndStatusNot(String category, Dataset.DatasetStatus excludeStatus, Pageable pageable);

    /**
     * 搜索数据集（按关键词匹配名称、描述或标签）
     *
     * @param keyword       搜索关键词
     * @param excludeStatus 要排除的状态
     * @param pageable      分页参数
     * @return 数据集分页列表
     */
    @Query("SELECT d FROM Dataset d WHERE d.status != :excludeStatus AND " +
           "(d.name LIKE %:keyword% OR d.description LIKE %:keyword% OR d.tags LIKE %:keyword%)")
    Page<Dataset> searchByKeyword(@Param("keyword") String keyword,
                                  @Param("excludeStatus") Dataset.DatasetStatus excludeStatus,
                                  Pageable pageable);

    /**
     * 查询名称包含指定关键词的数据集（排除指定状态）
     *
     * @param name          名称关键词
     * @param excludeStatus 要排除的状态
     * @return 数据集列表
     */
    List<Dataset> findByNameContainingAndStatusNot(String name, Dataset.DatasetStatus excludeStatus);

    /**
     * 检查名称是否已存在（排除指定状态）
     *
     * @param name          数据集名称
     * @param excludeStatus 要排除的状态
     * @return 是否存在
     */
    boolean existsByNameAndStatusNot(String name, Dataset.DatasetStatus excludeStatus);
}
