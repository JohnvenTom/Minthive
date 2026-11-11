package com.minthive.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路追踪过滤器
 *
 * <p>功能描述：为每个 HTTP 请求生成唯一的 traceId，写入 MDC 供日志输出、
 * 响应体（Result.traceId）与响应头（X-Trace-Id）共同使用，便于按请求串联日志</p>
 *
 * <p>注意事项：请求线程结束时必须清理 MDC，避免线程池复用造成 traceId 串号；
 * 异步/SSE 线程不会自动继承该值</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** MDC 中存放 traceId 的键名 */
    public static final String MDC_KEY = "traceId";

    /** 响应头名称 */
    public static final String HEADER = "X-Trace-Id";

    /** 兼容上游网关传入的追踪头，缺失时再本地生成 */
    private static final String INBOUND_HEADER = "X-Request-Id";

    /**
     * 生成并绑定 traceId，执行完请求后清理
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     * @return 无返回值
     * @throws ServletException Servlet 处理异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 解析追踪标识
     *
     * <p>功能描述：优先沿用上游传入的 X-Request-Id（限制长度与字符集防注入），
     * 否则生成本地随机 traceId</p>
     *
     * @param request HTTP 请求
     * @return 可直接写入日志与响应头的追踪标识
     */
    private String resolveTraceId(HttpServletRequest request) {
        String inbound = request.getHeader(INBOUND_HEADER);
        if (inbound != null && inbound.matches("[A-Za-z0-9_-]{8,64}")) {
            return inbound;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
