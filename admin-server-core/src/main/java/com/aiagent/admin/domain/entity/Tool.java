package com.aiagent.admin.domain.entity;

import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Tool 实体类
 * <p>
 * 用于管理工具配置信息，工具是 Agent 可调用的外部能力。
 * 支持以下类型：
 * <ul>
 *   <li>BUILTIN - 内置工具，平台预定义执行器</li>
 *   <li>CUSTOM - 自定义工具，用户配置的 HTTP API</li>
 *   <li>MCP - MCP 工具，从外部 MCP Server 发现</li>
 * </ul>
 * </p>
 * <p>
 * 每个工具定义 JSON Schema，描述输入参数结构。
 * </p>
 *
 * @see ToolType
 * @see ToolCategory
 * @see com.aiagent.admin.domain.entity.AgentTool
 */
@Entity
@Table(name = "tools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tool {

    /**
     * 工具唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * 工具名称
     * <p>
     * 全局唯一，用于 Agent 调用时识别工具。
     * 内置工具名称：calculator、datetime、knowledge_retrieval、shell_command
     * </p>
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * 工具描述
     * <p>
     * 用于 LLM 理解工具功能，决定何时调用。
     * </p>
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * 工具类别
     * <p>
     * 用于分类展示和筛选。
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private ToolCategory category = ToolCategory.GENERAL;

    /**
     * 工具类型
     * <p>
     * BUILTIN - 内置工具
     * CUSTOM - 自定义 HTTP API
     * MCP - MCP 工具
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ToolType type = ToolType.BUILTIN;

    /**
     * JSON Schema
     * <p>
     * 定义工具输入参数的结构，用于：
     * <ul>
     *   <li>LLM 生成正确的调用参数</li>
     *   <li>参数校验</li>
     * </ul>
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema", nullable = false)
    private String schema;

    /**
     * 执行器标识
     * <p>
     * BUILTIN 工具：Spring Bean 名称（如 calculatorExecutor）
     * CUSTOM 工具：HTTP API endpoint
     * MCP 工具：MCP Server ID
     * </p>
     */
    @Column(length = 100)
    private String executor;

    /**
     * 工具配置
     * <p>
     * JSON 格式，存储工具特定配置：
     * <ul>
     *   <li>CUSTOM 工具：认证信息、请求模板等</li>
     *   <li>BUILTIN 工具：沙盒配置、白名单等</li>
     * </ul>
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config")
    @Builder.Default
    private String config = "{}";

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 持久化前初始化默认值
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (type == null) {
            type = ToolType.BUILTIN;
        }
        if (category == null) {
            category = ToolCategory.GENERAL;
        }
        if (config == null || config.isEmpty()) {
            config = "{}";
        }
    }
}