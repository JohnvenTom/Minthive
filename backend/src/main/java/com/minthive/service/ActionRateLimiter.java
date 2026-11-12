package com.minthive.service;

import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.config.ActionRateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 用户行为防刷限流器
 *
 * <p>功能描述：基于 Redis 原子自增，限制单用户每日发帖 / 评论 / 私信次数。
 * 计数键按天隔离，首次写入时设置到当日结束的过期时间</p>
 *
 * <p>注意事项：Redis 不可用时放行，避免基础设施故障阻断正常业务；
 * 超出上限时回滚本次自增并抛出业务异常</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    private final ActionRateLimitProperties properties;

    /** 受限制的行为类型及其计数键标识 */
    public enum Action {
        /** 发布帖子（含转发） */
        POST("post", "发帖"),
        /** 发表评论 */
        COMMENT("comment", "评论"),
        /** 发送私信 */
        MESSAGE("message", "私信");

        private final String code;
        private final String label;

        Action(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    /**
     * 预占一次行为额度
     *
     * @param action 行为类型
     * @param userId 当前用户ID，为空时直接放行（由调用方保证登录态）
     * @throws BusinessException 超出当日上限时抛出 RATE_LIMITED
     */
    public void reserve(Action action, Long userId) {
        if (userId == null) return;
        int limit = limitOf(action);
        if (limit <= 0) return;
        String key = buildKey(action, userId);
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count == null) return;
            if (count == 1L) {
                stringRedisTemplate.expire(key, secondsUntilEndOfDay(), TimeUnit.SECONDS);
            }
            if (count > limit) {
                // 超限请求不占用额度
                stringRedisTemplate.opsForValue().decrement(key);
                log.warn("用户 {} {}次数已达每日上限 {}", userId, action.label, limit);
                throw new BusinessException(ResultCode.RATE_LIMITED,
                        action.label + "过于频繁，请明天再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[ActionRateLimiter] {} 限流校验异常，放行: {}", action.label, e.getMessage());
        }
    }

    /**
     * 释放一次已预占的额度
     *
     * <p>用于业务写入失败时回滚计数，避免失败请求消耗用户额度</p>
     *
     * @param action 行为类型
     * @param userId 当前用户ID
     */
    public void release(Action action, Long userId) {
        if (userId == null || limitOf(action) <= 0) return;
        try {
            stringRedisTemplate.opsForValue().decrement(buildKey(action, userId));
        } catch (Exception e) {
            log.warn("[ActionRateLimiter] {} 额度回滚异常: {}", action.label, e.getMessage());
        }
    }

    /**
     * 读取行为对应的每日上限
     *
     * @param action 行为类型
     * @return 每日上限，小于等于 0 表示不限制
     */
    private int limitOf(Action action) {
        return switch (action) {
            case POST -> properties.getPostDaily();
            case COMMENT -> properties.getCommentDaily();
            case MESSAGE -> properties.getMessageDaily();
        };
    }

    /**
     * 构建按天隔离的计数键
     *
     * @param action 行为类型
     * @param userId 用户ID
     * @return Redis 键
     */
    private String buildKey(Action action, Long userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "minthive:action:" + action.code + ":" + userId + ":" + today;
    }

    /**
     * 计算到当日 23:59:59 的剩余秒数
     *
     * @return 剩余秒数
     */
    private long secondsUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        return Math.max(1, java.time.Duration.between(now, endOfDay).getSeconds());
    }
}
