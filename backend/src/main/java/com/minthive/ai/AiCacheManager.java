package com.minthive.ai;

import com.minthive.common.RedisConstants;
import com.minthive.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI 缓存管理器
 *
 * <p>功能描述：将高频 AI 请求结果缓存到 Redis，减少重复调用大模型</p>
 * <p>注意事项：缓存 Key 基于请求参数哈希；可通过配置关闭缓存</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private final AiConfig aiConfig;

    /**
     * 读取缓存
     *
     * @param scene  AI 场景(如 generatePostContent)
     * @param param  请求参数
     * @return 缓存结果，未命中返回 null
     */
    public Object get(String scene, String param) {
        if (!Boolean.TRUE.equals(aiConfig.getCache().getEnabled())) {
            return null;
        }
        String key = buildKey(scene, param);
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("AI缓存读取失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 写入缓存
     *
     * @param scene  AI 场景
     * @param param  请求参数
     * @param result 结果
     */
    public void put(String scene, String param, Object result) {
        if (!Boolean.TRUE.equals(aiConfig.getCache().getEnabled())) {
            return;
        }
        String key = buildKey(scene, param);
        try {
            redisTemplate.opsForValue().set(key, result, aiConfig.getCache().getTtl(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("AI缓存写入失败: {}", e.getMessage());
        }
    }

    /**
     * 构建缓存 Key
     *
     * @param scene AI 场景
     * @param param 请求参数
     * @return Redis Key
     */
    private String buildKey(String scene, String param) {
        int hash = param == null ? 0 : param.hashCode();
        return RedisConstants.AI_CACHE_PREFIX + scene + ":" + hash;
    }
}
