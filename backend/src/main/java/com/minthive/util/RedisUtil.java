package com.minthive.util;

import com.minthive.common.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
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
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
     * 缓存登录 Token
     *
     * @param userId 用户ID
     * @param token  Token
     */
    public void cacheLoginToken(Long userId, String token) {
        String key = RedisConstants.LOGIN_TOKEN_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, token, RedisConstants.ONE_DAY_SECONDS * 7, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的登录 Token
     *
     * @param userId 用户ID
     * @return Token
     */
    public String getLoginToken(Long userId) {
        return stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_TOKEN_PREFIX + userId);
    }

    /**
     * 删除登录 Token
     *
     * @param userId 用户ID
     */
    public void deleteLoginToken(Long userId) {
        stringRedisTemplate.delete(RedisConstants.LOGIN_TOKEN_PREFIX + userId);
    }
}
