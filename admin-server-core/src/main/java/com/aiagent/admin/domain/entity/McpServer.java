package com.aiagent.admin.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * MCP Server 配置实体
 * <p>
 * 定义 MCP Server 的连接配置，支持 stdio 和 SSE transport。
 * Agent 可以绑定 MCP Server 提供的工具。
 * </p>
 *
 * @see McpTool
 */
@Entity
@Table(name = "mcp_servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpServer {

    /**
     * MCP Server 唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * MCP Server 名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Transport 类型
     * <p>
     * 支持：stdio、sse
     * </p>
     */
    @Column(name = "transport_type", length = 20)
    @Builder.Default
    private String transportType = "stdio";

    /**
     * 启动命令（stdio transport）
     * <p>
     * 例如：uvx、npx、python
     * </p>
     */
    @Column(name = "command", length = 200)
    private String command;

    /**
     * SSE URL（sse transport）
     * <p>
     * 例如：https://xxx/sse
     * </p>
     */
    @Column(name = "url", length = 500)
    private String url;

    /**
     * 命令参数
     * <p>
     * JSON 数组格式，例如：["mcp-server-fetch", "--port", "8080"]
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "args", columnDefinition = "JSONB")
    @Builder.Default
    private String args = "[]";

    /**
     * 环境变量
     * <p>
     * JSON 对象格式，例如：{"API_KEY": "xxx"}
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "env", columnDefinition = "JSONB")
    @Builder.Default
    private String env = "{}";

    /**
     * MCP Server 状态
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}