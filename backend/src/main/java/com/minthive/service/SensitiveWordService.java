package com.minthive.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 敏感词服务接口
 *
 * <p>功能描述：提供敏感词检测、替换、批量导入导出能力</p>
 */
public interface SensitiveWordService {

    /**
     * 检测文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return true 包含
     */
    boolean contains(String text);

    /**
     * 替换文本中的敏感词
     *
     * @param text 原始文本
     * @return 替换后文本
     */
    String replace(String text);

    /**
     * 批量导入敏感词
     *
     * @param file 敏感词文件(每行一个)
     * @return 导入数量
     */
    int importWords(MultipartFile file);
}
