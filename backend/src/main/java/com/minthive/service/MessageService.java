package com.minthive.service;

import com.minthive.entity.Message;

import java.util.List;

/**
 * 私信消息服务接口
 */
public interface MessageService {

    /**
     * 发送私信
     *
     * @param message 消息实体
     * @return 消息实体
     */
    Message send(Message message);

    /**
     * 查询两人聊天记录
     *
     * @param userId   当前用户ID
     * @param peerId   对方用户ID
     * @param limit    最大条数
     * @return 消息列表
     */
    List<Message> chatHistory(Long userId, Long peerId, Integer limit);

    /**
     * 标记消息已读
     *
     * @param fromUserId 发送方ID
     * @param toUserId   接收方ID
     */
    void markRead(Long fromUserId, Long toUserId);

    /**
     * 撤回 AI 代回复消息
     *
     * @param messageId 消息ID
     * @param userId    操作用户ID
     */
    void revokeAiMessage(Long messageId, Long userId);
}
