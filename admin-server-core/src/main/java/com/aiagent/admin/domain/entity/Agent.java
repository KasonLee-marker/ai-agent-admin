package com.aiagent.admin.domain.entity;

import com.aiagent.admin.domain.enums.AgentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Agent 实体类
 * <p>
 * 用于管理 Agent 配置信息，Agent 是 LLM + Tools + Prompt 的组合配置。
 * 支持以下功能：
 * <ul>
 *   <li>绑定 AI 模型</li>
 *   <li>配置系统提示词</li>
 *   <li>绑定多个工具</li>
 *   <li>运行参数配置（temperature、maxTokens）</li>
 *   <li>生命周期状态管理（DRAFT → PUBLISHED → ARCHIVED）</li>
 * </ul>
 * </p>
 *
 * @see AgentStatus
 * @see com.aiagent.admin.domain.entity.AgentTool
 */
@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    /**
     * Agent 唯一标识符
     */
    @Id
    @Column(length = 64)
    private String id;

    /**
     * Agent 名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Agent 描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Agent 版本号
     * <p>
     * 用于版本管理，默认为 1.0.0。
     * </p>
     */
    @Column(length = 20)
    @Builder.Default
    private String version = "1.0.0";

    /**
     * 绑定的模型配置 ID
     * <p>
     * Agent 必须绑定一个模型才能执行。
     * </p>
     */
    @Column(name = "model_id", nullable = false, length = 64)
    private String modelId;

    /**
     * 系统提示词
     * <p>
     * 定义 Agent 的行为和角色，可引用 Prompt 模板。
     * </p>
     */
    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    /**
     * Agent 运行配置
     * <p>
     * JSON 格式，包含：
     * <ul>
     *   <li>temperature - 输出随机性（默认 0.7）</li>
     *   <li>maxTokens - 最大输出 Token 数（默认 4096）</li>
     * </ul>
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config")
    @Builder.Default
    private String config = "{\"temperature\": 0.7, \"maxTokens\": 4096}";

    /**
     * Agent 状态
     * <p>
     * DRAFT - 草稿，可编辑测试
     * PUBLISHED - 发布，可被外部调用
     * ARCHIVED - 归档，不可编辑调用
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private AgentStatus status = AgentStatus.DRAFT;

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

    /**
     * 持久化前初始化默认值
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (status == null) {
            status = AgentStatus.DRAFT;
        }
        if (version == null || version.isEmpty()) {
            version = "1.0.0";
        }
        if (config == null || config.isEmpty()) {
            config = "{\"temperature\": 0.7, \"maxTokens\": 4096}";
        }
    }
}