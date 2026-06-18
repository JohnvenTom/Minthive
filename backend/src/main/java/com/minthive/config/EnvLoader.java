package com.minthive.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * .env 环境变量加载器
 *
 * <p>功能描述：在 Spring 启动前读取项目根目录下的 .env 文件，
 *   将其中的 KEY=VALUE 写入 System Properties，使 application.yml 中的
 *   ${ENV_KEY:默认值} 占位符能正确解析</p>
 *
 * <p>使用方式：在 main 方法中 SpringApplication.run() 之前调用 EnvLoader.load()</p>
 *
 * <p>注意事项：.env 文件格式为每行 KEY=Value，# 开头行为注释，忽略空行</p>
 */
@Slf4j
public class EnvLoader {

    /** 默认 .env 文件名 */
    private static final String ENV_FILE = ".env";

    /**
     * 加载 .env 文件并写入系统属性
     *
     * <p>从项目根目录读取 .env，每行解析为 KEY=VALUE 并设置到 System.setProperty()，
     * 使 Spring Boot 的 ${ENV_KEY:默认值} 能正确解析</p>
     *
     * <p>.env 文件不存在或读取失败时仅打印警告，不阻塞启动（使用 yml 中的默认值兜底）</p>
     */
    public static void load() {
        Path envPath = Paths.get(ENV_FILE);
        if (!Files.exists(envPath)) {
            log.info(".env 文件未找到，将使用 application.yml 中的默认配置");
            return;
        }
        try {
            int count = 0;
            for (String line : Files.readAllLines(envPath)) {
                String trimmed = line.trim();
                // 跳过空行和注释
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eqIdx = trimmed.indexOf('=');
                if (eqIdx <= 0) {
                    continue; // 忽略无等号或 key 为空的行
                }
                String key = trimmed.substring(0, eqIdx).trim();
                String value = trimmed.substring(eqIdx + 1).trim();
                System.setProperty(key, value);
                count++;
            }
            log.info("已从 .env 加载 {} 个环境变量", count);
        } catch (IOException e) {
            log.warn("读取 .env 文件失败: {}，将使用默认配置", e.getMessage());
        }
    }
}
