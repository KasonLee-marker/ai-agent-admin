# 中文 NLP 库调研报告

## 调研目的

为知识库语义分割功能选择合适的中文 NLP 库。

## 方案对比

### 1. HanLP（推荐）

| 项目           | 详情                                |
|--------------|-----------------------------------|
| **Maven 坐标** | `com.hankcs:hanlp:portable-1.8.6` |
| **Jar 大小**   | ~8MB                              |
| **GitHub**   | https://github.com/hankcs/HanLP   |
| **维护状态**     | 活跃（2024+ 更新）                      |
| **功能**       | 分词、词性标注、命名实体识别、依存句法分析、句子分割        |

**版本选择**：

- `portable` 版：轻量版，内置模型，无需额外下载
- `full` 版：完整版，需下载模型文件（~1GB）

**句子分割 API**：

```java
import com.hankcs.hanlp.HanLP;

import java.util.List;

// 句子分割
List<String> sentences = HanLP.extractSentence(text);

        // 分词
        List<String> words = HanLP.segment(text);

        // 词性标注
        List<Term> terms = HanLP.segment(text); // 包含词性
```

**语义分割能力**：

- HanLP 支持**依存句法分析**，可用于语义理解
- 但**语义分割**（基于语义边界分割段落）需要自行实现
- 可结合句子分割 + 语义相似度计算实现语义分割

---

### 2. jieba-analysis

| 项目           | 详情                                       |
|--------------|------------------------------------------|
| **Maven 坐标** | `com.huaban:jieba-analysis:1.0.2`        |
| **Jar 大小**   | ~2MB                                     |
| **GitHub**   | https://github.com/huaban/jieba-analysis |
| **维护状态**     | 较少更新                                     |
| **功能**       | 分词（仅分词，无句子分割）                            |

**局限**：

- 不提供句子分割功能
- 主要用于分词，不适合语义分割

---

### 3. ansj_seg

| 项目           | 详情                               |
|--------------|----------------------------------|
| **Maven 坐标** | `org.ansj:ansj_seg:5.1.6`        |
| **Jar 大小**   | ~4MB                             |
| **GitHub**   | https://github.com/ansj/ansj_seg |
| **维护状态**     | 较少更新                             |
| **功能**       | 分词、词性标注                          |

**局限**：

- 无句子分割 API
- 无语义分析功能

---

### 4. LTP (Language Technology Platform)

| 项目          | 详情                           |
|-------------|------------------------------|
| **来源**      | 哈工大社会计算与信息检索研究中心             |
| **Java 版本** | ltp4j（不在 Maven Central）      |
| **功能**      | 分词、词性标注、命名实体识别、依存句法分析、语义角色标注 |

**局限**：

- 需要本地编译安装
- 配置复杂，不推荐

---

## 推荐方案

### 方案 A：HanLP + 自定义语义分割

```xml

<dependency>
    <groupId>com.hankcs</groupId>
    <artifactId>hanlp</artifactId>
    <version>portable-1.8.6</version>
</dependency>
```

**实现思路**：

1. 使用 `HanLP.extractSentence()` 分割句子
2. 对句子计算 Embedding 向量
3. 相邻句子相似度低于阈值时，作为语义边界分割段落

**优点**：

- 句子分割准确
- 可结合语义相似度实现真正的语义分割

---

### 方案 B：纯自定义实现（当前方案）

无依赖，基于中文标点分割句子。

**优点**：

- 无依赖，轻量
- 足够用于 RAG 分块

**局限**：

- 不能处理复杂语法（如引号内的句号）
- 无语义理解能力

---

## 语义分割实现方案

如果需要**真正的语义分割**（基于语义边界，而非句子边界）：

```
文本 → HanLP句子分割 → 每句计算Embedding → 相邻句子相似度 → 
相似度低于阈值 = 语义边界 → 形成语义段落
```

**示例代码**：

```java
public List<String> semanticSplit(String text, double threshold) {
    List<String> sentences = HanLP.extractSentence(text);
    List<String> paragraphs = new ArrayList<>();
    StringBuilder currentPara = new StringBuilder();

    for (int i = 0; i < sentences.size(); i++) {
        String sentence = sentences.get(i);
        currentPara.append(sentence);

        // 检查与下一句的语义相似度
        if (i < sentences.size() - 1) {
            double similarity = embeddingSimilarity(sentence, sentences.get(i + 1));
            if (similarity < threshold) {
                // 语义边界，分割段落
                paragraphs.add(currentPara.toString());
                currentPara = new StringBuilder();
            }
        }
    }
    if (currentPara.length() > 0) {
        paragraphs.add(currentPara.toString());
    }
    return paragraphs;
}
```

## 结论

| 场景        | 推荐方案                  |
|-----------|-----------------------|
| 简单 RAG 分块 | 自定义规则（当前实现）           |
| 需要准确句子分割  | HanLP portable        |
| 需要语义分割    | HanLP + Embedding 相似度 |

**建议**：先用 HanLP portable 替代自定义规则，后续再实现语义分割。