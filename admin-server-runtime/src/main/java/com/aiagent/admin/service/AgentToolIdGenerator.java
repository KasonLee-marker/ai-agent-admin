package com.aiagent.admin.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * AgentTool 绑定关系 ID 生成器
 * <p>
 * 生成格式：at_{timestamp_hex}{random_suffix}
 * </p>
 */
@Component
public class AgentToolIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * 生成 AgentTool ID
     *
     * @return AgentTool ID，格式：at_xxxx
     */
    public String generateId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String randomPart = ENCODER.encodeToString(bytes);
        long timestamp = Instant.now().toEpochMilli();
        return "at_" + Long.toHexString(timestamp) + randomPart.substring(0, 8);
    }
}