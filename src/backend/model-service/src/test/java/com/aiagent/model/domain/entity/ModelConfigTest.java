package com.aiagent.model.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelConfigTest {

    @Test
    void testModelConfigBuilder() {
        ModelConfig config = ModelConfig.builder()
                .id("mdl_test123")
                .name("Test Model")
                .provider(ModelProvider.OPENAI)
                .modelName("gpt-4")
                .apiKey("test-key")
                .baseUrl("https://api.openai.com/v1")
                .temperature(0.7)
                .maxTokens(2048)
                .topP(1.0)
                .isDefault(true)
                .isActive(true)
                .build();

        assertEquals("mdl_test123", config.getId());
        assertEquals("Test Model", config.getName());
        assertEquals(ModelProvider.OPENAI, config.getProvider());
        assertEquals("gpt-4", config.getModelName());
        assertEquals("test-key", config.getApiKey());
        assertEquals("https://api.openai.com/v1", config.getBaseUrl());
        assertEquals(0.7, config.getTemperature());
        assertEquals(2048, config.getMaxTokens());
        assertEquals(1.0, config.getTopP());
        assertTrue(config.getIsDefault());
        assertTrue(config.getIsActive());
    }

    @Test
    void testPrePersistDefaults() {
        ModelConfig config = new ModelConfig();
        config.prePersist();

        assertFalse(config.getIsDefault());
        assertTrue(config.getIsActive());
        assertEquals(ModelConfig.HealthStatus.UNKNOWN, config.getHealthStatus());
    }

    @Test
    void testHealthStatusEnum() {
        assertEquals(3, ModelConfig.HealthStatus.values().length);
        assertNotNull(ModelConfig.HealthStatus.HEALTHY);
        assertNotNull(ModelConfig.HealthStatus.UNHEALTHY);
        assertNotNull(ModelConfig.HealthStatus.UNKNOWN);
    }
}
