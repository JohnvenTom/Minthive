package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Constants;
import com.minthive.common.RedisConstants;
import com.minthive.entity.SystemMsg;
import com.minthive.mapper.SystemMsgMapper;
import com.minthive.service.SystemMsgService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 系统消息服务实现
 */
@RequiredArgsConstructor
@Service
public class SystemMsgServiceImpl implements SystemMsgService {

    private final SystemMsgMapper systemMsgMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 推送系统消息
     *
     * @param userId  接收用户ID
     * @param msgType 消息类型
     * @param content 消息内容
     */
    @Override
    public void push(Long userId, Integer msgType, String content) {
        SystemMsg msg = new SystemMsg();
        msg.setUserId(userId);
        msg.setMsgType(msgType);
        msg.setContent(content);
        msg.setIsRead(Constants.READ_STATUS_UNREAD);
        systemMsgMapper.insert(msg);
        // 更新未读数缓存
        String key = RedisConstants.UNREAD_MSG_PREFIX + userId;
        stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 分页查询用户系统消息
     *
     * @param current 当前页
     * @param size    每页大小
     * @param userId  用户ID
     * @return 分页结果
     */
    @Override
    public Page<SystemMsg> pageByUser(long current, long size, Long userId) {
        LambdaQueryWrapper<SystemMsg> wrapper = new LambdaQueryWrapper<SystemMsg>()
                .eq(SystemMsg::getUserId, userId)
                .orderByDesc(SystemMsg::getCreateTime);
        return systemMsgMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 标记消息已读
     *
     * @param id 消息ID
     */
    @Override
    public void markRead(Long id) {
        systemMsgMapper.update(null, new LambdaUpdateWrapper<SystemMsg>()
                .eq(SystemMsg::getId, id)
                .set(SystemMsg::getIsRead, Constants.READ_STATUS_READ));
    }

    /**
     * 全部已读
     *
     * @param userId 用户ID
     */
    @Override
    public void markAllRead(Long userId) {
        systemMsgMapper.update(null, new LambdaUpdateWrapper<SystemMsg>()
                .eq(SystemMsg::getUserId, userId)
                .eq(SystemMsg::getIsRead, Constants.READ_STATUS_UNREAD)
                .set(SystemMsg::getIsRead, Constants.READ_STATUS_READ));
        stringRedisTemplate.delete(RedisConstants.UNREAD_MSG_PREFIX + userId);
    }

    /**
     * 未读消息数
     *
     * @param userId 用户ID
     * @return 未读数
     */
    @Override
    public long unreadCount(Long userId) {
        return systemMsgMapper.selectCount(new LambdaQueryWrapper<SystemMsg>()
                .eq(SystemMsg::getUserId, userId)
                .eq(SystemMsg::getIsRead, Constants.READ_STATUS_UNREAD));
    }

    /**
     * 推送全员系统公告
     *
     * @param content 公告内容
     * @description 向所有用户推送系统公告消息，通过WebSocket实时广播
     */
    @Override
    public void pushSystemNotice(String content) {
        // 查询所有活跃用户ID并逐个推送系统消息
        // 实际生产环境应分批处理，此处简化实现
        com.minthive.websocket.WebSocketServer.broadcast(
                com.minthive.websocket.WsMessage.system(content)
        );
    }
}
