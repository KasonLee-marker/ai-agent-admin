package com.aiagent.admin.domain.enums;

/**
 * 工具类别枚举
 * <p>
 * 定义工具的功能类别，用于分类展示和筛选。
 * </p>
 *
 * @see com.aiagent.admin.domain.entity.Tool
 */
public enum ToolCategory {

    /**
     * 通用工具类别
     */
    GENERAL,

    /**
     * 搜索类工具
     */
    SEARCH,

    /**
     * 计算类工具
     */
    CALCULATION,

    /**
     * 知识检索类工具
     */
    KNOWLEDGE,

    /**
     * Shell 命令类工具
     */
    SHELL,

    /**
     * HTTP 请求类工具
     */
    HTTP,

    /**
     * 代码执行类工具
     */
    CODE_EXECUTION
}