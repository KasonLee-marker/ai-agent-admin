package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.AgentExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Agent 执行日志数据访问接口
 * <p>
 * 提供 AgentExecutionLog 实体的 CRUD 操作和查询方法。
 * </p>
 *
 * @see AgentExecutionLog
 */
@Repository
public interface AgentExecutionLogRepository extends JpaRepository<AgentExecutionLog, String>, JpaSpecificationExecutor<AgentExecutionLog> {

    /**
     * 查找指定 Agent 的所有执行日志
     *
     * @param agentId Agent ID
     * @return 执行日志列表（按时间倒序）
     */
    @Query("SELECT l FROM AgentExecutionLog l WHERE l.agentId = :agentId ORDER BY l.createdAt DESC")
    List<AgentExecutionLog> findByAgentIdOrderByCreatedAtDesc(@Param("agentId") String agentId);

    /**
     * 查找指定 Agent 最近 N 条执行日志
     *
     * @param agentId Agent ID
     * @param limit   限制数量
     * @return 执行日志列表
     */
    @Query(value = "SELECT l FROM AgentExecutionLog l WHERE l.agentId = :agentId ORDER BY l.createdAt DESC LIMIT :limit")
    List<AgentExecutionLog> findRecentByAgentId(@Param("agentId") String agentId, @Param("limit") int limit);

    /**
     * 查找指定会话的所有执行日志
     *
     * @param sessionId 会话 ID
     * @return 执行日志列表
     */
    List<AgentExecutionLog> findBySessionId(String sessionId);

    /**
     * 统计指定 Agent 的执行次数
     *
     * @param agentId Agent ID
     * @return 执行次数
     */
    long countByAgentId(String agentId);

    /**
     * 统计指定 Agent 的成功执行次数
     *
     * @param agentId Agent ID
     * @return 成功执行次数
     */
    long countByAgentIdAndSuccessTrue(String agentId);
}