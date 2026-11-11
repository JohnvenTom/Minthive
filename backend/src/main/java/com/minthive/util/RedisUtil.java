package com.minthive.util;

import com.minthive.common.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 操作工具类
 *
 * <p>功能描述：封装 Redis 常用操作，支持普通对象与字符串操作</p>
 * <p>注意事项：所有 Key 应使用 RedisConstants 中定义的前缀</p>
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 写入对象缓存(带过期时间)
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 写入对象缓存(永久)
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 读取对象缓存
     *
     * @param key 缓存键
     * @return 缓存值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 读取对象缓存(指定类型)
     *
     * @param key   缓存键
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return clazz.cast(value);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return true 删除成功
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return true 存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 自增(默认步长1)
     *
     * @param key 缓存键
     * @return 自增后的值
     */
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增指定步长
     *
     * @param key   缓存键
     * @param delta 步长
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 设置过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true 设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 缓存登录 Token（按设备维度存储，同一用户多端登录互不影响）
     *
     * @param userId 用户ID
     * @param token  JWT Token
     * @note Key 中只保存 Token 的 SHA-256 摘要，避免明文凭证出现在 Redis 键空间中
     */
    public void cacheLoginToken(Long userId, String token) {
        long ttl = RedisConstants.ONE_DAY_SECONDS * 7;
        String setKey = RedisConstants.LOGIN_TOKEN_SET_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(loginTokenKey(userId, token), "1", ttl, TimeUnit.SECONDS);
        stringRedisTemplate.opsForSet().add(setKey, tokenHash(token));
        // 集合过期时间与单 Token 对齐，避免集合永久残留
        stringRedisTemplate.expire(setKey, ttl, TimeUnit.SECONDS);
    }

    /**
     * 判断某个 Token 是否仍是有效登录态
     *
     * @param userId 用户ID
     * @param token  待校验的 JWT Token
     * @return true 表示该设备的会话仍然有效
     * @note 只校验“存在性”，因此同一用户在不同设备上的 Token 可以同时有效
     */
    public boolean hasLoginToken(Long userId, String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(loginTokenKey(userId, token)));
    }

    /**
     * 撤销单个设备的登录 Token（登出时使用）
     *
     * @param userId 用户ID
     * @param token  当前设备持有的 JWT Token
     */
    public void deleteLoginToken(Long userId, String token) {
        stringRedisTemplate.delete(loginTokenKey(userId, token));
        stringRedisTemplate.opsForSet().remove(RedisConstants.LOGIN_TOKEN_SET_PREFIX + userId, tokenHash(token));
    }

    /**
     * 撤销该用户全部设备的登录 Token（封禁/改密/注销/角色变更时使用）
     *
     * @param userId 用户ID
     * @note 依赖用户 Token 摘要集合批量删除；集合不存在时仅执行一次删除，属于幂等操作
     */
    public void deleteLoginToken(Long userId) {
        String setKey = RedisConstants.LOGIN_TOKEN_SET_PREFIX + userId;
        java.util.Set<String> hashes = stringRedisTemplate.opsForSet().members(setKey);
        if (hashes != null && !hashes.isEmpty()) {
            java.util.List<String> keys = hashes.stream()
                    .map(hash -> RedisConstants.LOGIN_TOKEN_PREFIX + userId + ":" + hash)
                    .toList();
            stringRedisTemplate.delete(keys);
        }
        stringRedisTemplate.delete(setKey);
    }

    /**
     * 计算 Token 摘要
     *
     * @param token JWT Token
     * @return 十六进制 SHA-256 摘要
     * @throws IllegalStateException JDK 不提供 SHA-256 时抛出（实际不会发生）
     */
    public static String tokenHash(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 构建单设备登录态 Key
     *
     * @param userId 用户ID
     * @param token  JWT Token
     * @return Redis Key
     */
    public static String loginTokenKey(Long userId, String token) {
        return RedisConstants.LOGIN_TOKEN_PREFIX + userId + ":" + tokenHash(token);
    }

    /**
     * 缓存短信验证码（5分钟过期）
     *
     * @param phone 手机号
     * @param code  验证码
     */
    public void cacheSmsCode(String phone, String code) {
        String key = RedisConstants.SMS_CODE_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
    }

    /**
     * 获取并删除短信验证码（一次性使用）
     *
     * @param phone 手机号
     * @return 验证码，不存在返回 null
     */
    public String getAndDeleteSmsCode(String phone) {
        String key = RedisConstants.SMS_CODE_PREFIX + phone;
        String code = stringRedisTemplate.opsForValue().get(key);
        if (code != null) {
            stringRedisTemplate.delete(key);
        }
        return code;
    }

    /**
     * 检查短信验证码发送频率（60秒内不可重复发送）
     *
     * @param phone 手机号
     * @return true 表示频率受限，不可发送
     */
    public boolean isSmsRateLimited(String phone) {
        String key = RedisConstants.SMS_CODE_PREFIX + phone + ":limit";
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 标记短信验证码发送频率（设置60秒过期）
     *
     * @param phone 手机号
     */
    public void markSmsSent(String phone) {
        String key = RedisConstants.SMS_CODE_PREFIX + phone + ":limit";
        stringRedisTemplate.opsForValue().set(key, "1", 60, TimeUnit.SECONDS);
    }
}
