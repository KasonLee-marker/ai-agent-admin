package com.aiagent.admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 提示词模板实体
 * <p>
 * 用于管理可复用的提示词模板：
 * <ul>
 *   <li>模板内容 - 支持变量占位符（如 {{variable}}）</li>
 *   <li>版本管理 - 修改时自动创建版本记录</li>
 *   <li>分类和标签 - 便于组织管理</li>
 * </ul>
 * </p>
 *
 * @see PromptVersion
 * @see ChatSession
 */
@Entity
@Table(name = "prompt_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {

    /**
     * 模板唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 模板名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 模板内容（支持 {{variable}} 占位符）
     */
    @Column(nullable = false, length = 5000)
    private String content;

    /**
     * 模板描述
     */
    @Column(length = 1000)
    private String description;

    /**
     * 模板分类
     */
    @Column(length = 100)
    private String category;

    /**
     * 标签（逗号分隔）
     */
    @Column(length = 500)
    private String tags;

    /**
     * 当前版本号
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * 变量列表（JSON 格式）
     */
    @Column(length = 1000)
    private String variables;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    @Column(length = 100)
    private String createdBy;
}
