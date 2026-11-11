package com.minthive.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 匿名操作身份标记工具
 *
 * <p>功能描述：登录/注册/验证码/重置密码等匿名写请求在 Controller 中把尝试账号与
 * 手机号指纹写入当前审计上下文，便于追踪暴力破解与注册滥用</p>
 * <p>注意事项：只允许传入账号与手机号，禁止把密码/验证码写入审计上下文</p>
 */
public final class AuditIdentity {

    private AuditIdentity() {
    }

    /**
     * 标记匿名操作身份
     *
     * @param account 尝试账号（可为空）
     * @param phone   手机号（可为空，只保存 SHA-256 指纹）
     */
    public static void mark(String account, String phone) {
        AuditScope scope = AuditScope.current();
        if (scope == null) {
            return;
        }
        if (account != null && !account.isBlank()) {
            scope.setOperatorLabel(account.length() <= 128 ? account : account.substring(0, 128));
        }
        if (phone != null && !phone.isBlank()) {
            scope.setIdentityHash(sha256(phone));
        }
    }

    /**
     * 计算 SHA-256 指纹
     *
     * @param value 原始值
     * @return 十六进制摘要
     */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }
}
