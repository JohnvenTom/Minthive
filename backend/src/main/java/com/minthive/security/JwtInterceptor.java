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

    /**
     * 请求前置处理：校验 Token
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return true 放行 / false 拦截
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头获取 Token
        String header = request.getHeader(jwtConfig.getHeader());
        if (!StringUtils.hasText(header) || !header.startsWith(jwtConfig.getPrefix())) {
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

            // 校验 Redis 中 Token 是否仍然有效(支持登出失效)
            String redisKey = RedisConstants.LOGIN_TOKEN_PREFIX + userId;
            String cachedToken = stringRedisTemplate.opsForValue().get(redisKey);
            if (!StringUtils.hasText(cachedToken) || !cachedToken.equals(token)) {
                return writeUnauthorized(response, ResultCode.TOKEN_EXPIRED);
            }

            // 存入上下文
            UserContext.set(new LoginUser(userId, account, role));
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
