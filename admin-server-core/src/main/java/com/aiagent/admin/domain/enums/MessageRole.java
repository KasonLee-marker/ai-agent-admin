package com.aiagent.admin.domain.enums;

import com.aiagent.admin.domain.entity.ChatMessage;
import com.aiagent.admin.domain.entity.RagMessage;
import lombok.Getter;

/**
 * 消息角色枚举
 * <p>
 * 定义对话消息的角色类型：
 * <ul>
 *   <li>USER - 用户消息，输入的问题或请求</li>
 *   <li>ASSISTANT - 助手消息，AI 生成的回答</li>
 *   <li>SYSTEM - 系统消息，预设的提示词或指令</li>
 * </ul>
 * </p>
 *
 * @see ChatMessage
 * @see RagMessage
 */
@Getter
public enum MessageRole {

    /**
     * 用户消息
     */
    USER("用户"),

    /**
     * 助手消息
     */
    ASSISTANT("助手"),

    /**
     * 系统消息
     */
    SYSTEM("系统");

    /**
     * 显示名称
     */
    private final String displayName;

    MessageRole(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 从字符串解析消息角色
     *
     * @param role 角色字符串（大小写不敏感）
     * @return 对应的消息角色
     * @throws IllegalArgumentException 如果角色字符串无效
     */
    public static MessageRole fromString(String role) {
        for (MessageRole messageRole : values()) {
            if (messageRole.name().equalsIgnoreCase(role)) {
                return messageRole;
            }
        }
        throw new IllegalArgumentException("Unknown message role: " + role);
    }
}
