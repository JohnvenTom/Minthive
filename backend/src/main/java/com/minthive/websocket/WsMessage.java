package com.minthive.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * WebSocket 消息体
 *
 * <p>功能描述：统一封装 WebSocket 推送的消息结构</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage implements Serializable {

    /** 消息类型 */
    private String type;

    /** 发送者ID */
    private Long fromUserId;

    /** 接收者ID */
    private Long toUserId;

    /** 消息内容 */
    private Object content;

    /** 业务数据：前端按 type 分发后直接读取 data，避免调用方再解析 content */
    private Object data;

    /** 时间戳 */
    private Long timestamp;

    /**
     * 构建带业务数据的推送消息
     *
     * @param type 消息类型
     * @param data 业务数据（前端 handler 直接接收该对象）
     * @return WsMessage 实例
     */
    public static WsMessage ofData(String type, Object data) {
        WsMessage msg = new WsMessage();
        msg.setType(type);
        msg.setData(data);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    /**
     * 构建消息
     *
     * @param type       消息类型
     * @param fromUserId 发送者ID
     * @param toUserId   接收者ID
     * @param content    内容
     * @return WsMessage 实例
     */
    public static WsMessage of(String type, Long fromUserId, Long toUserId, Object content) {
        WsMessage msg = new WsMessage();
        msg.setType(type);
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setContent(content);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    /**
     * 构建系统公告消息
     *
     * @param content 公告内容
     * @return WsMessage 实例（类型为SYSTEM，无特定接收者）
     */
    public static WsMessage system(Object content) {
        return of(MessageType.SYSTEM.getCode(), null, null, content);
    }
}
