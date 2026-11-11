package com.minthive.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 跨域配置类
 *
 * <p>功能描述：全局配置 CORS 跨域策略，只允许 app.cors.allowed-origins 中显式声明的来源访问后端接口</p>
 * <p>注意事项：禁止使用通配符 *；未配置任何来源时直接启动失败，避免默认放开跨域</p>
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    /**
     * 配置 CORS 跨域规则
     *
     * @param registry 跨域注册器
     * @throws IllegalStateException 未配置来源或配置了通配符 * 时抛出
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = corsProperties.getAllowedOrigins() == null
                ? List.of()
                : corsProperties.getAllowedOrigins().stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .distinct()
                        .toList();
        if (origins.isEmpty()) {
            throw new IllegalStateException("app.cors.allowed-origins 未配置，请显式声明允许的前端来源");
        }
        if (origins.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException("app.cors.allowed-origins 不允许使用通配符 *");
        }
        registry.addMapping("/**")
                // 前端显式发送 Bearer Token，不使用跨站 Cookie/环境凭证
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "Range")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
