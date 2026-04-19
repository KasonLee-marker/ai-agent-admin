package com.aiagent.admin.splitter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文句子分割方案对比测试
 * <p>
 * 对比三种方案：HanLP、jieba、自定义规则
 * </p>
 */
public class ChineseSentenceSplitterTest {

    private static final String CHINESE_TEXT = """
            这是第一句话。这是第二句话，包含逗号。这是第三句话！
            这是第四句话？最后一句话没有标点结尾
            
            这是新的一段，段落之间用换行分隔。段内也有句子。测试段落分割效果。
            """;

    /**
     * 方案 1: 自定义规则 - 最简单，无依赖
     * <p>
     * 基于中文标点符号分割句子，适用于大多数场景。
     * </p>
     */
    @Test
    @DisplayName("方案1: 自定义规则句子分割")
    void testCustomRuleSplitter() {
        System.out.println("\n========== 方案1: 自定义规则 (无依赖) ==========\n");

        // 中文句子结束标点：句号、感叹号、问号、分号
        // 注意：需要处理引号内的内容（可选）
        String[] sentenceEndPunctuation = {"。", "！", "？", "；"};

        List<String> sentences = splitByCustomRule(CHINESE_TEXT, sentenceEndPunctuation);

        System.out.println("分句数: " + sentences.size());
        for (int i = 0; i < sentences.size(); i++) {
            System.out.println("句子 " + i + ": \"" + sentences.get(i).trim() + "\"");
        }

        System.out.println("\n优点: 无依赖，实现简单，速度快");
        System.out.println("缺点: 不能处理复杂情况（如引号内的句号）");
        System.out.println("适用场景: RAG 文档分块，对准确度要求不高");
    }

    /**
     * 方案 2: 使用正则表达式 - 简洁版本
     */
    @Test
    @DisplayName("方案2: 正则表达式句子分割")
    void testRegexSplitter() {
        System.out.println("\n========== 方案2: 正则表达式 ==========\n");

        // 正则：匹配中文句子结束标点
        String[] sentences = CHINESE_TEXT.split("[。！？；]+");

        System.out.println("分句数: " + sentences.length);
        for (int i = 0; i < sentences.length; i++) {
            String s = sentences[i].trim();
            if (!s.isEmpty()) {
                System.out.println("句子 " + i + ": \"" + s + "\"");
            }
        }

        System.out.println("\n优点: 一行代码搞定");
        System.out.println("缺点: 标点会被去掉，不能保留原标点");
    }

    /**
     * 方案 3: HanLP - 功能强大但依赖重
     * <p>
     * 需要添加依赖：
     * <dependency>
     * <groupId>com.hankcs</groupId>
     * <artifactId>hanlp</artifactId>
     * <version>portable-1.8.4</version>
     * </dependency>
     * </p>
     */
    @Test
    @DisplayName("方案3: HanLP (需要依赖)")
    void testHanLPSplitter() {
        System.out.println("\n========== 方案3: HanLP ==========\n");
        System.out.println("依赖: com.hankcs:hanlp:portable-1.8.4 (~50MB)");
        System.out.println("模型: 需下载模型文件或使用 portable 版");
        System.out.println();
        System.out.println("代码示例:");
        System.out.println("  List<String> sentences = HanLP.extractSentence(CHINESE_TEXT);");
        System.out.println();
        System.out.println("优点: 准确度高，能处理复杂语法");
        System.out.println("缺点: 依赖重，模型大，启动慢");
        System.out.println("适用场景: 对句子边界要求严格的场景");

        // 实际运行需要添加依赖，这里只展示概念
        System.out.println("\n实际测试需要添加 HanLP 依赖后运行");
    }

    /**
     * 方案 4: jieba - 轻量级分词
     * <p>
     * jieba 主要用于分词，不直接提供句子分割功能。
     * 需要添加依赖：
     * <dependency>
     * <groupId>com.huaban</groupId>
     * <artifactId>jieba-analysis</artifactId>
     * <version>1.0.2</version>
     * </dependency>
     * </p>
     */
    @Test
    @DisplayName("方案4: jieba (主要用于分词)")
    void testJiebaSplitter() {
        System.out.println("\n========== 方案4: jieba ==========\n");
        System.out.println("依赖: com.huaban:jieba-analysis:1.0.2 (~2MB)");
        System.out.println();
        System.out.println("注意: jieba 是分词库，不直接支持句子分割");
        System.out.println("仍需自行处理句子边界，但可结合分词结果优化");
        System.out.println();
        System.out.println("优点: 轻量，分词效果好");
        System.out.println("缺点: 不提供句子分割功能");
        System.out.println("适用场景: 需要分词但不一定要句子分割");
    }

    /**
     * 推荐方案: 结合自定义规则 + LangChain4j overlap
     */
    @Test
    @DisplayName("推荐方案: 自定义中文句子分割器 + LangChain4j")
    void testRecommendedApproach() {
        System.out.println("\n========== 推荐方案 ==========\n");
        System.out.println("实现思路:");
        System.out.println("1. 使用自定义规则分割中文句子（简单高效）");
        System.out.println("2. 将分割结果作为 LangChain4j DocumentBySentenceSplitter 的输入");
        System.out.println("3. 或直接实现字符 overlap（更简单）");
        System.out.println();
        System.out.println("核心代码:");
        System.out.println("""
                // 中文句子分割（保留标点）
                public static List<String> splitChineseSentences(String text) {
                    List<String> sentences = new ArrayList<>();
                    int start = 0;
                    for (int i = 0; i < text.length(); i++) {
                        char c = text.charAt(i);
                        if (c == '。' || c == '！' || c == '？' || c == '；') {
                            sentences.add(text.substring(start, i + 1));
                            start = i + 1;
                        }
                    }
                    // 处理剩余内容
                    if (start < text.length()) {
                        sentences.add(text.substring(start));
                    }
                    return sentences;
                }
                """);
    }

    /**
     * 自定义规则句子分割实现
     */
    private List<String> splitByCustomRule(String text, String[] endPunctuations) {
        List<String> sentences = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            for (String punct : endPunctuations) {
                if (c == punct.charAt(0)) {
                    // 找到句子结束标点，截取句子（包含标点）
                    sentences.add(text.substring(start, i + 1));
                    start = i + 1;
                    break;
                }
            }
        }

        // 处理最后没有标点的部分
        if (start < text.length()) {
            String remaining = text.substring(start).trim();
            if (!remaining.isEmpty()) {
                sentences.add(remaining);
            }
        }

        return sentences;
    }

    /**
     * 完整的中文文档分块器（带 overlap）
     */
    @Test
    @DisplayName("完整方案: 中文文档分块器实现")
    void testChineseDocumentSplitter() {
        System.out.println("\n========== 完整方案演示 ==========\n");

        String text = "这是第一句话。这是第二句话。这是第三句话！这是第四句话？这是第五句话。";
        int chunkSize = 20; // 字符
        int overlap = 8;

        List<String> chunks = splitChineseWithOverlap(text, chunkSize, overlap);

        System.out.println("配置: chunkSize=" + chunkSize + ", overlap=" + overlap);
        System.out.println("分块数: " + chunks.size());
        System.out.println();

        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("分块 " + i + ": \"" + chunks.get(i) + "\"");
            if (i > 0) {
                String prev = chunks.get(i - 1);
                String curr = chunks.get(i);
                // 检查 overlap
                String expectedOverlap = prev.substring(Math.max(0, prev.length() - overlap));
                if (curr.startsWith(expectedOverlap)) {
                    System.out.println("  ✓ 重叠: \"" + expectedOverlap + "\"");
                }
            }
        }

        System.out.println("\n结论: 自定义方案足够简单，无需引入额外依赖");
    }

    /**
     * 中文文档分块 + overlap
     */
    private List<String> splitChineseWithOverlap(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // 尝试在句子边界结束（可选优化）
            if (end < text.length()) {
                // 向后找句子结束标点
                for (int i = end; i < Math.min(end + 5, text.length()); i++) {
                    char c = text.charAt(i);
                    if (c == '。' || c == '！' || c == '？') {
                        end = i + 1;
                        break;
                    }
                }
            }

            chunks.add(text.substring(start, end));
            start = end - overlap;
            if (start < 0) start = 0;

            if (end >= text.length()) break;
        }

        return chunks;
    }
}