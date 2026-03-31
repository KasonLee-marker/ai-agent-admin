package com.aiagent.model.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelProviderTest {

    @Test
    void testFromStringValid() {
        assertEquals(ModelProvider.OPENAI, ModelProvider.fromString("OPENAI"));
        assertEquals(ModelProvider.DASHSCOPE, ModelProvider.fromString("dashscope"));
        assertEquals(ModelProvider.DEEPSEEK, ModelProvider.fromString("DeepSeek"));
        assertEquals(ModelProvider.OLLAMA, ModelProvider.fromString("ollama"));
    }

    @Test
    void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ModelProvider.fromString("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> ModelProvider.fromString(""));
    }

    @Test
    void testGetDisplayName() {
        assertEquals("OpenAI", ModelProvider.OPENAI.getDisplayName());
        assertEquals("DashScope (Aliyun)", ModelProvider.DASHSCOPE.getDisplayName());
        assertEquals("DeepSeek", ModelProvider.DEEPSEEK.getDisplayName());
        assertEquals("Ollama", ModelProvider.OLLAMA.getDisplayName());
    }

    @Test
    void testGetDefaultBaseUrl() {
        assertEquals("https://api.openai.com/v1", ModelProvider.OPENAI.getDefaultBaseUrl());
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", ModelProvider.DASHSCOPE.getDefaultBaseUrl());
        assertEquals("https://api.deepseek.com/v1", ModelProvider.DEEPSEEK.getDefaultBaseUrl());
        assertEquals("http://localhost:11434", ModelProvider.OLLAMA.getDefaultBaseUrl());
    }

    @Test
    void testGetBuiltinModels() {
        assertFalse(ModelProvider.OPENAI.getBuiltinModels().isEmpty());
        assertFalse(ModelProvider.DASHSCOPE.getBuiltinModels().isEmpty());
        assertFalse(ModelProvider.DEEPSEEK.getBuiltinModels().isEmpty());
        assertFalse(ModelProvider.OLLAMA.getBuiltinModels().isEmpty());
    }

    @Test
    void testBuiltinModelRecord() {
        ModelProvider.BuiltinModel model = ModelProvider.BuiltinModel.of("test", "Test Model", "Test description");
        assertEquals("test", model.name());
        assertEquals("Test Model", model.displayName());
        assertEquals("Test description", model.description());
    }
}
