package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.EvaluationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, String> {

    List<EvaluationResult> findByJobId(String jobId);

    Page<EvaluationResult> findByJobIdOrderByCreatedAtAsc(String jobId, Pageable pageable);

    List<EvaluationResult> findByJobIdAndStatus(String jobId, EvaluationResult.ResultStatus status);

    @Query("SELECT COUNT(r) FROM EvaluationResult r WHERE r.jobId = :jobId")
    long countByJobId(@Param("jobId") String jobId);

    @Query("SELECT COUNT(r) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = :status")
    long countByJobIdAndStatus(@Param("jobId") String jobId, @Param("status") EvaluationResult.ResultStatus status);

    @Query("SELECT AVG(r.latencyMs) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = 'SUCCESS'")
    Double calculateAverageLatencyByJobId(@Param("jobId") String jobId);

    @Query("SELECT SUM(r.inputTokens) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = 'SUCCESS'")
    Long calculateTotalInputTokensByJobId(@Param("jobId") String jobId);

    @Query("SELECT SUM(r.outputTokens) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = 'SUCCESS'")
    Long calculateTotalOutputTokensByJobId(@Param("jobId") String jobId);

    Optional<EvaluationResult> findById(String id);

    void deleteByJobId(String jobId);
}
