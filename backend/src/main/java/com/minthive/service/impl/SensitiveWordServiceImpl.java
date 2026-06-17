package com.minthive.service.impl;

import com.minthive.service.SensitiveWordService;
import com.minthive.util.SensitiveWordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * 敏感词服务实现
 */
@RequiredArgsConstructor
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordUtil sensitiveWordUtil;

    /**
     * 检测文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return true 包含
     */
    @Override
    public boolean contains(String text) {
        return sensitiveWordUtil.contains(text);
    }

    /**
     * 替换文本中的敏感词
     *
     * @param text 原始文本
     * @return 替换后文本
     */
    @Override
    public String replace(String text) {
        return sensitiveWordUtil.replace(text);
    }

    /**
     * 批量导入敏感词
     *
     * @param file 敏感词文件
     * @return 导入数量
     */
    @Override
    public int importWords(MultipartFile file) {
        // 占位实现：实际应写入外部敏感词文件并重新加载 DFA 树
        Set<String> words = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    words.add(line);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("敏感词导入失败", e);
        }
        return words.size();
    }
}
