package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.SystemMsg;

/**
 * 系统消息服务接口
 */
public interface SystemMsgService {

    /**
     * 推送系统消息
     *
     * @param userId  接收用户ID
     * @param msgType 消息类型
     * @param content 消息内容
     */
    void push(Long userId, Integer msgType, String content);

    /**
     * 分页查询用户系统消息
     *
     * @param current 当前页
     * @param size    每页大小
     * @param userId  用户ID
     * @return 分页结果
     */
    Page<SystemMsg> pageByUser(long current, long size, Long userId);

    /**
     * 标记消息已读
     *
     * @param id 消息ID
     */
    void markRead(Long id);

    /**
     * 全部已读
     *
     * @param userId 用户ID
     */
    void markAllRead(Long userId);

    /**
     * 未读消息数
     *
     * @param userId 用户ID
     * @return 未读数
     */
    long unreadCount(Long userId);

    /**
     * 推送全员系统公告
     *
     * @param content 公告内容
     */
    void pushSystemNotice(String content);
}
