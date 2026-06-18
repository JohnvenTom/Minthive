package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Message;

import java.util.List;
import java.util.Map;

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

    /**
     * 获取当前用户的私信会话列表
     *
     * <p>查询当前用户参与的所有私信会话，每个会话包含对方用户信息、最新消息和未读数</p>
     *
     * @param userId 当前登录用户ID
     * @return 会话列表，每个元素包含对方用户信息、最新消息内容、未读消息数
     */
    List<Map<String, Object>> getChatList(Long userId);

    /**
     * 获取通知列表
     *
     * <p>聚合五类通知数据（点赞/评论/关注/圈子/系统），支持按类型筛选和分页</p>
     *
     * @param userId   当前用户ID
     * @param type     通知类型（可选），如 like/comment/follow/circle/system，空字符串表示全部
     * @param page     当前页码，默认1
     * @param size     每页条数，默认10
     * @return 分页通知列表，每条通知包含 id/type/fromUserId/fromNickname/fromAvatar/targetId/content/read/createTime
     */
    Page<Map<String, Object>> getNotificationList(Long userId, String type, long page, long size);

    /**
     * 获取各类型未读消息统计
     *
     * <p>实时统计五类通知的未读数量：like/comment/follow/message/circle/system</p>
     *
     * @param userId 当前用户ID
     * @return 未读统计Map，键为类型名(like/comment/follow/message/circle/system)，值为未读数量
     */
    Map<String, Integer> getUnreadCount(Long userId);
}
