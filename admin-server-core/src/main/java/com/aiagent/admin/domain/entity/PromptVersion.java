package com.aiagent.admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 提示词版本实体
 * <p>
 * 用于保存提示词模板的历史版本：
 * <ul>
 *   <li>版本号 - 自增序号</li>
 *   <li>内容快照 - 保存当时的模板内容</li>
 *   <li>变更日志 - 记录修改原因</li>
 * </ul>
 * </p>
 *
 * @see PromptTemplate
 */
@Entity
@Table(name = "prompt_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersion {

    /**
     * 版本记录唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 关联模板 ID
     */
    @Column(nullable = false, length = 36)
    private String promptId;

    /**
     * 版本号
     */
    @Column(nullable = false)
    private Integer version;

    /**
     * 版本内容快照
     */
    @Column(nullable = false, length = 5000)
    private String content;

    /**
     * 变更日志（修改原因）
     */
    @Column(length = 1000)
    private String changeLog;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
