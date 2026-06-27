package com.minthive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.common.BusinessException;
import com.minthive.common.Result;
import com.minthive.common.ResultCode;
import com.minthive.config.JwtConfig;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.minthive.common.RedisConstants;

import java.util.List;

/**
 * JWT 拦截器
 *
 * <p>功能描述：拦截用户端请求，校验 JWT Token 合法性，并将登录用户信息存入 UserContext</p>
 * <p>注意事项：放行路径在 WebMvcConfig 中配置；请求结束后必须清理 ThreadLocal</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    private final JwtConfig jwtConfig;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private static final org.springframework.util.AntPathMatcher PATH_MATCHER = new org.springframework.util.AntPathMatcher();

    private static final List<String> PUBLIC_GET_PATTERNS = List.of(
            "/api/post/{id}",
            "/api/circle/{id}"
    );

    /**
     * 请求前置处理：校验 Token
     * <p>GET 请求的公开路径（帖子详情、圈子详情等）允许无 token 访问；
     * 其他方法或非公开路径必须携带有效 token。</p>
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return true 放行 / false 拦截
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求（OPTIONS）直接放行，由 CorsConfig 处理跨域响应头
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 从请求头获取 Token
        String header = request.getHeader(jwtConfig.getHeader());
        if (!StringUtils.hasText(header) || !header.startsWith(jwtConfig.getPrefix())) {
            // 无 token 时，允许已声明的公开 GET 路径放行（公开展示无需登录）
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                String path = request.getRequestURI();
                for (String pattern : PUBLIC_GET_PATTERNS) {
                    if (PATH_MATCHER.match(pattern, path)) {
                        return true;
                    }
                }
            }
            return writeUnauthorized(response, ResultCode.UNAUTHORIZED);
        }
        // 去除前缀
        String token = header.substring(jwtConfig.getPrefix().length()).trim();
        try {
            // 解析 Token
            Claims claims = jwtUtils.parseToken(token);
            Long userId = ((Number) claims.get("userId")).longValue();
            String account = (String) claims.get("account");
            Integer role = claims.get("role") == null ? 0 : ((Number) claims.get("role")).intValue();
            log.debug("Token解析成功: userId={}, account={}", userId, account);

            // 校验 Redis 中 Token 是否仍然有效(支持登出失效)
            String redisKey = RedisConstants.LOGIN_TOKEN_PREFIX + userId;
            String cachedToken = stringRedisTemplate.opsForValue().get(redisKey);
            log.debug("Redis校验: key={}, cachedToken存在={}, 是否匹配={}",
                    redisKey,
                    StringUtils.hasText(cachedToken),
                    cachedToken != null && cachedToken.equals(token));
            if (!StringUtils.hasText(cachedToken) || !cachedToken.equals(token)) {
                log.warn("Redis Token校验失败: userId={}, redisKey={}, token存在={}, 匹配结果={}",
                        userId, redisKey, StringUtils.hasText(cachedToken),
                        cachedToken != null && cachedToken.equals(token));
                return writeUnauthorized(response, ResultCode.TOKEN_EXPIRED);
            }

            // 存入上下文
            UserContext.set(new LoginUser(userId, account, role));
            log.debug("用户上下文设置成功: userId={}", userId);
            return true;
        } catch (BusinessException e) {
            return writeUnauthorized(response, ResultCode.TOKEN_INVALID);
        } catch (Exception e) {
            log.error("Token 校验异常: ", e);
            return writeUnauthorized(response, ResultCode.SERVER_ERROR);
        }
    }

    /**
     * 请求完成后清理 ThreadLocal
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 写入未授权响应
     *
     * @param response   HTTP 响应
     * @param resultCode 错误码
     * @return false 拦截请求
     * @throws Exception 写入异常
     */
    private boolean writeUnauthorized(HttpServletResponse response, ResultCode resultCode) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.error(resultCode);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        return false;
    }
}
