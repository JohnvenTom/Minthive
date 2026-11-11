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
     * 原子预占一次 AI 调用额度
     *
     * <p>功能描述：用 Redis INCR 的原子性完成"校验 + 计数"，
     * 避免 checkLimit 与 increment 分离导致的并发超额；超限时回滚本次自增并抛出业务异常</p>
     *
     * @return 无返回值
     * @throws BusinessException 当日额度已用完时抛出 AI_RATE_LIMITED
     * @note Redis 不可用或未登录时放行，与 checkLimit 的降级策略保持一致
     */
    public void reserve() {
        try {
            Long userId = getUserIdSafe();
            if (userId == null) return;
            String key = buildKey(userId);
            Long newCount = stringRedisTemplate.opsForValue().increment(key);
            if (newCount == null) return;
            if (newCount == 1L) {
                stringRedisTemplate.expire(key, computeSecondsUntilEndOfDay(), TimeUnit.SECONDS);
            }
            int limit = aiConfig.getRateLimit().getDaily();
            if (newCount > limit) {
                // 超出上限的这次调用不消耗额度，回滚自增
                stringRedisTemplate.opsForValue().decrement(key);
                log.warn("用户 {} 当日AI调用次数已达上限 {}", userId, limit);
                throw new BusinessException(ResultCode.AI_RATE_LIMITED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[AiRateLimiter] 额度预占异常，跳过: {}", e.getMessage());
        }
    }

    /**
     * 释放一次已预占的额度
     *
     * <p>功能描述：AI 调用失败时把 reserve() 预占的计数回滚，保证失败请求不消耗额度</p>
     *
     * @return 无返回值
     * @note Redis 不可用或未登录时静默跳过
     */
    public void release() {
        try {
            Long userId = getUserIdSafe();
            if (userId == null) return;
            stringRedisTemplate.opsForValue().decrement(buildKey(userId));
        } catch (Exception e) {
            log.warn("[AiRateLimiter] 额度回滚异常，跳过: {}", e.getMessage());
        }
    }

    /**
     * 查询当前用户今日剩余 AI 调用次数
     *
     * <p>Redis 不可用时返回配置的上限值</p>
     *
     * @return 剩余次数
     */
    public int remaining() {
        try {
            Long userId = getUserIdSafe();
            if (userId == null) return aiConfig.getRateLimit().getDaily(); // 未登录返回上限值
            String key = buildKey(userId);
            String countStr = stringRedisTemplate.opsForValue().get(key);
            int count = countStr == null ? 0 : Integer.parseInt(countStr);
            return Math.max(0, aiConfig.getRateLimit().getDaily() - count);
        } catch (Exception e) {
            log.warn("[AiRateLimiter] Redis 查询异常，返回默认值: {}", e.getMessage());
            return aiConfig.getRateLimit().getDaily();
        }
    }

    /**
     * 安全获取当前用户ID（未登录时返回 null 而非抛异常）
     *
     * <p>适用于 AI 接口等免登录场景，避免 IllegalStateException 噪音日志</p>
     *
     * @return 用户ID，未登录返回 null
     */
    private Long getUserIdSafe() {
        com.minthive.security.LoginUser user = UserContext.get();
        return user != null ? user.getUserId() : null;
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
