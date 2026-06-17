package com.minthive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置类
 *
 * <p>功能描述：读取 AI 双模式配置（cloud 公有云 / local 本地私有化），限流、缓存、降级参数</p>
 * <p>注意事项：通过 mode 字段一键切换 AI 调用模式，无需改动代码</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /** AI 模式: cloud 公有云 / local 本地私有化 */
    private String mode = "local";

    /** 公有云配置 */
    private Cloud cloud = new Cloud();

    /** 本地私有化配置 */
    private Local local = new Local();

    /** 限流配置 */
    private RateLimit rateLimit = new RateLimit();

    /** 缓存配置 */
    private Cache cache = new Cache();

    /** 降级配置 */
    private Fallback fallback = new Fallback();

    /**
     * 公有云 AI 配置
     */
    @Data
    public static class Cloud {
        /** API 基础地址 */
        private String baseUrl;
        /** API Key */
        private String apiKey;
        /** 模型名称 */
        private String model;
        /** 超时时间(毫秒) */
        private Long timeout;
    }

    /**
     * 本地私有化 AI 配置
     */
    @Data
    public static class Local {
        /** Ollama 服务地址 */
        private String baseUrl;
        /** 本地模型名称 */
        private String model;
        /** 超时时间(毫秒) */
        private Long timeout;
    }

    /**
     * 限流配置
     */
    @Data
    public static class RateLimit {
        /** 每用户每日调用上限 */
        private Integer daily = 50;
    }

    /**
     * 缓存配置
     */
    @Data
    public static class Cache {
        /** 是否启用缓存 */
        private Boolean enabled = true;
        /** 缓存有效期(秒) */
        private Long ttl = 3600L;
    }

    /**
     * 降级配置
     */
    @Data
    public static class Fallback {
        /** 是否启用降级 */
        private Boolean enabled = true;
        /** 降级返回文案 */
        private String message;
    }
}
