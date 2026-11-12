package com.minthive.audit;

import com.minthive.common.Constants;
import com.minthive.common.TraceIdFilter;
import com.minthive.entity.AuditLog;
import com.minthive.security.LoginUser;
import com.minthive.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.Locale;

/**
 * 写请求审计 Web 拦截器
 *
 * <p>功能描述：为非 GET 的 /api/** 请求创建审计上下文，请求结束后汇总操作者、
 * 客户端信息、结果与耗时并投递异步写入；GET 请求只创建“不采集”标记用于跳过 DB 写入</p>
 * <p>注意事项：必须注册在 JwtInterceptor 之后，afterCompletion 才能读取到登录身份</p>
 */
@Component
@RequiredArgsConstructor
public class AuditWebInterceptor implements AsyncHandlerInterceptor {

    private final AuditProperties properties;

    private final AuditWriter writer;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean auditable = !isReadMethod(request.getMethod());
        String remoteAddr = request.getRemoteAddr();
        String forwardedFor = truncate(request.getHeader("X-Forwarded-For"), 512);
        AuditScope.beginHttp(auditable, request.getMethod(), truncate(path, 512),
                truncate(request.getQueryString(), 1024), truncate(remoteAddr, 64), forwardedFor,
                resolveClientIp(remoteAddr, forwardedFor), truncate(request.getHeader("User-Agent"), 512));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        finish(response.getStatus());
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // SSE/异步请求不会回调 afterCompletion，这里先用当前状态提交请求级审计，
        // 异步线程后续产生的写入会以 SYSTEM 主体单独记录
        finish(response.getStatus());
    }

    /**
     * 汇总并提交当前审计上下文
     *
     * @param httpStatus HTTP 状态码
     */
    private void finish(int httpStatus) {
        AuditScope scope = AuditScope.current();
        if (scope == null) {
            return;
        }
        try {
            if (!scope.isAuditable()) {
                return;
            }
            LoginUser user = UserContext.get();
            String operatorType;
            Long operatorId = null;
            Integer operatorRole = null;
            if (user == null) {
                operatorType = "ANONYMOUS";
            } else {
                operatorId = user.getUserId();
                operatorRole = user.getRole();
                operatorType = Integer.valueOf(Constants.ROLE_ADMIN).equals(user.getRole()) ? "ADMIN" : "USER";
                if (scope.getOperatorLabel() == null) {
                    scope.setOperatorLabel(user.getAccount());
                }
            }
            long durationMs = (System.nanoTime() - scope.getStartNanos()) / 1_000_000L;
            AuditLog log = scope.toAuditLog(operatorType, operatorId, operatorRole,
                    MDC.get(TraceIdFilter.MDC_KEY), httpStatus,
                    (int) Math.min(durationMs, Integer.MAX_VALUE));
            writer.submit(AuditEventFactory.event(log, scope.getChanges()));
        } finally {
            // 拦截器/线程复用前必须清理，避免污染下一个请求
            AuditScope.clear();
        }
    }

    /**
     * 判断是否为只读 HTTP 方法
     *
     * @param method HTTP 方法
     * @return true=只读
     */
    private boolean isReadMethod(String method) {
        String normalized = method == null ? "" : method.toUpperCase(Locale.ROOT);
        return "GET".equals(normalized) || "HEAD".equals(normalized) || "OPTIONS".equals(normalized);
    }

    /**
     * 解析可信客户端 IP
     *
     * @param remoteAddr   直接连接地址
     * @param forwardedFor X-Forwarded-For 原文
     * @return 可信代理解析出的客户端 IP，不可信或缺失时返回 null
     */
    private String resolveClientIp(String remoteAddr, String forwardedFor) {
        if (remoteAddr == null || forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        boolean trusted = properties.getTrustedProxies() != null && properties.getTrustedProxies().stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .anyMatch(remoteAddr::equals);
        if (!trusted) {
            return null;
        }
        String first = forwardedFor.split(",")[0].trim();
        return first.matches("[0-9a-fA-F:.]{3,64}") ? first : null;
    }

    /**
     * 字符串截断
     *
     * @param value 原始值
     * @param max   最大长度
     * @return 截断后的值
     */
    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
