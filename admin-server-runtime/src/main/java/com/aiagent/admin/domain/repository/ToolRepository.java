package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tool 数据访问接口
 * <p>
 * 提供 Tool 实体的 CRUD 操作和自定义查询方法，支持按类型、类别等条件查询。
 * </p>
 *
 * @see Tool
 * @see ToolType
 * @see ToolCategory
 */
@Repository
public interface ToolRepository extends JpaRepository<Tool, String>, JpaSpecificationExecutor<Tool> {

    /**
     * 根据名称查找工具
     *
     * @param name 工具名称
     * @return 工具 Optional
     */
    Optional<Tool> findByName(String name);

    /**
     * 检查工具名称是否已存在
     *
     * @param name 工具名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 查找指定类型的工具
     *
     * @param type 工具类型
     * @return 工具列表
     */
    List<Tool> findByType(ToolType type);

    /**
     * 查找指定类别的工具
     *
     * @param category 工具类别
     * @return 工具列表
     */
    List<Tool> findByCategory(ToolCategory category);

    /**
     * 查找指定执行器的工具
     * <p>
     * 用于查询某个 MCP Server 的所有工具（executor = mcpServerId）
     * </p>
     *
     * @param executor 执行器标识
     * @return 工具列表
     */
    List<Tool> findByExecutor(String executor);

    /**
     * 查找指定执行器和类型的工具
     * <p>
     * 用于查询某个 MCP Server 的所有 MCP 类型工具
     * </p>
     *
     * @param executor 执行器标识（MCP Server ID）
     * @param type     工具类型（MCP）
     * @return 工具列表
     */
    List<Tool> findByExecutorAndType(String executor, ToolType type);

    /**
     * 根据config中的mcpServerId查询MCP工具
     * <p>
     * PostgreSQL JSON查询：config->>'mcpServerId' = mcpServerId
     * </p>
     *
     * @param mcpServerId MCP Server ID
     * @return MCP工具列表
     */
    @Query(value = "SELECT * FROM tools WHERE type = 'MCP' AND config::json->>'mcpServerId' = :mcpServerId", nativeQuery = true)
    List<Tool> findByMcpServerId(@Param("mcpServerId") String mcpServerId);

    /**
     * 按条件筛选工具
     * <p>
     * 支持类型、类别、关键词筛选。
     * </p>
     *
     * @param type     工具类型（可为空）
     * @param category 工具类别（可为空）
     * @param keyword  搜索关键词（可为空）
     * @return 筛选后的工具列表
     */
    @Query("SELECT t FROM Tool t WHERE " +
            "(:type IS NULL OR t.type = :type) AND " +
            "(:category IS NULL OR t.category = :category) AND " +
            "(:keyword IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    List<Tool> findByFilters(@Param("type") ToolType type,
                             @Param("category") ToolCategory category,
                             @Param("keyword") String keyword);
}