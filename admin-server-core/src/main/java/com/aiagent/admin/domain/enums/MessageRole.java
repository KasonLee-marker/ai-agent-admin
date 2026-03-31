package com.aiagent.admin.domain.enums;

import lombok.Getter;

@Getter
public enum MessageRole {
    USER("用户"),
    ASSISTANT("助手"),
    SYSTEM("系统");

    private final String displayName;

    MessageRole(String displayName) {
        this.displayName = displayName;
    }

    public static MessageRole fromString(String role) {
        for (MessageRole messageRole : values()) {
            if (messageRole.name().equalsIgnoreCase(role)) {
                return messageRole;
            }
        }
        throw new IllegalArgumentException("Unknown message role: " + role);
    }
}
