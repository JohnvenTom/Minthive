package com.minthive.config;

import com.minthive.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 *
 * <p>功能描述：注册 JWT 拦截器，配置拦截路径与放行路径</p>
 * <p>注意事项：登录、注册、Swagger 文档等路径需放行</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 注册拦截器，配置拦截规则
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 登录注册放行（无需 Token）
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/sms",
                        "/api/auth/login/sms",
                        "/api/auth/reset",
                        // 首页信息流放行（允许未登录浏览）
                        "/api/post/feed",
                        // AI 助手放行（允许未登录使用）
                        "/api/ai/**",
                        // 文档放行
                        "/doc.html",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/favicon.ico",
                        // 静态资源放行
                        "/static/**",
                        // WebSocket 端点放行(连接时单独校验 token)
                        "/ws/**"
                );
    }
}
