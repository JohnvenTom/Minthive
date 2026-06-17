package com.minthive.common;

/**
 * Redis Key 常量定义
 *
 * <p>功能描述：集中管理所有 Redis 缓存 Key 前缀，避免散落在代码各处</p>
 * <p>注意事项：Key 设计遵循 业务:维度:标识 格式</p>
 */
public final class RedisConstants {

    private RedisConstants() {
    }

    /** 登录态 Token 前缀 */
    public static final String LOGIN_TOKEN_PREFIX = "minthive:token:";

    /** 用户信息缓存前缀 */
    public static final String USER_INFO_PREFIX = "minthive:user:info:";

    /** 热门帖子缓存前缀 */
    public static final String HOT_POST_PREFIX = "minthive:post:hot:";

    /** 未读消息数前缀 */
    public static final String UNREAD_MSG_PREFIX = "minthive:msg:unread:";

    /** AI 请求缓存前缀 */
    public static final String AI_CACHE_PREFIX = "minthive:ai:cache:";

    /** AI 用户画像缓存前缀 */
    public static final String AI_PROFILE_PREFIX = "minthive:ai:profile:";

    /** AI 用户兴趣向量前缀(RedisVector) */
    public static final String AI_INTEREST_VECTOR_PREFIX = "minthive:ai:vector:";

    /** AI 每日调用计数前缀 */
    public static final String AI_DAILY_COUNT_PREFIX = "minthive:ai:count:";

    /** 发帖每日计数前缀 */
    public static final String POST_DAILY_COUNT_PREFIX = "minthive:post:count:";

    /** 评论每日计数前缀 */
    public static final String COMMENT_DAILY_COUNT_PREFIX = "minthive:comment:count:";

    /** 私信每日计数前缀 */
    public static final String MESSAGE_DAILY_COUNT_PREFIX = "minthive:message:count:";

    /** WebSocket 在线用户前缀 */
    public static final String WS_ONLINE_PREFIX = "minthive:ws:online:";

    /** 短信验证码前缀 */
    public static final String SMS_CODE_PREFIX = "minthive:sms:code:";

    /** 一天秒数 */
    public static final long ONE_DAY_SECONDS = 24 * 60 * 60L;
    /** 一小时秒数 */
    public static final long ONE_HOUR_SECONDS = 60 * 60L;
}
