package com.aiagent.admin.domain.repository;

import com.aiagent.admin.domain.entity.McpServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MCP Server 配置数据访问接口
 * <p>
 * 提供 McpServer 实体的 CRUD 操作。
 * </p>
 *
 * @see McpServer
 */
@Repository
public interface McpServerRepository extends JpaRepository<McpServer, String>, JpaSpecificationExecutor<McpServer> {

    /**
     * 查找所有活跃的 MCP Server
     *
     * @return 活跃的 MCP Server 列表
     */
    List<McpServer> findByStatus(String status);

    /**
     * 按名称查找 MCP Server
     *
     * @param name 名称
     * @return MCP Server Optional
     */
    Optional<McpServer> findByName(String name);

    /**
     * 检查名称是否已存在
     *
     * @param name 名称
     * @return 是否存在
     */
    boolean existsByName(String name);
}