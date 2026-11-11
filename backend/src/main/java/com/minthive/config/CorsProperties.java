package com.minthive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域白名单配置
 *
 * <p>功能描述：读取 app.cors.allowed-origins 配置项，支持逗号分隔的多个来源</p>
 * <p>注意事项：生产环境必须通过环境变量 CORS_ALLOWED_ORIGINS 显式配置，禁止使用通配符 *</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /** 允许跨域的前端来源列表（精确匹配，不含路径） */
    private List<String> allowedOrigins = new ArrayList<>();
}
