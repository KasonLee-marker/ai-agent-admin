package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.AgentTool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Agent-Tool 绑定数据访问接口
 * <p>
 * 提供 AgentTool 实体的 CRUD 操作和自定义查询方法，
 * 管理 Agent 与工具的绑定关系。
 * </p>
 *
 * @see AgentTool
 */
@Repository
public interface AgentToolRepository extends JpaRepository<AgentTool, String>, JpaSpecificationExecutor<AgentTool> {

    /**
     * 查找指定 Agent 的所有工具绑定
     *
     * @param agentId Agent ID
     * @return 工具绑定列表
     */
    List<AgentTool> findByAgentId(String agentId);

    /**
     * 查找指定 Agent 的启用工具绑定
     *
     * @param agentId Agent ID
     * @return 启用的工具绑定列表
     */
    List<AgentTool> findByAgentIdAndEnabledTrue(String agentId);

    /**
     * 查找指定工具被哪些 Agent 绑定
     *
     * @param toolId 工具 ID
     * @return 工具绑定列表
     */
    List<AgentTool> findByToolId(String toolId);

    /**
     * 查找指定的 Agent-Tool 绑定关系
     *
     * @param agentId Agent ID
     * @param toolId  工具 ID
     * @return 绑定关系 Optional
     */
    Optional<AgentTool> findByAgentIdAndToolId(String agentId, String toolId);

    /**
     * 检查 Agent 是否已绑定指定工具
     *
     * @param agentId Agent ID
     * @param toolId  工具 ID
     * @return 是否已绑定
     */
    boolean existsByAgentIdAndToolId(String agentId, String toolId);

    /**
     * 删除指定 Agent 的所有工具绑定
     *
     * @param agentId Agent ID
     */
    @Modifying
    @Query("DELETE FROM AgentTool at WHERE at.agentId = :agentId")
    void deleteByAgentId(@Param("agentId") String agentId);

    /**
     * 删除指定的 Agent-Tool 绑定
     *
     * @param agentId Agent ID
     * @param toolId  工具 ID
     */
    @Modifying
    @Query("DELETE FROM AgentTool at WHERE at.agentId = :agentId AND at.toolId = :toolId")
    void deleteByAgentIdAndToolId(@Param("agentId") String agentId, @Param("toolId") String toolId);
}