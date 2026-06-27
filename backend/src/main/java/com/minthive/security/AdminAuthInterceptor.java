package com.minthive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.common.Constants;
import com.minthive.common.Result;
import com.minthive.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        LoginUser user = UserContext.get();
        if (user == null || user.getRole() < Constants.ROLE_ADMIN) {
            log.warn("非管理员访问后台接口被拦截: userId={}, role={}", user != null ? user.getUserId() : null, user != null ? user.getRole() : null);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(ResultCode.FORBIDDEN)));
            return false;
        }
        return true;
    }
}
