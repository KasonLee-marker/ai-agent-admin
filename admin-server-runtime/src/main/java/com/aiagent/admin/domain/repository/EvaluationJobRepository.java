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

/**
 * 评估任务数据访问接口
 * <p>
 * 提供评估任务实体的 CRUD 操作和自定义查询方法，支持按状态、关键词等条件查询，
 * 以及查询运行中的任务（用于进度更新）。
 * </p>
 *
 * @see EvaluationJob
 */
@Repository
public interface EvaluationJobRepository extends JpaRepository<EvaluationJob, String> {

    /**
     * 查询指定 ID 的评估任务
     *
     * @param id 任务 ID
     * @return 任务 Optional
     */
    Optional<EvaluationJob> findById(String id);

    /**
     * 分页查询所有评估任务（按创建时间倒序）
     *
     * @param pageable 分页参数
     * @return 任务分页列表
     */
    Page<EvaluationJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 分页查询指定状态的评估任务（按创建时间倒序）
     *
     * @param status   任务状态
     * @param pageable 分页参数
     * @return 任务分页列表
     */
    Page<EvaluationJob> findByStatusOrderByCreatedAtDesc(EvaluationJob.JobStatus status, Pageable pageable);

    /**
     * 搜索评估任务（按关键词匹配名称或描述）
     *
     * @param keyword  搜索关键词
     * @param pageable 分页参数
     * @return 任务分页列表
     */
    @Query("SELECT j FROM EvaluationJob j WHERE " +
           "(j.name LIKE %:keyword% OR j.description LIKE %:keyword%) " +
           "ORDER BY j.createdAt DESC")
    Page<EvaluationJob> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查询指定状态列表的评估任务
     * <p>
     * 用于查询所有运行中或待运行的任务（用于定时调度）。
     * </p>
     *
     * @param statuses 状态列表
     * @return 任务列表
     */
    List<EvaluationJob> findByStatusIn(List<EvaluationJob.JobStatus> statuses);

    /**
     * 查询运行中的指定任务
     * <p>
     * 用于验证任务是否仍在运行，防止重复操作。
     * </p>
     *
     * @param jobId 任务 ID
     * @return 任务 Optional
     */
    @Query("SELECT j FROM EvaluationJob j WHERE j.status = 'RUNNING' AND j.id = :jobId")
    Optional<EvaluationJob> findRunningJobById(@Param("jobId") String jobId);
}
