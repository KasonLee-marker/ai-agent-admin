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

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, String> {

    Optional<Dataset> findByIdAndStatusNot(String id, Dataset.DatasetStatus status);

    Page<Dataset> findByStatusNot(Dataset.DatasetStatus status, Pageable pageable);

    Page<Dataset> findByCategoryAndStatusNot(String category, Dataset.DatasetStatus status, Pageable pageable);

    @Query("SELECT d FROM Dataset d WHERE d.status != :excludeStatus AND " +
           "(d.name LIKE %:keyword% OR d.description LIKE %:keyword% OR d.tags LIKE %:keyword%)")
    Page<Dataset> searchByKeyword(@Param("keyword") String keyword,
                                  @Param("excludeStatus") Dataset.DatasetStatus excludeStatus,
                                  Pageable pageable);

    List<Dataset> findByNameContainingAndStatusNot(String name, Dataset.DatasetStatus status);

    boolean existsByNameAndStatusNot(String name, Dataset.DatasetStatus status);
}
