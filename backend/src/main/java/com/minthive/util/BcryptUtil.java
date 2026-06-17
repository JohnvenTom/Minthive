package com.minthive.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bcrypt 密码加密工具类
 *
 * <p>功能描述：使用 Bcrypt 自适应加盐算法加密/校验密码，彻底淘汰 MD5</p>
 * <p>注意事项：每次加密生成不同盐值，相同明文加密结果不同</p>
 */
@Component
public class BcryptUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 加密明文密码
     *
     * @param rawPassword 明文密码
     * @return Bcrypt 加密后的密文
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验明文密码与密文是否匹配
     *
     * @param rawPassword  明文密码
     * @param encodedPassword 密文
     * @return true 匹配 / false 不匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
