package com.aiagent.admin.domain.enums;

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
        assertEquals("阿里云百炼 (DashScope)", ModelProvider.DASHSCOPE.getDisplayName());
        assertEquals("DeepSeek", ModelProvider.DEEPSEEK.getDisplayName());
        assertEquals("Ollama (本地)", ModelProvider.OLLAMA.getDisplayName());
    }

    @Test
    void testGetDefaultBaseUrl() {
        // 注意：Spring AI 会自动添加 /v1/chat/completions 路径
        // 所以 baseUrl 不应该包含 /v1 后缀
        assertEquals("https://api.openai.com", ModelProvider.OPENAI.getDefaultBaseUrl());
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode", ModelProvider.DASHSCOPE.getDefaultBaseUrl());
        assertEquals("https://api.deepseek.com", ModelProvider.DEEPSEEK.getDefaultBaseUrl());
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

    @Test
    void testModelType() {
        assertEquals(ModelProvider.ModelType.CHAT, ModelProvider.OPENAI.getModelType());
        assertEquals(ModelProvider.ModelType.CHAT, ModelProvider.DASHSCOPE.getModelType());
        assertEquals(ModelProvider.ModelType.EMBEDDING, ModelProvider.OPENAI_EMBEDDING.getModelType());
        assertEquals(ModelProvider.ModelType.EMBEDDING, ModelProvider.DASHSCOPE_EMBEDDING.getModelType());
    }
}