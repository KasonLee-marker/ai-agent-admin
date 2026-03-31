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
        // Format: mdl_<timestamp_hex><random>
        String[] parts = id.split("_");
        assertTrue(parts.length >= 2);
        assertEquals("mdl", parts[0]);
        assertTrue(parts[1].length() >= 16); // timestamp hex + random part
    }
}
