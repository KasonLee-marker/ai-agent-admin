package com.aiagent.admin.domain.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ModelProvider {
    // 国际供应商
    // 注意：Spring AI OpenAiApi 会自动添加 /v1/chat/completions 路径
    // 所以 defaultBaseUrl 不应该包含 /v1 后缀
    OPENAI("OpenAI", "https://api.openai.com", List.of(
            BuiltinModel.of("gpt-4", "GPT-4", "Most capable model"),
            BuiltinModel.of("gpt-4-turbo", "GPT-4 Turbo", "Latest GPT-4"),
            BuiltinModel.of("gpt-4o", "GPT-4o", "Multimodal model"),
            BuiltinModel.of("gpt-3.5-turbo", "GPT-3.5 Turbo", "Fast and efficient"),
            BuiltinModel.of("o1-preview", "O1 Preview", "Reasoning model"),
            BuiltinModel.of("o1-mini", "O1 Mini", "Fast reasoning model")
    )),
    ANTHROPIC("Anthropic", "https://api.anthropic.com", List.of(
            BuiltinModel.of("claude-3-opus", "Claude 3 Opus", "Most powerful model"),
            BuiltinModel.of("claude-3-sonnet", "Claude 3 Sonnet", "Balanced performance"),
            BuiltinModel.of("claude-3-haiku", "Claude 3 Haiku", "Fast and efficient"),
            BuiltinModel.of("claude-3-5-sonnet", "Claude 3.5 Sonnet", "Latest Claude model")
    )),
    // 国内供应商 (OpenAI Compatible)
    SILICONFLOW("硅基流动 (SiliconFlow)", "https://api.siliconflow.cn", List.of(
            BuiltinModel.of("Qwen/Qwen2.5-72B-Instruct", "Qwen2.5-72B", "阿里通义千问"),
            BuiltinModel.of("deepseek-ai/DeepSeek-V3", "DeepSeek-V3", "DeepSeek最新"),
            BuiltinModel.of("THUDM/glm-4-9b-chat", "GLM-4-9B", "智谱GLM轻量"),
            BuiltinModel.of("meta-llama/Meta-Llama-3.1-70B-Instruct", "Llama-3.1-70B", "Meta Llama大模型")
    )),
    MOONSHOT("月之暗面 (Moonshot/Kimi)", "https://api.moonshot.cn", List.of(
            BuiltinModel.of("moonshot-v1-8k", "Moonshot V1 8K", "8K上下文"),
            BuiltinModel.of("moonshot-v1-32k", "Moonshot V1 32K", "32K上下文"),
            BuiltinModel.of("moonshot-v1-128k", "Moonshot V1 128K", "128K长文本")
    )),
    ZHIPU("智谱AI (GLM)", "https://open.bigmodel.cn/api/paas/v4", List.of(
            BuiltinModel.of("glm-4", "GLM-4", "智谱最新模型"),
            BuiltinModel.of("glm-4-flash", "GLM-4 Flash", "快速响应"),
            BuiltinModel.of("glm-4-plus", "GLM-4 Plus", "增强版"),
            BuiltinModel.of("glm-4-long", "GLM-4 Long", "长文本模型")
    )),
    DASHSCOPE("阿里云百炼 (DashScope)", "https://dashscope.aliyuncs.com/compatible-mode", List.of(
            BuiltinModel.of("qwen-turbo", "Qwen Turbo", "快速响应"),
            BuiltinModel.of("qwen-plus", "Qwen Plus", "平衡性能"),
            BuiltinModel.of("qwen-max", "Qwen Max", "最强能力"),
            BuiltinModel.of("qwen-long", "Qwen Long", "长文本"),
            BuiltinModel.of("text-embedding-v3", "Text Embedding V3", "文本向量")
    )),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", List.of(
            BuiltinModel.of("deepseek-chat", "DeepSeek Chat", "通用对话"),
            BuiltinModel.of("deepseek-coder", "DeepSeek Coder", "代码专用"),
            BuiltinModel.of("deepseek-reasoner", "DeepSeek Reasoner", "推理模型")
    )),
    // 本地部署 - Ollama 使用不同的路径格式，不经过 /v1
    OLLAMA("Ollama (本地)", "http://localhost:11434", List.of(
            BuiltinModel.of("llama3.1", "Llama 3.1", "Meta最新"),
            BuiltinModel.of("llama3", "Llama 3", "Meta Llama 3"),
            BuiltinModel.of("qwen2.5", "Qwen 2.5", "通义千问"),
            BuiltinModel.of("deepseek-v2", "DeepSeek V2", "DeepSeek本地版"),
            BuiltinModel.of("mistral", "Mistral", "Mistral AI")
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
