package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Message;
import com.minthive.mapper.MessageMapper;
import com.minthive.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
