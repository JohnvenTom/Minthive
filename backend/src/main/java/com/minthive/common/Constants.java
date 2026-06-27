package com.minthive.common;

/**
 * 系统常量定义
 *
 * <p>功能描述：集中管理系统中使用的常量值</p>
 */
public final class Constants {

    private Constants() {
    }

    /** 用户角色: 普通用户 */
    public static final int ROLE_USER = 0;
    /** 用户角色: 圈主 */
    public static final int ROLE_CIRCLE_OWNER = 1;
    /** 用户角色: 管理员 */
    public static final int ROLE_ADMIN = 2;

    /** 账号状态: 封禁 */
    public static final int USER_STATUS_BANNED = 0;
    /** 账号状态: 正常 */
    public static final int USER_STATUS_NORMAL = 1;

    /** 注销状态: 正常 */
    public static final int CANCEL_STATUS_NORMAL = 0;
    /** 注销状态: 注销中(冷静期) */
    public static final int CANCEL_STATUS_PENDING = 1;
    /** 注销状态: 已注销 */
    public static final int CANCEL_STATUS_DONE = 2;

    /** 帖子可见范围: 公开 */
    public static final int VISIBILITY_PUBLIC = 0;
    /** 帖子可见范围: 仅粉丝 */
    public static final int VISIBILITY_FOLLOWER = 1;
    /** 帖子可见范围: 仅自己 */
    public static final int VISIBILITY_SELF = 2;

    /** 审核状态: 待审 */
    public static final int AUDIT_PENDING = 0;
    /** 审核状态: 通过 */
    public static final int AUDIT_PASS = 1;
    /** 审核状态: 驳回 */
    public static final int AUDIT_REJECT = 2;

    /** 点赞收藏类型: 点赞帖子 */
    public static final int LC_TYPE_LIKE_POST = 1;
    /** 点赞收藏类型: 点赞评论 */
    public static final int LC_TYPE_LIKE_COMMENT = 2;
    /** 点赞收藏类型: 收藏帖子 */
    public static final int LC_TYPE_COLLECT_POST = 3;

    /** 圈子状态: 下架 */
    public static final int CIRCLE_STATUS_OFFLINE = 0;
    /** 圈子状态: 正常(已上线) */
    public static final int CIRCLE_STATUS_NORMAL = 1;
    /** 圈子状态: 待审核 */
    public static final int CIRCLE_STATUS_PENDING = 2;
    /** 圈子状态: 已驳回 */
    public static final int CIRCLE_STATUS_REJECTED = 3;

    /** 圈子类型: 公开 */
    public static final int CIRCLE_TYPE_PUBLIC = 0;
    /** 圈子类型: 私密 */
    public static final int CIRCLE_TYPE_PRIVATE = 1;

    /** 圈子成员身份: 普通成员 */
    public static final int CIRCLE_MEMBER_NORMAL = 0;
    /** 圈子成员身份: 圈主 */
    public static final int CIRCLE_MEMBER_OWNER = 1;

    /** 消息类型: 文字 */
    public static final int MSG_TYPE_TEXT = 0;
    /** 消息类型: 图片 */
    public static final int MSG_TYPE_IMAGE = 1;
    /** 消息类型: 表情 */
    public static final int MSG_TYPE_EMOJI = 2;

    /** 系统消息类型: 回复通知 */
    public static final int SYS_MSG_TYPE_REPLY = 6;
    /** 系统消息类型: 举报结果通知 */
    public static final int SYS_MSG_TYPE_REPORT = 7;
    /** 系统消息类型: 审核结果通知 */
    public static final int SYS_MSG_TYPE_AUDIT = 8;

    /** 已读状态: 未读 */
    public static final int READ_STATUS_UNREAD = 0;
    /** 已读状态: 已读 */
    public static final int READ_STATUS_READ = 1;

    /** AI 生成标记: 手动 */
    public static final int AI_GENERATED_NO = 0;
    /** AI 生成标记: AI 生成 */
    public static final int AI_GENERATED_YES = 1;

    /** 举报对象: 帖子 */
    public static final int REPORT_TARGET_POST = 1;
    /** 举报对象: 评论 */
    public static final int REPORT_TARGET_COMMENT = 2;
    /** 举报对象: 私信 */
    public static final int REPORT_TARGET_MESSAGE = 3;
    /** 举报对象: 用户 */
    public static final int REPORT_TARGET_USER = 4;

    /** 举报工单状态: 待处理 */
    public static final int REPORT_STATUS_PENDING = 0;
    /** 举报工单状态: 已驳回 */
    public static final int REPORT_STATUS_REJECTED = 1;
    /** 举报工单状态: 已处理 */
    public static final int REPORT_STATUS_HANDLED = 2;

    /** AI 风险等级: 未评 */
    public static final int AI_RISK_NONE = 0;
    /** AI 风险等级: 低 */
    public static final int AI_RISK_LOW = 1;
    /** AI 风险等级: 中 */
    public static final int AI_RISK_MEDIUM = 2;
    /** AI 风险等级: 高 */
    public static final int AI_RISK_HIGH = 3;

    /** 注销冷静期天数 */
    public static final int CANCEL_COOLING_DAYS = 7;
    /** 单用户最大加入圈子数 */
    public static final int MAX_CIRCLE_JOIN = 20;
    /** 单帖最大图片数 */
    public static final int MAX_POST_IMAGES = 9;
    /** 视频最大时长(秒) */
    public static final int MAX_VIDEO_DURATION = 60;
}
