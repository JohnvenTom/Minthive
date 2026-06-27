package com.minthive.util;

import com.minthive.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JWT 工具门面
 *
 * <p>功能描述：对外暴露 JWT 工具方法，内部委托给 JwtUtils</p>
 * <p>注意事项：保持与 security 包 JwtUtils 一致的行为</p>
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtUtils jwtUtils;

    /**
     * 生成 Token
     *
     * @param userId  用户ID
     * @param account 账号
     * @param role    角色
     * @return Token 字符串
     */
    public String generateToken(Long userId, String account, Integer role) {
        return jwtUtils.generateToken(userId, account, role);
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * 校验 Token 是否有效
     *
     * @param token JWT Token
     * @return true 有效 / false 无效
     */
    public boolean validateToken(String token) {
        return jwtUtils.validateToken(token);
    }
}
