package com.minthive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.entity.User;
import com.minthive.mapper.UserMapper;
import com.minthive.common.RedisConstants;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.List;
import java.util.Set;

/** 所有 API 的认证入口：公开接口可不带凭证，但携带凭证时必须完整验证。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements AsyncHandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final JwtConfig jwtConfig;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/auth/login", "/api/auth/login/sms", "/api/auth/register",
            "/api/auth/sms", "/api/auth/reset");
    private static final List<String> PUBLIC_GET_PATTERNS = List.of(
            "/api/post/feed", "/api/post/search", "/api/post/page", "/api/post/{id:[0-9]+}",
            "/api/circle/page", "/api/circle/categories", "/api/circle/recommend",
            "/api/circle/{id:[0-9]+}", "/api/circle/{id:[0-9]+}/posts",
            "/api/user/{id:[0-9]+}", "/api/user/{id:[0-9]+}/posts",
            "/api/search", "/api/search/user", "/api/search/post", "/api/search/circle", "/api/search/hot",
            "/api/comment/page", "/api/announcement/latest", "/api/announcement/list", "/api/config/banners",
            // 媒体代理使用独立短时票据鉴权，不接受登录 JWT 替代票据。
            "/api/file/content/{object:[A-Za-z0-9_-]+}");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // preHandle 拒绝的请求不会触发自身 afterCompletion，也要确保线程上没有遗留身份。
        UserContext.clear();
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String header = request.getHeader(jwtConfig.getHeader());
        if (header == null) {
            return allowsAnonymous(request.getMethod(), path)
                    || writeUnauthorized(response, ResultCode.UNAUTHORIZED);
        }
        // 空白、错误前缀也属于无效凭证，不能在公开接口降级为匿名。
        if (!header.startsWith(jwtConfig.getPrefix())) {
            return writeUnauthorized(response, ResultCode.TOKEN_INVALID);
        }
        String token = header.substring(jwtConfig.getPrefix().length()).trim();
        if (!StringUtils.hasText(token)) {
            return writeUnauthorized(response, ResultCode.TOKEN_INVALID);
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            Object subjectId = claims.get("userId");
            if (!(subjectId instanceof Number) || ((Number) subjectId).longValue() <= 0) {
                return writeUnauthorized(response, ResultCode.TOKEN_INVALID);
            }
            Long userId = ((Number) subjectId).longValue();
            String cachedToken = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_TOKEN_PREFIX + userId);
            if (!StringUtils.hasText(cachedToken)) {
                return writeUnauthorized(response, ResultCode.TOKEN_EXPIRED);
            }
            if (!cachedToken.equals(token)) {
                return writeUnauthorized(response, ResultCode.ACCOUNT_KICKED_OUT);
            }
            // Redis 只证明会话仍活跃，权限和账号状态必须以当前数据库为准。
            User user = userMapper.selectById(userId);
            if (user == null || !Integer.valueOf(Constants.USER_STATUS_NORMAL).equals(user.getStatus())
                    || !Integer.valueOf(Constants.CANCEL_STATUS_NORMAL).equals(user.getCancelStatus())
                    || !Integer.valueOf(0).equals(user.getDeleted())
                    || user.getRole() == null || user.getRole() < Constants.ROLE_USER
                    || user.getRole() > Constants.ROLE_ADMIN) {
                stringRedisTemplate.delete(RedisConstants.LOGIN_TOKEN_PREFIX + userId);
                return writeUnauthorized(response, ResultCode.UNAUTHORIZED);
            }
            UserContext.set(new LoginUser(userId, user.getAccount(), user.getRole()));
            return true;
        } catch (BusinessException | IllegalArgumentException e) {
            return writeUnauthorized(response, ResultCode.TOKEN_INVALID);
        } catch (Exception e) {
            log.error("认证依赖校验失败", e);
            return writeUnauthorized(response, ResultCode.SERVER_ERROR);
        }
    }

    private boolean allowsAnonymous(String method, String path) {
        if ("POST".equals(method)) {
            return PUBLIC_POST_PATHS.contains(path);
        }
        return "GET".equals(method) && PUBLIC_GET_PATTERNS.stream().anyMatch(p -> PATH_MATCHER.match(p, path));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.clear();
    }

    private boolean writeUnauthorized(HttpServletResponse response, ResultCode resultCode) throws Exception {
        UserContext.clear();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(resultCode)));
        return false;
    }
}
