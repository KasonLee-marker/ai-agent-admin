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

@Repository
public interface DatasetItemRepository extends JpaRepository<DatasetItem, String> {

    List<DatasetItem> findByDatasetIdAndVersionAndStatusNot(String datasetId, Integer version, DatasetItem.ItemStatus status);

    Page<DatasetItem> findByDatasetIdAndVersionAndStatusNot(String datasetId, Integer version, DatasetItem.ItemStatus status, Pageable pageable);

    List<DatasetItem> findByDatasetIdAndStatusNot(String datasetId, DatasetItem.ItemStatus status);

    Page<DatasetItem> findByDatasetIdAndStatusNot(String datasetId, DatasetItem.ItemStatus status, Pageable pageable);

    Optional<DatasetItem> findByIdAndStatusNot(String id, DatasetItem.ItemStatus status);

    long countByDatasetIdAndVersionAndStatusNot(String datasetId, Integer version, DatasetItem.ItemStatus status);

    long countByDatasetIdAndStatusNot(String datasetId, DatasetItem.ItemStatus status);

    @Modifying
    @Query("UPDATE DatasetItem di SET di.status = :status WHERE di.datasetId = :datasetId")
    int updateStatusByDatasetId(@Param("datasetId") String datasetId, @Param("status") DatasetItem.ItemStatus status);

    @Modifying
    @Query("UPDATE DatasetItem di SET di.status = :status WHERE di.datasetId = :datasetId AND di.version = :version")
    int updateStatusByDatasetIdAndVersion(@Param("datasetId") String datasetId,
                                          @Param("version") Integer version,
                                          @Param("status") DatasetItem.ItemStatus status);

    void deleteByDatasetId(String datasetId);
}
