package com.aiagent.admin.domain.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ModelProvider {
    OPENAI("OpenAI", "https://api.openai.com/v1", List.of(
            BuiltinModel.of("gpt-4", "GPT-4", "Most capable model"),
            BuiltinModel.of("gpt-4-turbo", "GPT-4 Turbo", "Latest GPT-4"),
            BuiltinModel.of("gpt-3.5-turbo", "GPT-3.5 Turbo", "Fast and efficient")
    )),
    DASHSCOPE("DashScope (Aliyun)", "https://dashscope.aliyuncs.com/compatible-mode/v1", List.of(
            BuiltinModel.of("qwen-turbo", "Qwen Turbo", "Fast Qwen model"),
            BuiltinModel.of("qwen-plus", "Qwen Plus", "Balanced Qwen model"),
            BuiltinModel.of("qwen-max", "Qwen Max", "Most capable Qwen model"),
            BuiltinModel.of("qwen3.5-omni-plus", "Qwen 3.5 Omni Plus", "Multimodal model"),
            BuiltinModel.of("text-embedding-v1", "Text Embedding V1", "Embedding model")
    )),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", List.of(
            BuiltinModel.of("deepseek-chat", "DeepSeek Chat", "General chat model"),
            BuiltinModel.of("deepseek-coder", "DeepSeek Coder", "Code specialized model")
    )),
    OLLAMA("Ollama", "http://localhost:11434", List.of(
            BuiltinModel.of("llama2", "Llama 2", "Meta Llama 2"),
            BuiltinModel.of("llama3", "Llama 3", "Meta Llama 3"),
            BuiltinModel.of("mistral", "Mistral", "Mistral AI"),
            BuiltinModel.of("codellama", "CodeLlama", "Code specialized")
    ));

    private final String displayName;
    private final String defaultBaseUrl;
    private final List<BuiltinModel> builtinModels;

    ModelProvider(String displayName, String defaultBaseUrl, List<BuiltinModel> builtinModels) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.builtinModels = builtinModels;
    }

    public static ModelProvider fromString(String provider) {
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown provider: " + provider));
    }

    public record BuiltinModel(String name, String displayName, String description) {
        public static BuiltinModel of(String name, String displayName, String description) {
            return new BuiltinModel(name, displayName, description);
        }
    }
}
