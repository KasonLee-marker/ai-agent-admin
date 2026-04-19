package com.aiagent.admin.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IdGeneratorTest {

    private final IdGenerator idGenerator = new IdGenerator();

    @Test
    void testGenerateId() {
        String id = idGenerator.generateId();
        assertNotNull(id);
        assertTrue(id.startsWith("mdl_"));
        assertTrue(id.length() > 10);
    }

    @Test
    void testGenerateIdUnique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String id = idGenerator.generateId();
            assertTrue(ids.add(id), "Generated ID should be unique: " + id);
        }
    }

    @Test
    void testGenerateIdFormat() {
        String id = idGenerator.generateId();
        // Format: mdl_<timestamp_hex><random_8_chars>
        // timestamp_hex length varies based on timestamp value (e.g., 13 chars for 2024)
        String[] parts = id.split("_");
        assertTrue(parts.length >= 2);
        assertEquals("mdl", parts[0]);
        // timestamp hex + 8 random chars = at least 8 chars
        assertTrue(parts[1].length() >= 8);
    }
}
