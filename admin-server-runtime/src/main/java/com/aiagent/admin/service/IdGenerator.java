package com.aiagent.admin.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Component
public class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public String generateId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String randomPart = ENCODER.encodeToString(bytes);
        long timestamp = Instant.now().toEpochMilli();
        return "mdl_" + Long.toHexString(timestamp) + randomPart.substring(0, 8);
    }
}
