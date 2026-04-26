package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 简要信息 DTO
 * <p>
 * 用于展示引用某个资源的 Agent 列表。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentInfoDTO {

    /**
     * Agent ID
     */
    private String id;

    /**
     * Agent 名称
     */
    private String name;

    /**
     * Agent 状态
     */
    private String status;
}
