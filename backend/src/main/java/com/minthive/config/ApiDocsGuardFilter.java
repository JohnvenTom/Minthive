package com.minthive.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 接口文档访问守卫
 *
 * <p>功能描述：当 springdoc.api-docs.enabled=false（生产默认）时，
 * 直接以 404 拒绝文档页面、OpenAPI JSON、Swagger UI 与 Knife4j 静态资源，
 * 避免仅靠框架开关时静态页面仍可被访问</p>
 *
 * <p>注意事项：开发环境保持 api-docs.enabled=true 时不受影响</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiDocsGuardFilter extends OncePerRequestFilter {

    /** 是否允许访问接口文档 */
    @Value("${springdoc.api-docs.enabled:true}")
    private boolean apiDocsEnabled;

    /** 需要保护的文档路径前缀 */
    private static final String[] PROTECTED_PATHS = {
            "/doc.html", "/v3/api-docs", "/swagger-ui", "/webjars"
    };

    /**
     * 在文档关闭时拦截文档相关请求
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!apiDocsEnabled) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            for (String protectedPath : PROTECTED_PATHS) {
                if (path.equals(protectedPath) || path.startsWith(protectedPath + "/")) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
