package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.EvaluationJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationJobRepository extends JpaRepository<EvaluationJob, String> {

    Optional<EvaluationJob> findById(String id);

    Page<EvaluationJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<EvaluationJob> findByStatusOrderByCreatedAtDesc(EvaluationJob.JobStatus status, Pageable pageable);

    @Query("SELECT j FROM EvaluationJob j WHERE " +
           "(j.name LIKE %:keyword% OR j.description LIKE %:keyword%) " +
           "ORDER BY j.createdAt DESC")
    Page<EvaluationJob> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    List<EvaluationJob> findByStatusIn(List<EvaluationJob.JobStatus> statuses);

    @Query("SELECT j FROM EvaluationJob j WHERE j.status = 'RUNNING' AND j.id = :jobId")
    Optional<EvaluationJob> findRunningJobById(@Param("jobId") String jobId);
}
