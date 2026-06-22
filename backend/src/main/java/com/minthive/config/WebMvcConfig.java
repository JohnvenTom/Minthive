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
                        // 公开浏览接口（允许未登录访问）
                        "/api/post/feed",           // 首页信息流
                        "/api/post/search",         // 帖子搜索
                        "/api/post/page",           // 帖子分页列表
                        "/api/search/**",           // 搜索接口
                        "/api/circle/page",           // 圈子分页列表（公开浏览）
                        "/api/circle/categories",      // 圈子分类列表（公开）
                        "/api/circle/recommend",       // 推荐圈子（公开）
                        "/api/circle/*/posts",         // 圈内动态列表（公开浏览）
                        "/api/circle/{id}",             // 圈子详情（公开浏览）
                        // 用户主页公开浏览（Spring AntPathMatcher 支持 {id} 通配匹配 /api/user/123）
                        "/api/user/{id}",
                        "/api/user/{id}/posts",
                        "/api/comment/page",           // 评论分页列表（公开浏览）
                        // 帖子详情公开浏览
                        "/api/post/{id}",
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
