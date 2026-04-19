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
 * LangChain4j 1.13.0 版本中文 overlap 效果验证测试
 * <p>
 * 验证升级后各分块器对中文文本的重叠处理能力。
 * </p>
 */
public class LangChain4jV113OverlapTest {

    private static final String OUTPUT_FILE = "langchain4j_v113_overlap_result.txt";

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
     * 测试 1: Character 模式分块 - 基于字符数的简单分块
     * <p>
     * 理论上应该按字符数截取，overlap 也应该是简单的字符截取。
     * </p>
     */
    @Test
    @DisplayName("Character 模式分块 (100字符, 30重叠)")
    void testCharacterSplitterOverlap() throws IOException {
        clearOutputFile();
        writeToFile("\n========== LangChain4j 1.13.0 - Character 模式分块 ==========\n");
        writeToFile("配置: maxSegmentSize=100, maxOverlapSize=30\n\n");

        String chineseText = loadChineseText();

        DocumentByCharacterSplitter splitter = new DocumentByCharacterSplitter(100, 30);
        List<TextSegment> segments = splitter.split(Document.from(chineseText, Metadata.from("source", "test")));

        writeToFile("分块数: " + segments.size());
        analyzeOverlap(segments);
    }

    /**
     * 测试 2: Sentence 模式分块 - 新版本使用 OpenNLP
     * <p>
     * LangChain4j 1.x 使用 OpenNLP SentenceDetector 进行句子分割，
     * 默认使用英文模型，验证对中文的处理效果。
     * </p>
     */
    @Test
    @DisplayName("Sentence 模式分块 (200字符, 50重叠)")
    void testSentenceSplitterOverlap() throws IOException {
        writeToFile("\n========== LangChain4j 1.13.0 - Sentence 模式分块 ==========\n");
        writeToFile("配置: maxSegmentSize=200, maxOverlapSize=50\n");
        writeToFile("注意: 默认使用 OpenNLP 英文模型，对中文可能仍有问题\n\n");

        String chineseText = loadChineseText();

        DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(200, 50);
        List<TextSegment> segments = splitter.split(Document.from(chineseText, Metadata.from("source", "test")));

        writeToFile("分块数: " + segments.size());
        analyzeOverlap(segments);

        // 验证句子分割效果
        writeToFile("\n--- 句子分割验证 ---");
        String[] sentences = splitter.split(chineseText.substring(0, Math.min(200, chineseText.length())));
        for (int i = 0; i < sentences.length; i++) {
            writeToFile("句子 " + i + ": \"" + sentences[i] + "\"");
        }
    }

    /**
     * 测试 3: Paragraph 模式分块
     */
    @Test
    @DisplayName("Paragraph 模式分块 (300字符, 80重叠)")
    void testParagraphSplitterOverlap() throws IOException {
        writeToFile("\n========== LangChain4j 1.13.0 - Paragraph 模式分块 ==========\n");
        writeToFile("配置: maxSegmentSize=300, maxOverlapSize=80\n\n");

        String chineseText = loadChineseText();

        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(300, 80);
        List<TextSegment> segments = splitter.split(Document.from(chineseText, Metadata.from("source", "test")));

        writeToFile("分块数: " + segments.size());
        analyzeOverlap(segments);
    }

    /**
     * 测试 4: Recursive 模式 - 推荐的通用分块方式
     */
    @Test
    @DisplayName("Recursive 模式分块 (150字符, 40重叠)")
    void testRecursiveSplitterOverlap() throws IOException {
        writeToFile("\n========== LangChain4j 1.13.0 - Recursive 模式分块 ==========\n");
        writeToFile("配置: maxSegmentSize=150, maxOverlapSize=40\n\n");

        String chineseText = loadChineseText();

        var splitter = DocumentSplitters.recursive(150, 40);
        List<TextSegment> segments = splitter.split(Document.from(chineseText, Metadata.from("source", "test")));

        writeToFile("分块数: " + segments.size());
        analyzeOverlap(segments);
    }

    /**
     * 测试 5: 英文文本对比 - 验证英文文本 overlap 是否正常
     */
    @Test
    @DisplayName("英文文本 Recursive 分块 (对比)")
    void testEnglishRecursiveOverlap() throws IOException {
        writeToFile("\n========== 英文文本对比测试 ==========\n");
        writeToFile("配置: maxSegmentSize=150, maxOverlapSize=40\n\n");

        String englishText = """
                This is the first paragraph. It contains multiple sentences to test the overlap functionality.
                We need to ensure that the overlap parameter works correctly for English text.
                
                This is the second paragraph. LangChain4j should be able to detect sentence boundaries
                using the period followed by space pattern. Overlap should preserve complete sentences.
                
                This is the third paragraph. The overlap mechanism should take sentences from the end
                of one segment and prepend them to the next segment. Let's verify this behavior.
                """.stripIndent();

        var splitter = DocumentSplitters.recursive(150, 40);
        List<TextSegment> segments = splitter.split(Document.from(englishText, Metadata.from("source", "test")));

        writeToFile("英文分块数: " + segments.size());
        analyzeOverlap(segments);
    }

    /**
     * 测试 6: 自定义实现 - 基于字符数的简单 overlap
     * <p>
     * 作为对比参考，展示如何正确实现中文 overlap。
     * </p>
     */
    @Test
    @DisplayName("自定义字符 overlap 实现")
    void testCustomCharacterOverlap() throws IOException {
        writeToFile("\n========== 自定义字符 overlap 实现 ==========\n");
        writeToFile("配置: chunkSize=100, overlap=30 (手动实现)\n\n");

        String chineseText = loadChineseText();
        int chunkSize = 100;
        int overlap = 30;

        List<String> customSegments = splitWithCharacterOverlap(chineseText, chunkSize, overlap);

        writeToFile("分块数: " + customSegments.size());
        for (int i = 0; i < customSegments.size(); i++) {
            String content = customSegments.get(i);
            writeToFile("\n--- 分块 " + i + " ---");
            writeToFile("长度: " + content.length());
            writeToFile("内容: \"" + content + "\"");
        }

        // 验证重叠
        writeToFile("\n自定义 overlap 检查:");
        for (int i = 1; i < customSegments.size(); i++) {
            String prev = customSegments.get(i - 1);
            String curr = customSegments.get(i);
            // 验证当前分块开头是否与前一分块末尾重叠
            String expectedOverlap = prev.substring(Math.max(0, prev.length() - overlap));
            if (curr.startsWith(expectedOverlap)) {
                writeToFile("分块" + (i - 1) + " → 分块" + i + ": ✓ 有重叠 \"" + expectedOverlap + "\" (长度: " + expectedOverlap.length() + ")");
            } else {
                // 可能最后一个分块不满 overlap
                writeToFile("分块" + (i - 1) + " → 分块" + i + ": 部分重叠或文档末尾");
            }
        }
    }

    /**
     * 自定义基于字符数的分块 + overlap 实现
     */
    private List<String> splitWithCharacterOverlap(String text, int chunkSize, int overlap) {
        List<String> segments = new java.util.ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            segments.add(text.substring(start, end));

            // 下一个分块起点 = 当前终点 - overlap（实现重叠）
            start = end - overlap;
            if (start < 0) start = 0;

            // 避免无限循环：如果 overlap >= chunkSize，强制前进
            if (end == text.length()) break;
        }

        return segments;
    }

    /**
     * 加载中文测试文本
     */
    private String loadChineseText() throws IOException {
        String resourcePath = "test/knowledge/lol-doc1.md";
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("无法找到资源文件: " + resourcePath);
            }
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * 分析分块间的重叠情况
     */
    private void analyzeOverlap(List<TextSegment> segments) throws IOException {
        writeToFile("\n========== 重叠分析 ==========\n");

        int overlapCount = 0;
        for (int i = 1; i < segments.size(); i++) {
            String prev = segments.get(i - 1).text();
            String curr = segments.get(i).text();

            writeToFile("\n分块" + (i - 1) + " (长度: " + prev.length() + ") → 分块" + i + " (长度: " + curr.length() + ")");

            // 尝试找到重叠内容
            int maxCheckLen = Math.min(50, prev.length());
            boolean found = false;
            for (int len = maxCheckLen; len > 0; len--) {
                String tail = prev.substring(prev.length() - len);
                if (curr.startsWith(tail)) {
                    writeToFile("发现重叠: \"" + tail + "\" (长度: " + len + ")");
                    found = true;
                    overlapCount++;
                    break;
                }
            }
            if (!found) {
                writeToFile("无重叠");
            }
        }

        writeToFile("\n总结: " + overlapCount + "/" + (segments.size() - 1) + " 分块对有重叠");

        // 打印前几个分块详情
        writeToFile("\n========== 分块详情 (前5个) ==========\n");
        for (int i = 0; i < Math.min(5, segments.size()); i++) {
            String content = segments.get(i).text();
            writeToFile("分块 " + i + ":");
            writeToFile("  长度: " + content.length());
            writeToFile("  开头: \"" + content.substring(0, Math.min(30, content.length())) + "\"");
            writeToFile("  末尾: \"" + content.substring(Math.max(0, content.length() - 30)) + "\"");
        }
    }
}