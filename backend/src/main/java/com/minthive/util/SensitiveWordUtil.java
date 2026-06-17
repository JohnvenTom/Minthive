package com.minthive.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 敏感词过滤工具类(DFA 算法)
 *
 * <p>功能描述：本地加载敏感词库，构建 DFA 词库树，提供文本敏感词检测与替换</p>
 * <p>注意事项：敏感词库文件支持 classpath 与外部路径，外部路径优先</p>
 */
@Slf4j
@Component
public class SensitiveWordUtil {

    /** DFA 词库根节点 */
    private final Map<Character, Object> sensitiveWordMap = new HashMap<>();

    /** 敏感词结束标记 */
    private static final String IS_END = "isEnd";

    /** 结束: 1 */
    private static final String END_YES = "1";
    /** 未结束: 0 */
    private static final String END_NO = "0";

    @Value("${sensitive.word.path}")
    private String wordPath;

    @Value("${sensitive.word.external-path}")
    private String externalPath;

    /**
     * 初始化：加载敏感词库并构建 DFA 树
     */
    @PostConstruct
    public void init() {
        Set<String> words = loadWords();
        buildDfaTree(words);
        log.info("敏感词库加载完成，共 {} 个敏感词", words.size());
    }

    /**
     * 加载敏感词集合
     *
     * @return 敏感词集合
     */
    private Set<String> loadWords() {
        Set<String> words = new HashSet<>();
        // 优先外部路径
        if (StringUtils.hasText(externalPath)) {
            File externalFile = new File(externalPath);
            if (externalFile.exists() && externalFile.isFile()) {
                loadFromFile(words, externalFile.getAbsolutePath(), false);
            }
        }
        // classpath 兜底
        if (words.isEmpty()) {
            try {
                ClassPathResource resource = new ClassPathResource("sensitive-words.txt");
                try (InputStream is = resource.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (StringUtils.hasText(line)) {
                            words.add(line);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("加载 classpath 敏感词库失败: {}", e.getMessage());
            }
        }
        return words;
    }

    /**
     * 从文件路径加载敏感词
     *
     * @param words    敏感词集合
     * @param path     文件路径
     * @param classpath 是否 classpath
     */
    private void loadFromFile(Set<String> words, String path, boolean classpath) {
        try (InputStream is = classpath ? new ClassPathResource(path).getInputStream() : new FileInputStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (StringUtils.hasText(line)) {
                    words.add(line);
                }
            }
        } catch (Exception e) {
            log.warn("加载敏感词库失败: path={}, err={}", path, e.getMessage());
        }
    }

    /**
     * 构建 DFA 词库树
     *
     * @param words 敏感词集合
     */
    @SuppressWarnings("unchecked")
    private void buildDfaTree(Set<String> words) {
        for (String word : words) {
            Map<Character, Object> current = sensitiveWordMap;
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                Object next = current.get(ch);
                Map<Character, Object> nextMap;
                if (next instanceof Map) {
                    nextMap = (Map<Character, Object>) next;
                } else {
                    nextMap = new HashMap<>();
                    nextMap.put(IS_END.charAt(0), END_NO.charAt(0));
                    current.put(ch, nextMap);
                }
                current = nextMap;
                if (i == word.length() - 1) {
                    current.put(IS_END.charAt(0), END_YES.charAt(0));
                }
            }
        }
    }

    /**
     * 检测文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return true 包含 / false 不包含
     */
    public boolean contains(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (checkSensitiveWord(text, i) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 替换文本中的敏感词为 *
     *
     * @param text 原始文本
     * @return 替换后文本
     */
    public String replace(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        StringBuilder result = new StringBuilder(text);
        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                for (int j = i; j < i + length && j < result.length(); j++) {
                    result.setCharAt(j, '*');
                }
                i = i + length - 1;
            }
        }
        return result.toString();
    }

    /**
     * 从指定位置开始检测敏感词
     *
     * @param text       文本
     * @param beginIndex 起始位置
     * @return 敏感词长度(0表示未命中)
     */
    @SuppressWarnings("unchecked")
    private int checkSensitiveWord(String text, int beginIndex) {
        Map<Character, Object> current = sensitiveWordMap;
        int length = 0;
        boolean isEnd = false;
        for (int i = beginIndex; i < text.length(); i++) {
            char ch = text.charAt(i);
            Object next = current.get(ch);
            if (next == null) {
                break;
            }
            length++;
            current = (Map<Character, Object>) next;
            if (END_YES.charAt(0) == current.get(IS_END.charAt(0))) {
                isEnd = true;
                break;
            }
        }
        return isEnd ? length : 0;
    }
}
