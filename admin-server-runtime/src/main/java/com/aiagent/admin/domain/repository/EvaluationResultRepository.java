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

/**
 * 评估结果数据访问接口
 * <p>
 * 提供评估结果实体的 CRUD 操作和自定义查询方法，支持按任务、状态等条件查询，
 * 以及统计和聚合计算（平均延迟、Token 消耗等）。
 * </p>
 *
 * @see EvaluationResult
 */
@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, String> {

    /**
     * 查询任务的所有评估结果
     *
     * @param jobId 任务 ID
     * @return 结果列表
     */
    List<EvaluationResult> findByJobId(String jobId);

    /**
     * 分页查询任务的评估结果（按创建时间升序）
     *
     * @param jobId    任务 ID
     * @param pageable 分页参数
     * @return 结果分页列表
     */
    Page<EvaluationResult> findByJobIdOrderByCreatedAtAsc(String jobId, Pageable pageable);

    /**
     * 查询任务指定状态的评估结果
     *
     * @param jobId  任务 ID
     * @param status 结果状态
     * @return 结果列表
     */
    List<EvaluationResult> findByJobIdAndStatus(String jobId, EvaluationResult.ResultStatus status);

    /**
     * 统计任务的结果数量
     *
     * @param jobId 任务 ID
     * @return 结果数量
     */
    @Query("SELECT COUNT(r) FROM EvaluationResult r WHERE r.jobId = :jobId")
    long countByJobId(@Param("jobId") String jobId);

    /**
     * 统计任务指定状态的结果数量
     *
     * @param jobId  任务 ID
     * @param status 结果状态
     * @return 结果数量
     */
    @Query("SELECT COUNT(r) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = :status")
    long countByJobIdAndStatus(@Param("jobId") String jobId, @Param("status") EvaluationResult.ResultStatus status);

    /**
     * 计算任务的平均延迟（仅统计成功结果）
     *
     * @param jobId 任务 ID
     * @return 平均延迟（毫秒）
     */
    @Query("SELECT AVG(r.latencyMs) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = 'SUCCESS'")
    Double calculateAverageLatencyByJobId(@Param("jobId") String jobId);

    /**
     * 计算任务的输入 Token 总消耗（仅统计成功结果）
     *
     * @param jobId 任务 ID
     * @return 输入 Token 总量
     */
    @Query("SELECT SUM(r.inputTokens) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = 'SUCCESS'")
    Long calculateTotalInputTokensByJobId(@Param("jobId") String jobId);

    /**
     * 计算任务的输出 Token 总消耗（仅统计成功结果）
     *
     * @param jobId 任务 ID
     * @return 输出 Token 总量
     */
    @Query("SELECT SUM(r.outputTokens) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = 'SUCCESS'")
    Long calculateTotalOutputTokensByJobId(@Param("jobId") String jobId);

    /**
     * 查询指定 ID 的评估结果
     *
     * @param id 结果 ID
     * @return 结果 Optional
     */
    Optional<EvaluationResult> findById(String id);

    /**
     * 删除任务的所有评估结果
     *
     * @param jobId 任务 ID
     */
    void deleteByJobId(String jobId);

    /**
     * 计算任务的平均 AI 得分（仅统计成功且有得分的结果）
     *
     * @param jobId 任务 ID
     * @return 平均 AI 得分（0-100）
     */
    @Query("SELECT AVG(r.score) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = 'SUCCESS' AND r.score IS NOT NULL")
    Double calculateAverageScoreByJobId(@Param("jobId") String jobId);

    /**
     * 计算任务的平均语义相似度（仅统计成功且有值的结果）
     *
     * @param jobId 任务 ID
     * @return 平均语义相似度（0-1）
     */
    @Query("SELECT AVG(r.semanticSimilarity) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = 'SUCCESS' AND r.semanticSimilarity IS NOT NULL")
    Double calculateAverageSemanticSimilarityByJobId(@Param("jobId") String jobId);

    /**
     * 计算任务的平均忠实度（仅统计成功且有值的结果）
     *
     * @param jobId 任务 ID
     * @return 平均忠实度（0-1）
     */
    @Query("SELECT AVG(r.faithfulness) FROM EvaluationResult r WHERE r.jobId = :jobId AND r.status = 'SUCCESS' AND r.faithfulness IS NOT NULL")
    Double calculateAverageFaithfulnessByJobId(@Param("jobId") String jobId);
}
