package com.aiagent.admin.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Tool ID 生成器
 * <p>
 * 生成格式：tool_{timestamp_hex}{random_suffix}
 * </p>
 */
@Component
public class ToolIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * 生成 Tool ID
     *
     * @return Tool ID，格式：tool_xxxx
     */
    public String generateId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String randomPart = ENCODER.encodeToString(bytes);
        long timestamp = Instant.now().toEpochMilli();
        return "tool_" + Long.toHexString(timestamp) + randomPart.substring(0, 8);
    }
}