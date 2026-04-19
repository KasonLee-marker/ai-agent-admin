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

import java.util.List;

/**
 * LangChain4j 分块器重叠行为测试
 * <p>
 * 验证不同分块策略对中文文本的重叠效果。
 * </p>
 */
public class LangChain4jSplitterOverlapTest {

    /**
     * 测试文本 - 包含多个段落，足够长以产生多个分块
     */
    private static final String TEST_TEXT = """
            这是第一段内容，用于测试分块器的重叠功能。我们需要确保这段文本足够长，才能产生多个分块来观察重叠效果。
            
            这是第二段内容，继续添加更多文字来确保文本足够长。重叠功能在RAG检索中非常重要，因为它可以确保相邻分块之间有共同的内容，避免信息在分块边界处丢失。
            
            这是第三段内容，我们继续添加文字。当用户查询的内容恰好跨越两个分块的边界时，重叠可以确保这部分内容在两个分块中都存在，提高检索的召回率。
            
            这是第四段内容，测试文本已经足够长了。我们希望看到相邻分块之间有重叠的内容，比如末尾50字符应该出现在下一个分块的开头。
            
            这是第五段，最后一段内容。用于验证分块器是否正确处理文档末尾。重叠测试到此结束。
            """.stripIndent();

    /**
     * 测试 RECURSIVE 策略 - 递归分块（段落→句子→字符）
     * <p>
     * DocumentSplitters.recursive 是最推荐的分块方式，
     * 它会先尝试按段落分割，如果段落过长则按句子分割，最后按字符分割。
     * </p>
     */
    @Test
    @DisplayName("RECURSIVE 策略分块测试")
    void testRecursiveSplitter() {
        System.out.println("\n========== RECURSIVE 策略 (chunkSize=200, overlap=50) ==========\n");

        var splitter = DocumentSplitters.recursive(200, 50);
        List<TextSegment> segments = splitter.split(Document.from(TEST_TEXT, Metadata.from("source", "test")));

        printSegments(segments, "RECURSIVE");
    }

    /**
     * 测试 FIXED_SIZE 策略 - 按字符分块
     */
    @Test
    @DisplayName("FIXED_SIZE 策略分块测试")
    void testCharacterSplitter() {
        System.out.println("\n========== FIXED_SIZE 策略 (chunkSize=200, overlap=50) ==========\n");

        var splitter = new DocumentByCharacterSplitter(200, 50);
        List<TextSegment> segments = splitter.split(Document.from(TEST_TEXT, Metadata.from("source", "test")));

        printSegments(segments, "FIXED_SIZE");
    }

    /**
     * 测试 SENTENCE 策略 - 按句子分块
     */
    @Test
    @DisplayName("SENTENCE 策略分块测试")
    void testSentenceSplitter() {
        System.out.println("\n========== SENTENCE 策略 (chunkSize=200, overlap=50) ==========\n");

        var splitter = new DocumentBySentenceSplitter(200, 50);
        List<TextSegment> segments = splitter.split(Document.from(TEST_TEXT, Metadata.from("source", "test")));

        printSegments(segments, "SENTENCE");
    }

    /**
     * 测试 PARAGRAPH 策略 - 按段落分块
     */
    @Test
    @DisplayName("PARAGRAPH 策略分块测试")
    void testParagraphSplitter() {
        System.out.println("\n========== PARAGRAPH 策略 (chunkSize=200, overlap=50) ==========\n");

        var splitter = new DocumentByParagraphSplitter(200, 50);
        List<TextSegment> segments = splitter.split(Document.from(TEST_TEXT, Metadata.from("source", "test")));

        printSegments(segments, "PARAGRAPH");
    }

    /**
     * 测试小 overlap 值的重叠效果
     */
    @Test
    @DisplayName("RECURSIVE 策略 - 小 overlap (10字符)")
    void testRecursiveSmallOverlap() {
        System.out.println("\n========== RECURSIVE 策略 (chunkSize=150, overlap=10) ==========\n");

        var splitter = DocumentSplitters.recursive(150, 10);
        List<TextSegment> segments = splitter.split(Document.from(TEST_TEXT, Metadata.from("source", "test")));

        printSegments(segments, "RECURVIVE_SMALL");
    }

    /**
     * 测试大 overlap 值的重叠效果
     */
    @Test
    @DisplayName("RECURSIVE 策略 - 大 overlap (100字符)")
    void testRecursiveLargeOverlap() {
        System.out.println("\n========== RECURSIVE 策略 (chunkSize=300, overlap=100) ==========\n");

        var splitter = DocumentSplitters.recursive(300, 100);
        List<TextSegment> segments = splitter.split(Document.from(TEST_TEXT, Metadata.from("source", "test")));

        printSegments(segments, "RECURVIVE_LARGE");
    }

    /**
     * 打印分块结果，检查重叠情况
     */
    private void printSegments(List<TextSegment> segments, String strategy) {
        System.out.println("总分块数: " + segments.size());
        System.out.println("原文总长度: " + TEST_TEXT.length() + " 字符\n");

        String previousOverlap = null;

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String content = segment.text();

            System.out.println("---------- 分块 " + i + " ----------");
            System.out.println("长度: " + content.length() + " 字符");

            // 显示分块内容（如果太长则截断显示）
            if (content.length() <= 100) {
                System.out.println("内容: " + content);
            } else {
                System.out.println("开头(50字): " + content.substring(0, 50) + "...");
                System.out.println("结尾(50字): ..." + content.substring(content.length() - 50));
            }

            // 检查与上一分块的重叠
            if (i > 0 && previousOverlap != null) {
                // 尝试找到重叠内容
                String overlapFound = findOverlap(previousOverlap, content);
                if (overlapFound != null && !overlapFound.isEmpty()) {
                    System.out.println("✓ 与上一分块的重叠内容: \"" + overlapFound + "\" (长度: " + overlapFound.length() + ")");
                } else {
                    System.out.println("✗ 没有找到与上一分块的重叠内容");
                }
            }

            // 记录当前分块末尾 50 字符，用于下一轮重叠检查
            previousOverlap = content.length() > 50 ? content.substring(content.length() - 50) : content;
            System.out.println();
        }

        // 总结重叠情况
        System.out.println("========== 重叠情况总结 ==========");
        int overlapCount = 0;
        for (int i = 1; i < segments.size(); i++) {
            String prevContent = segments.get(i - 1).text();
            String currContent = segments.get(i).text();
            String overlap = findOverlap(prevContent, currContent);
            if (overlap != null && overlap.length() > 0) {
                overlapCount++;
                System.out.println("分块" + (i - 1) + " → 分块" + i + ": 重叠 " + overlap.length() + " 字符");
            } else {
                System.out.println("分块" + (i - 1) + " → 分块" + i + ": 无重叠");
            }
        }
        System.out.println("\n有效重叠的分块对数: " + overlapCount + "/" + (segments.size() - 1));
    }

    /**
     * 查找两个分块之间的重叠内容
     * <p>
     * 从第一个分块的末尾开始，尝试在第二个分块的开头找到匹配的内容。
     * </p>
     */
    private String findOverlap(String previousContent, String currentContent) {
        // 从前一分块末尾提取最长可能的重叠（最多50字符）
        int maxOverlapLen = Math.min(50, previousContent.length());

        for (int len = maxOverlapLen; len > 0; len--) {
            String tail = previousContent.substring(previousContent.length() - len);
            if (currentContent.startsWith(tail)) {
                return tail;
            }
        }
        return null;
    }
}