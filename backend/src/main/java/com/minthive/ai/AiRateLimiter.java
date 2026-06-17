package com.minthive.ai;

import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.common.RedisConstants;
import com.minthive.config.AiConfig;
import com.minthive.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * AI 限流器
 *
 * <p>功能描述：基于 Redis 计数，限制单用户每日 AI 调用次数(默认 50 次)</p>
 * <p>注意事项：每日 0 点过期重置；超限抛出 AI_RATE_LIMITED 业务异常</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    private final AiConfig aiConfig;

    /**
     * 校验当前用户是否超出每日 AI 调用上限
     *
     * @throws BusinessException 超出上限时抛出
     */
    public void checkLimit() {
        Long userId = UserContext.getUserId();
        String key = buildKey(userId);
        String countStr = stringRedisTemplate.opsForValue().get(key);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        int limit = aiConfig.getRateLimit().getDaily();
        if (count >= limit) {
            log.warn("用户 {} 当日AI调用次数已达上限 {}", userId, limit);
            throw new BusinessException(ResultCode.AI_RATE_LIMITED);
        }
    }

    /**
     * 记录一次 AI 调用(计数 +1)
     */
    public void increment() {
        Long userId = UserContext.getUserId();
        String key = buildKey(userId);
        Long newCount = stringRedisTemplate.opsForValue().increment(key);
        // 首次调用设置过期时间(到当日 23:59:59)
        if (newCount != null && newCount == 1L) {
            long secondsUntilEndOfDay = computeSecondsUntilEndOfDay();
            stringRedisTemplate.expire(key, secondsUntilEndOfDay, TimeUnit.SECONDS);
        }
    }

    /**
     * 查询当前用户今日剩余 AI 调用次数
     *
     * @return 剩余次数
     */
    public int remaining() {
        Long userId = UserContext.getUserId();
        String key = buildKey(userId);
        String countStr = stringRedisTemplate.opsForValue().get(key);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        return Math.max(0, aiConfig.getRateLimit().getDaily() - count);
    }

    /**
     * 构建 Redis Key
     *
     * @param userId 用户ID
     * @return Redis Key
     */
    private String buildKey(Long userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return RedisConstants.AI_DAILY_COUNT_PREFIX + userId + ":" + today;
    }

    /**
     * 计算当日剩余秒数(到 23:59:59)
     *
     * @return 剩余秒数
     */
    private long computeSecondsUntilEndOfDay() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        return java.time.Duration.between(now, endOfDay).getSeconds();
    }
}
