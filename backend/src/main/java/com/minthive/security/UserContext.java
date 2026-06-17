package com.minthive.security;

import java.util.Optional;

/**
 * 用户上下文(ThreadLocal)
 *
 * <p>功能描述：在请求线程内传递当前登录用户信息，避免在方法间反复传参</p>
 * <p>注意事项：请求结束后必须调用 clear() 清理，防止内存泄漏</p>
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 设置当前登录用户
     *
     * @param user 登录用户信息
     */
    public static void set(LoginUser user) {
        CONTEXT.set(user);
    }

    /**
     * 获取当前登录用户
     *
     * @return 登录用户信息(可能为 null)
     */
    public static LoginUser get() {
        return CONTEXT.get();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     * @throws IllegalStateException 未登录时抛出
     */
    public static Long getUserId() {
        return Optional.ofNullable(CONTEXT.get())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前线程未设置登录用户"));
    }

    /**
     * 获取当前登录账号
     *
     * @return 账号
     */
    public static String getAccount() {
        return Optional.ofNullable(CONTEXT.get())
                .map(LoginUser::getAccount)
                .orElse(null);
    }

    /**
     * 清理当前线程的登录用户信息
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
