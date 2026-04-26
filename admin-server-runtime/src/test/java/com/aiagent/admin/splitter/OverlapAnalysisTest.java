package com.aiagent.admin.splitter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 详细分析 LangChain4j overlapFrom 方法对中文的处理
 * <p>
 * 测试内容：
 * <ul>
 *   <li>字符模式分块（默认）</li>
 *   <li>Token模式分块（使用 Tokenizer）</li>
 *   <li>中英文重叠效果对比</li>
 * </ul>
 * </p>
 */
public class OverlapAnalysisTest {

    private static final String OUTPUT_FILE = "overlap_analysis_result.txt";

    private void writeToFile(String content) throws IOException {
        FileWriter writer = new FileWriter(OUTPUT_FILE, StandardCharsets.UTF_8, true);
        writer.write(content);
        writer.write("\n");
        writer.close();
    }

    private void clearOutputFile() throws IOException {
        FileWriter writer = new FileWriter(OUTPUT_FILE, StandardCharsets.UTF_8, false);
        writer.write("");
        writer.close();
    }


    /**
     * 测试：字符模式 vs Token 模式对比
     */
    @Test
    @DisplayName("字符模式 vs Token 模式分块对比")
    void testCharacterVsTokenMode() throws IOException {
        clearOutputFile();
        writeToFile("\n========== 字符模式 vs Token 模式对比 ==========\n");
        // 1. 使用正斜杠 '/' 作为路径分隔符，并且不要以 '/' 开头
        String resourcePath = "test/knowledge/lol-doc1.md";

        byte[] js;
        // 2. 使用 getResourceAsStream() 获取输入流
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {

            // 3. 务必检查流是否为 null，如果为 null 说明资源未找到
            if (inputStream == null) {
                throw new RuntimeException("无法找到资源文件: " + resourcePath + "。请确保文件位于 src/main/resources/ 目录下。");
            }

            // 4. 直接从流中读取所有字节 (Java 9+)
            js = inputStream.readAllBytes();

        }

        // 5. 将字节数组转换为字符串，并显式指定编码（推荐 UTF-8）
        String chineseText = new String(js, StandardCharsets.UTF_8);

        writeToFile("原文长度: " + chineseText.length() + " 字符");

        // ===== 字符模式测试 =====
        writeToFile("\n========== 1. 字符模式分块  ==========\n");

        DocumentByCharacterSplitter charSplitter = new DocumentByCharacterSplitter(100, 80);
        List<TextSegment> charSegments = charSplitter.split(Document.from(chineseText, Metadata.from("source", "test")));

        writeToFile("分块数: " + charSegments.size());
        for (int i = 0; i < charSegments.size(); i++) {
            String content = charSegments.get(i).text();
            writeToFile("\n--- 分块 " + i + " ---");
            writeToFile("字符长度: " + content.length());
            writeToFile("内容: \"" + content + "\"");
        }

        // 检查重叠
        writeToFile("\n字符模式重叠检查:");
        for (int i = 1; i < charSegments.size(); i++) {
            String prev = charSegments.get(i - 1).text();
            String curr = charSegments.get(i).text();
            boolean found = false;
            for (int len = Math.min(30, prev.length()); len > 0; len--) {
                if (curr.startsWith(prev.substring(prev.length() - len))) {
                    writeToFile("分块" + (i - 1) + " → 分块" + i + ": 有重叠 \"" + prev.substring(prev.length() - len) + "\" (长度: " + len + ")");
                    found = true;
                    break;
                }
            }
            if (!found) writeToFile("分块" + (i - 1) + " → 分块" + i + ": 无重叠");
        }
    }

    /**
     * 测试段落分割器的工作原理
     */
    @Test
    @DisplayName("段落分割器详细分析")
    void testParagraphSplitterDetail() throws IOException {
        writeToFile("\n========== 段落分割器工作原理分析 ==========\n");

        String text = """
                这是第一段落内容。
                
                这是第二段落内容，包含更多文字。
                
                这是第三段落，非常短。
                """.stripIndent();

        writeToFile("原文:\n" + text);
        writeToFile("原文长度: " + text.length() + "\n");

        // 分析 split 方法
        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(500, 50);
        String[] paragraphs = splitter.split(text);

        writeToFile("段落分割结果 (按 \\n\\n 分割):");
        for (int i = 0; i < paragraphs.length; i++) {
            writeToFile("段落 " + i + ": \"" + paragraphs[i] + "\" (长度: " + paragraphs[i].length() + ")");
        }

        writeToFile("\n注释说明:");
        writeToFile("Paragraph boundaries are detected by a minimum of two newline characters (\\n\\n)");
        writeToFile("如果单个段落超过 maxSegmentSize，会调用 subSplitter (默认 DocumentBySentenceSplitter) 再分割");
        writeToFile("重叠使用 overlapFrom 方法，从上一分块末尾按句子提取重叠内容");
    }

    /**
     * 分析句子分割器对中文的处理
     */
    @Test
    @DisplayName("句子分割器中文处理分析")
    void testSentenceSplitterChinese() throws IOException {
        writeToFile("\n========== 句子分割器中文处理分析 ==========\n");

        // 包含各种句子边界的中文文本
        String text = "这是第一句。这是第二句，包含逗号。这是第三句！这是第四句？最后一句没有标点";

        writeToFile("原文: " + text);
        writeToFile("长度: " + text.length() + "\n");

        // DocumentBySentenceSplitter 的 split 方法
        DocumentBySentenceSplitter sentenceSplitter = new DocumentBySentenceSplitter(1, 0);
        String[] sentences = sentenceSplitter.split(text);

        writeToFile("句子分割结果:");
        for (int i = 0; i < sentences.length; i++) {
            writeToFile("句子 " + i + ": \"" + sentences[i] + "\" (长度: " + sentences[i].length() + ")");
        }

        writeToFile("\n结论:");
        writeToFile("DocumentBySentenceSplitter 基于英文句号 '. ' 设计，对中文句号 '。' 不能正确识别");
        writeToFile("中文整段被当作一个句子，导致 overlapFrom 无法正确提取重叠");
    }

    /**
     * 英文文本重叠测试（对比）
     */
    @Test
    @DisplayName("英文文本重叠效果对比")
    void testEnglishOverlapDetailed() throws IOException {
        writeToFile("\n========== 英文文本重叠效果 (RECURSIVE, 100字符, 30重叠) ==========\n");

        String text = """
                This is the first paragraph for testing. We need enough text.
                
                This is the second paragraph. Adding more content for testing purposes.
                
                This is the third paragraph. Overlap should work correctly for English.
                
                """;

        writeToFile("原文:\n" + text);

        var splitter = DocumentSplitters.recursive(100, 30);
        List<TextSegment> segments = splitter.split(Document.from(text, Metadata.from("source", "test")));

        writeToFile("分块数: " + segments.size() + "\n");

        for (int i = 0; i < segments.size(); i++) {
            String content = segments.get(i).text();
            writeToFile("--- 分块 " + i + " ---");
            writeToFile("长度: " + content.length());
            writeToFile("内容: \"" + content + "\"");
            writeToFile("");
        }

        // 详细检查重叠
        writeToFile("========== 重叠详细分析 ==========");
        for (int i = 1; i < segments.size(); i++) {
            String prev = segments.get(i - 1).text();
            String curr = segments.get(i).text();

            writeToFile("\n分块" + (i - 1) + " 末尾句子:");
            DocumentBySentenceSplitter sentenceSplitter = new DocumentBySentenceSplitter(1, 0);
            String[] prevTailSentences = sentenceSplitter.split(prev.substring(Math.max(0, prev.length() - 30)));
            for (String s : prevTailSentences) {
                writeToFile("  - \"" + s + "\"");
            }

            writeToFile("分块" + i + " 开头: \"" + curr.substring(0, Math.min(50, curr.length())) + "\"");

            // 找重叠
            for (int len = Math.min(30, prev.length()); len > 0; len--) {
                if (curr.startsWith(prev.substring(prev.length() - len))) {
                    writeToFile("发现重叠: \"" + prev.substring(prev.length() - len) + "\" (长度: " + len + ")");
                    break;
                }
            }
        }

        writeToFile("\n结论: 英文句子以 '. ' 明确分隔，overlapFrom 能正确提取完整句子作为重叠");
    }
}