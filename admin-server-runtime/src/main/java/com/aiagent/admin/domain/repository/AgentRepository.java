package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.Agent;
import com.aiagent.admin.domain.enums.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Agent 数据访问接口
 * <p>
 * 提供 Agent 实体的 CRUD 操作和自定义查询方法，支持按状态、模型等条件查询。
 * </p>
 *
 * @see Agent
 * @see AgentStatus
 */
@Repository
public interface AgentRepository extends JpaRepository<Agent, String>, JpaSpecificationExecutor<Agent> {

    /**
     * 查找指定状态的 Agent 列表
     *
     * @param status Agent 状态
     * @return Agent 列表
     */
    List<Agent> findByStatus(AgentStatus status);

    /**
     * 查找指定模型的所有 Agent
     *
     * @param modelId 模型 ID
     * @return Agent 列表
     */
    List<Agent> findByModelId(String modelId);

    /**
     * 检查 Agent 名称是否已存在
     *
     * @param name Agent 名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查 Agent 名称是否已存在（排除指定 ID）
     *
     * @param name      Agent 名称
     * @param excludeId 排除的 Agent ID
     * @return 是否存在
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Agent a WHERE a.name = :name AND a.id != :excludeId")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("excludeId") String excludeId);

    /**
     * 按条件筛选 Agent
     * <p>
     * 支持状态、模型、关键词（匹配名称或描述）筛选。
     * </p>
     *
     * @param status  Agent 状态（可为空）
     * @param modelId 模型 ID（可为空）
     * @param keyword 搜索关键词（可为空）
     * @return 筛选后的 Agent 列表
     */
    @Query("SELECT a FROM Agent a WHERE " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:modelId IS NULL OR a.modelId = :modelId) AND " +
            "(:keyword IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(a.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    List<Agent> findByFilters(@Param("status") AgentStatus status,
                              @Param("modelId") String modelId,
                              @Param("keyword") String keyword);
}