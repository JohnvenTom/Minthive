package com.minthive.config;

import com.minthive.security.AdminAuthInterceptor;
import com.minthive.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 所有 API 统一认证；匿名访问由 JWT 拦截器按 HTTP 方法和精确路径判定。 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor).addPathPatterns("/api/**").order(0);
        registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/api/admin/**").order(1);
    }
}
