package com.minthive.websocket;

/**
 * WebSocket 消息类型枚举
 *
 * <p>功能描述：定义所有 WebSocket 推送的消息类型，前端按类型分发处理</p>
 */
public enum MessageType {

    /** 新评论推送 */
    COMMENT("comment"),

    /** 评论回复推送 */
    REPLY("reply"),

    /** 点赞提醒 */
    LIKE("like"),

    /** 收藏提醒 */
    COLLECT("collect"),

    /** 关注提醒 */
    FOLLOW("follow"),

    /** 私信消息 */
    CHAT("chat"),

    /** 系统公告 */
    SYSTEM("system"),

    /** 圈子公告 */
    CIRCLE_NOTICE("circle_notice"),

    /** 已读回执 */
    READ_RECEIPT("read_receipt"),

    /** 上线通知 */
    ONLINE("online"),

    /** 下线通知 */
    OFFLINE("offline");

    private final String code;

    MessageType(String code) {
        this.code = code;
    }

    /**
     * 获取消息类型编码
     *
     * @return 编码字符串
     */
    public String getCode() {
        return code;
    }
}
