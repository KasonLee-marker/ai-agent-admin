package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.DatasetItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据集项数据访问接口
 * <p>
 * 提供数据集项实体的 CRUD 操作和自定义查询方法，支持按数据集、版本、状态等条件查询，
 * 以及批量状态更新操作。
 * </p>
 *
 * @see DatasetItem
 */
@Repository
public interface DatasetItemRepository extends JpaRepository<DatasetItem, String> {

    /**
     * 查询数据集指定版本的数据项（排除指定状态）
     *
     * @param datasetId 数据集 ID
     * @param version   数据集版本
     * @param status    要排除的状态
     * @return 数据项列表
     */
    List<DatasetItem> findByDatasetIdAndVersionAndStatusNot(String datasetId, Integer version, DatasetItem.ItemStatus status);

    /**
     * 分页查询数据集指定版本的数据项（排除指定状态）
     *
     * @param datasetId 数据集 ID
     * @param version   数据集版本
     * @param status    要排除的状态
     * @param pageable  分页参数
     * @return 数据项分页列表
     */
    Page<DatasetItem> findByDatasetIdAndVersionAndStatusNot(String datasetId, Integer version, DatasetItem.ItemStatus status, Pageable pageable);

    /**
     * 查询数据集的所有数据项（排除指定状态）
     *
     * @param datasetId 数据集 ID
     * @param status    要排除的状态
     * @return 数据项列表
     */
    List<DatasetItem> findByDatasetIdAndStatusNot(String datasetId, DatasetItem.ItemStatus status);

    /**
     * 分页查询数据集的数据项（排除指定状态）
     *
     * @param datasetId 数据集 ID
     * @param status    要排除的状态
     * @param pageable  分页参数
     * @return 数据项分页列表
     */
    Page<DatasetItem> findByDatasetIdAndStatusNot(String datasetId, DatasetItem.ItemStatus status, Pageable pageable);

    /**
     * 查询指定 ID 的数据项（排除指定状态）
     *
     * @param id     数据项 ID
     * @param status 要排除的状态
     * @return 数据项 Optional
     */
    Optional<DatasetItem> findByIdAndStatusNot(String id, DatasetItem.ItemStatus status);

    /**
     * 统计数据集指定版本的数据项数量（排除指定状态）
     *
     * @param datasetId 数据集 ID
     * @param version   数据集版本
     * @param status    要排除的状态
     * @return 数据项数量
     */
    long countByDatasetIdAndVersionAndStatusNot(String datasetId, Integer version, DatasetItem.ItemStatus status);

    /**
     * 统计数据集的数据项数量（排除指定状态）
     *
     * @param datasetId 数据集 ID
     * @param status    要排除的状态
     * @return 数据项数量
     */
    long countByDatasetIdAndStatusNot(String datasetId, DatasetItem.ItemStatus status);

    /**
     * 批量更新数据集所有数据项的状态
     *
     * @param datasetId 数据集 ID
     * @param status    新状态
     * @return 更新的数据项数量
     */
    @Modifying
    @Query("UPDATE DatasetItem di SET di.status = :status WHERE di.datasetId = :datasetId")
    int updateStatusByDatasetId(@Param("datasetId") String datasetId, @Param("status") DatasetItem.ItemStatus status);

    /**
     * 批量更新数据集指定版本数据项的状态
     *
     * @param datasetId 数据集 ID
     * @param version   数据集版本
     * @param status    新状态
     * @return 更新的数据项数量
     */
    @Modifying
    @Query("UPDATE DatasetItem di SET di.status = :status WHERE di.datasetId = :datasetId AND di.version = :version")
    int updateStatusByDatasetIdAndVersion(@Param("datasetId") String datasetId,
                                          @Param("version") Integer version,
                                          @Param("status") DatasetItem.ItemStatus status);

    /**
     * 删除数据集的所有数据项
     *
     * @param datasetId 数据集 ID
     */
    void deleteByDatasetId(String datasetId);
}
