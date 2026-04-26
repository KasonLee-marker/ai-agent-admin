package com.aiagent.admin.domain.enums;

/**
 * Agent 状态枚举
 * <p>
 * 定义 Agent 的生命周期状态：
 * <ul>
 *   <li>DRAFT - 草稿状态，可编辑、测试，不可被外部调用</li>
 *   <li>PUBLISHED - 发布状态，可被外部系统调用</li>
 *   <li>ARCHIVED - 归档状态，不可编辑、不可调用</li>
 * </ul>
 * </p>
 *
 * @see com.aiagent.admin.domain.entity.Agent
 */
public enum AgentStatus {

    /**
     * 草稿状态
     * <p>
     * 创建后的初始状态，允许编辑和测试。
     * </p>
     */
    DRAFT,

    /**
     * 发布状态
     * <p>
     * 可被外部系统调用的状态，为 MCP 阶段准备。
     * </p>
     */
    PUBLISHED,

    /**
     * 归档状态
     * <p>
     * 已停用，仅保留历史记录，不可编辑或调用。
     * </p>
     */
    ARCHIVED
}