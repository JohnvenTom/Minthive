package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Message;
import com.minthive.security.UserContext;
import com.minthive.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 私信消息控制器
 */
@Tag(name = "私信接口")
@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final MessageService messageService;

    /**
     * 构造器注入 MessageService
     *
     * @param messageService 消息服务
     */
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 发送私信
     *
     * @param message 消息实体
     * @return 消息实体
     */
    @Operation(summary = "发送私信")
    @PostMapping
    public Result<Message> send(@RequestBody Message message) {
        message.setFromUserId(UserContext.getUserId());
        return Result.success(messageService.send(message));
    }

    /**
     * 查询聊天记录（游标分页）
     *
     * @param peerId   对方用户ID
     * @param beforeId 游标：只返回ID小于该值的更早消息，不传表示加载最新一页
     * @param limit    每页条数
     * @return 按ID降序排列的消息列表（前端反转后正序渲染）
     */
    @Operation(summary = "查询聊天记录")
    @GetMapping("/history")
    public Result<List<Message>> history(@RequestParam Long peerId,
                                         @RequestParam(required = false) Long beforeId,
                                         @RequestParam(defaultValue = "30") Integer limit) {
        return Result.success(messageService.chatHistory(UserContext.getUserId(), peerId, beforeId, limit));
    }

    /**
     * 标记消息已读
     *
     * @param fromUserId 发送方ID
     * @return 操作结果
     */
    @Operation(summary = "标记消息已读")
    @PostMapping("/read")
    public Result<Void> markRead(@RequestParam Long fromUserId) {
        messageService.markRead(fromUserId, UserContext.getUserId());
        return Result.success();
    }

    /**
     * 撤回消息
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @Operation(summary = "撤回消息")
    @DeleteMapping("/revoke/{messageId}")
    public Result<Void> revoke(@PathVariable Long messageId) {
        messageService.revokeAiMessage(messageId, UserContext.getUserId());
        return Result.success();
    }

    /**
     * 获取会话列表
     *
     * <p>返回当前用户的所有私信会话，每个会话包含对方用户信息、最新消息和未读数</p>
     *
     * @return 会话列表
     */
    @Operation(summary = "获取会话列表")
    @GetMapping("/chats")
    public Result<List<Map<String, Object>>> chatList() {
        return Result.success(messageService.getChatList(UserContext.getUserId()));
    }

    /**
     * 获取通知列表
     *
     * <p>简化实现：返回空列表占位（通知系统可后续完善）</p>
     *
     * @param type     通知类型（可选），如 like/comment/circle/system
     * @param current  当前页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 分页通知列表
     */
    @Operation(summary = "获取通知列表")
    @GetMapping("/notifications")
    public Result<Page<Map<String, Object>>> notifications(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(messageService.getNotificationList(UserContext.getUserId(), type, page, pageSize));
    }

    /**
     * 标记单条通知已读
     *
     * <p>点赞/评论/关注通知写入已读记录，回复/圈子/系统/举报/审核通知更新 system_msg 的已读状态</p>
     *
     * @param type 通知类型（like/comment/follow/reply/circle/system/report/audit）
     * @param id   通知记录ID
     * @return 操作结果
     */
    @Operation(summary = "标记通知已读")
    @PostMapping("/notifications/read")
    public Result<Void> markNotificationRead(@RequestParam String type, @RequestParam Long id) {
        messageService.markNotificationRead(UserContext.getUserId(), type, id);
        return Result.success();
    }

    /**
     * 获取未读消息统计
     *
     * <p>返回各类型未读数：like/comment/message/circle/system</p>
     *
     * @return 未读统计Map
     */
    @Operation(summary = "获取未读消息统计")
    @GetMapping("/unread")
    public Result<Map<String, Integer>> unreadCount() {
        return Result.success(messageService.getUnreadCount(UserContext.getUserId()));
    }
}
