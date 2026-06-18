package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Message;
import com.minthive.mapper.MessageMapper;
import com.minthive.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 私信消息服务实现
 */
@RequiredArgsConstructor
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    /**
     * 发送私信
     *
     * @param message 消息实体
     * @return 消息实体
     */
    @Override
    public Message send(Message message) {
        message.setIsRead(Constants.READ_STATUS_UNREAD);
        if (message.getAiReply() == null) {
            message.setAiReply(Constants.AI_GENERATED_NO);
        }
        if (message.getMsgType() == null) {
            message.setMsgType(Constants.MSG_TYPE_TEXT);
        }
        messageMapper.insert(message);
        return message;
    }

    /**
     * 查询两人聊天记录
     *
     * @param userId 当前用户ID
     * @param peerId 对方用户ID
     * @param limit  最大条数
     * @return 消息列表
     */
    @Override
    public List<Message> chatHistory(Long userId, Long peerId, Integer limit) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .and(w -> w.eq(Message::getFromUserId, userId).eq(Message::getToUserId, peerId))
                .or(w -> w.eq(Message::getFromUserId, peerId).eq(Message::getToUserId, userId))
                .orderByDesc(Message::getCreateTime)
                .last(limit != null ? "LIMIT " + limit : "");
        return messageMapper.selectList(wrapper);
    }

    /**
     * 标记消息已读
     *
     * @param fromUserId 发送方ID
     * @param toUserId   接收方ID
     */
    @Override
    public void markRead(Long fromUserId, Long toUserId) {
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getFromUserId, fromUserId)
                .eq(Message::getToUserId, toUserId)
                .eq(Message::getIsRead, Constants.READ_STATUS_UNREAD)
                .set(Message::getIsRead, Constants.READ_STATUS_READ));
    }

    /**
     * 撤回 AI 代回复消息
     *
     * @param messageId 消息ID
     * @param userId    操作用户ID
     * @throws BusinessException 消息不存在或非AI消息时抛出
     */
    @Override
    public void revokeAiMessage(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (message.getAiReply() != Constants.AI_GENERATED_YES) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "仅可撤回AI代回复消息");
        }
        if (!message.getFromUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权撤回他人消息");
        }
        messageMapper.deleteById(messageId);
    }

    /**
     * 获取当前用户的私信会话列表
     *
     * <p>查询当前用户作为发送方或接收方的所有私信记录，按对方用户ID分组，
     * 取每组最新的一条消息作为会话代表，并统计未读数</p>
     *
     * @param userId 当前登录用户ID
     * @return 会话列表，每个Map包含：peerUser(对方用户对象)、latestMessage(最新消息内容)、unreadCount(未读数)
     */
    @Override
    public List<Map<String, Object>> getChatList(Long userId) {
        // 1. 查询当前用户参与的所有消息（发送或接收）
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .and(w -> w.eq(Message::getFromUserId, userId).or().eq(Message::getToUserId, userId))
                .orderByDesc(Message::getCreateTime);
        List<Message> allMessages = messageMapper.selectList(wrapper);

        if (allMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按对方用户ID分组，取每组最新的消息
        Map<Long, Message> latestByPeer = new LinkedHashMap<>();
        Map<Long, Integer> unreadCountByPeer = new HashMap<>();

        for (Message msg : allMessages) {
            Long peerId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            if (!latestByPeer.containsKey(peerId)) {
                latestByPeer.put(peerId, msg);
                // 统计未读：当前用户是接收方且消息未读
                unreadCountByPeer.put(peerId, 0);
            }
            // 更新未读计数
            if (msg.getToUserId().equals(userId) && msg.getIsRead().equals(Constants.READ_STATUS_UNREAD)) {
                unreadCountByPeer.merge(peerId, 1, Integer::sum);
            }
        }

        // 3. 组装会话列表
        List<Map<String, Object>> chatList = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : latestByPeer.entrySet()) {
            Long peerId = entry.getKey();
            Message latestMsg = entry.getValue();
            Map<String, Object> chat = new HashMap<>();
            chat.put("peerUserId", peerId);
            chat.put("latestMessage", latestMsg.getContent());
            chat.put("latestTime", latestMsg.getCreateTime());
            chat.put("unreadCount", unreadCountByPeer.getOrDefault(peerId, 0));
            chatList.add(chat);
        }

        return chatList;
    }

    /**
     * 获取通知列表（占位实现）
     *
     * <p>简化实现：返回空分页结果占位（通知系统可后续完善）</p>
     *
     * @param userId   当前用户ID
     * @param type     通知类型（可选），如 like/comment/circle/system
     * @param current  当前页码，默认1
     * @param size     每页条数，默认10
     * @return 空的分页通知列表
     */
    @Override
    public Page<Map<String, Object>> getNotificationList(Long userId, String type, long current, long size) {
        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        return page;
    }

    /**
     * 获取各类型未读消息统计（占位实现）
     *
     * <p>简化实现：返回全零占位，包含 like/comment/message/circle/system 各类型</p>
     *
     * @param userId 当前用户ID
     * @return 未读统计Map，各类型值均为0
     */
    @Override
    public Map<String, Integer> getUnreadCount(Long userId) {
        Map<String, Integer> unreadMap = new HashMap<>();
        unreadMap.put("like", 0);
        unreadMap.put("comment", 0);
        unreadMap.put("message", 0);
        unreadMap.put("circle", 0);
        unreadMap.put("system", 0);
        return unreadMap;
    }
}
