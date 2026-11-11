package com.minthive.config;

import com.minthive.audit.AuditWebInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 审计拦截器注册配置
 *
 * <p>功能描述：在 JWT 认证拦截器之后注册审计拦截器，确保请求结束时能读取登录身份</p>
 * <p>注意事项：只有 app.audit.enabled=true 时才注册，便于测试与紧急关闭</p>
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditWebConfig implements WebMvcConfigurer {

    private final AuditWebInterceptor auditWebInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditWebInterceptor).addPathPatterns("/api/**").order(2);
    }
}
