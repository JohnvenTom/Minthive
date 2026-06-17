package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.entity.Message;
import com.minthive.security.UserContext;
import com.minthive.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 查询聊天记录
     *
     * @param peerId 对方用户ID
     * @param limit  最大条数
     * @return 消息列表
     */
    @Operation(summary = "查询聊天记录")
    @GetMapping("/history")
    public Result<List<Message>> history(@RequestParam Long peerId,
                                         @RequestParam(defaultValue = "100") Integer limit) {
        return Result.success(messageService.chatHistory(UserContext.getUserId(), peerId, limit));
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
     * 撤回 AI 代回复消息
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @Operation(summary = "撤回AI代回复消息")
    @DeleteMapping("/revoke/{messageId}")
    public Result<Void> revoke(@PathVariable Long messageId) {
        messageService.revokeAiMessage(messageId, UserContext.getUserId());
        return Result.success();
    }
}
