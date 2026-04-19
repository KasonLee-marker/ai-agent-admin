package com.aiagent.admin.splitter;

import com.hankcs.hanlp.utility.SentencesUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HanLP 分块策略集成验证测试
 * <p>
 * 验证所有分块策略（FIXED_SIZE、PARAGRAPH、SENTENCE、RECURSIVE）
 * 使用 HanLP 后的 overlap 效果。
 * </p>
 */
public class HanLPAllStrategiesOverlapTest {

    /**
     * 模拟 DocumentAsyncService 的分块方法
     */

    private List<String> splitBySentenceWithOverlap(List<String> sentences, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        String overlapContent = "";

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(overlapContent + currentChunk.toString().trim());
                overlapContent = getOverlapFromChunk(overlapContent + currentChunk.toString(), overlap);
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }

        if (currentChunk.length() > 0) {
            chunks.add(overlapContent + currentChunk.toString().trim());
        }

        return chunks;
    }

    private List<String> splitByParagraphWithHanLP(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");
        StringBuilder currentChunk = new StringBuilder();
        String overlapContent = "";

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            if (paragraph.length() > chunkSize) {
                List<String> paragraphSentences = SentencesUtil.toSentenceList(paragraph);
                for (String sentence : paragraphSentences) {
                    sentence = sentence.trim();
                    if (sentence.isEmpty()) continue;

                    if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                        chunks.add(overlapContent + currentChunk.toString().trim());
                        overlapContent = getOverlapFromChunk(overlapContent + currentChunk.toString(), overlap);
                        currentChunk = new StringBuilder();
                    }
                    currentChunk.append(sentence).append(" ");
                }
            } else {
                if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                    chunks.add(overlapContent + currentChunk.toString().trim());
                    overlapContent = getOverlapFromChunk(overlapContent + currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder();
                }
                currentChunk.append(paragraph).append(" ");
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(overlapContent + currentChunk.toString().trim());
        }

        return chunks;
    }

    private String getOverlapFromChunk(String chunk, int overlapSize) {
        if (chunk == null || chunk.length() <= overlapSize) {
            return chunk != null ? chunk : "";
        }
        return chunk.substring(chunk.length() - overlapSize);
    }

    /**
     * 测试 SENTENCE 策略 overlap
     */
    @Test
    @DisplayName("SENTENCE 策略 overlap 测试")
    void testSentenceStrategyOverlap() {
        System.out.println("\n========== SENTENCE 策略 overlap 测试 ==========\n");

        String text = "这是第一句话。这是第二句话。这是第三句话！这是第四句话？这是第五句话，包含逗号。这是第六句话。这是第七句话。这是第八句话。";
        int chunkSize = 30;
        int overlap = 15;

        System.out.println("原文: " + text);
        System.out.println("配置: chunkSize=" + chunkSize + ", overlap=" + overlap);

        List<String> sentences = SentencesUtil.toSentenceList(text);
        sentences = sentences.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        System.out.println("HanLP 句子分割: " + sentences.size() + " 个句子");

        List<String> chunks = splitBySentenceWithOverlap(sentences, chunkSize, overlap);

        System.out.println("\n分块结果: " + chunks.size() + " 个分块");
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("  分块 " + i + ": \"" + chunks.get(i) + "\" (长度: " + chunks.get(i).length() + ")");
        }

        // 验证 overlap
        int overlapCount = verifyOverlap(chunks, overlap);
        System.out.println("\nOverlap: " + overlapCount + "/" + (chunks.size() - 1) + " 分块对有重叠");
        assert overlapCount > 0 : "应该有 overlap";
        System.out.println("✓ 测试通过");
    }

    /**
     * 测试 FIXED_SIZE 策略 overlap（实际也用句子分割）
     */
    @Test
    @DisplayName("FIXED_SIZE 策略 overlap 测试")
    void testFixedSizeStrategyOverlap() {
        System.out.println("\n========== FIXED_SIZE 策略 overlap 测试 ==========\n");

        String text = "这是第一句话。这是第二句话。这是第三句话！这是第四句话？这是第五句话，包含逗号。这是第六句话。这是第七句话。这是第八句话。这是第九句话。这是第十句话。";
        int chunkSize = 50;
        int overlap = 20;

        System.out.println("原文: " + text);
        System.out.println("配置: chunkSize=" + chunkSize + ", overlap=" + overlap);

        List<String> sentences = SentencesUtil.toSentenceList(text);
        sentences = sentences.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        // FIXED_SIZE 也用句子分割
        List<String> chunks = splitBySentenceWithOverlap(sentences, chunkSize, overlap);

        System.out.println("\n分块结果: " + chunks.size() + " 个分块");
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("  分块 " + i + ": \"" + chunks.get(i) + "\" (长度: " + chunks.get(i).length() + ")");
        }

        int overlapCount = verifyOverlap(chunks, overlap);
        System.out.println("\nOverlap: " + overlapCount + "/" + (chunks.size() - 1) + " 分块对有重叠");
        assert overlapCount > 0 : "应该有 overlap";
        System.out.println("✓ 测试通过");
    }

    /**
     * 测试 PARAGRAPH 策略 overlap
     */
    @Test
    @DisplayName("PARAGRAPH 策略 overlap 测试")
    void testParagraphStrategyOverlap() {
        System.out.println("\n========== PARAGRAPH 策略 overlap 测试 ==========\n");

        String text = "这是第一段内容，比较长一些。包含多句话。\n\n这是第二段，也包含内容。\n\n这是第三段，测试段落分割效果。最后结束。";
        int chunkSize = 30;
        int overlap = 15;

        System.out.println("原文: " + text);
        System.out.println("配置: chunkSize=" + chunkSize + ", overlap=" + overlap);

        List<String> chunks = splitByParagraphWithHanLP(text, chunkSize, overlap);

        System.out.println("\n分块结果: " + chunks.size() + " 个分块");
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("  分块 " + i + ": \"" + chunks.get(i) + "\" (长度: " + chunks.get(i).length() + ")");
        }

        int overlapCount = verifyOverlap(chunks, overlap);
        System.out.println("\nOverlap: " + overlapCount + "/" + (chunks.size() - 1) + " 分块对有重叠");
        assert overlapCount > 0 || chunks.size() <= 1 : "应该有 overlap 或只有一个分块";
        System.out.println("✓ 测试通过");
    }

    /**
     * 验证分块间的 overlap
     */
    private int verifyOverlap(List<String> chunks, int overlapSize) {
        int overlapCount = 0;
        for (int i = 1; i < chunks.size(); i++) {
            String prev = chunks.get(i - 1);
            String curr = chunks.get(i);

            for (int len = Math.min(overlapSize, prev.length()); len > 0; len--) {
                String tail = prev.substring(prev.length() - len);
                if (curr.startsWith(tail)) {
                    System.out.println("  分块" + (i - 1) + " → 分块" + i + ": overlap \"" + tail + "\" (长度: " + len + ")");
                    overlapCount++;
                    break;
                }
            }
        }
        return overlapCount;
    }
}