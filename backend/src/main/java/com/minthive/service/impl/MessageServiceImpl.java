package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.*;
import com.minthive.mapper.*;
import com.minthive.service.MessageService;
import com.minthive.websocket.MessageType;
import com.minthive.websocket.WebSocketServer;
import com.minthive.websocket.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 私信消息与通知服务实现
 *
 * <p>私信功能：发送、聊天记录、已读标记、AI代回复撤回、会话列表</p>
 * <p>通知功能：聚合点赞/评论/关注/圈子/系统 五类通知，支持类型筛选与分页</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final LikeCollectMapper likeCollectMapper;
    private final FollowMapper followMapper;
    private final SystemMsgMapper systemMsgMapper;
    private final NotificationReadMapper notificationReadMapper;
    private final com.minthive.config.BusinessMetrics businessMetrics;

    private final com.minthive.service.ActionRateLimiter actionRateLimiter;

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
        if (message.getCreateTime() == null) {
            message.setCreateTime(LocalDateTime.now());
        }
        actionRateLimiter.reserve(com.minthive.service.ActionRateLimiter.Action.MESSAGE, message.getFromUserId());
        try {
            messageMapper.insert(message);
        } catch (RuntimeException e) {
            actionRateLimiter.release(com.minthive.service.ActionRateLimiter.Action.MESSAGE, message.getFromUserId());
            throw e;
        }
        pushChat(message);
        return message;
    }

    /**
     * 向接收方推送新私信
     *
     * <p>功能描述：把持久化后的消息连同发送者昵称、头像一起推给接收方，
     * 接收方前端据此实时追加消息并更新会话列表</p>
     *
     * @param message 已入库的消息
     * @return 无返回值
     * @throws 无异常抛出：推送失败仅记录日志，不影响消息已入库的事实
     * @note 推送不是可靠投递，接收方离线时依赖下拉历史记录补齐
     */
    private void pushChat(Message message) {
        try {
            User sender = userMapper.selectById(message.getFromUserId());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", message.getId());
            payload.put("fromUserId", message.getFromUserId());
            payload.put("toUserId", message.getToUserId());
            payload.put("content", message.getContent());
            payload.put("msgType", message.getMsgType());
            payload.put("isRead", message.getIsRead());
            payload.put("aiReply", message.getAiReply());
            payload.put("createTime", message.getCreateTime());
            payload.put("fromNickname", sender == null ? null : sender.getNickname());
            payload.put("fromAvatar", sender == null ? null : sender.getAvatar());
            WebSocketServer.sendToUser(message.getToUserId(),
                    WsMessage.ofData(MessageType.CHAT.getCode(), payload));
        } catch (Exception e) {
            businessMetrics.messagePushFailed();
            log.warn("私信实时推送失败 messageId={}", message.getId(), e);
        }
    }

    /**
     * 查询两人聊天记录（游标分页）
     *
     * <p>功能描述：按消息ID倒序取一页，传入 beforeId 时只取更早的消息，
     * 避免深翻页时 LIMIT OFFSET 的扫描成本，也避免新旧消息插入导致错页/重复</p>
     *
     * @param userId   当前用户ID
     * @param peerId   对方用户ID
     * @param beforeId 游标（上一页最旧一条消息的ID），null 或 0 表示最新一页
     * @param limit    每页条数，默认30，最大100
     * @return 按ID降序排列的消息列表
     * @note 会话双方必须是当前用户与对方，查询条件已限定，不存在越权读取第三人会话
     */
    @Override
    public List<Message> chatHistory(Long userId, Long peerId, Long beforeId, Integer limit) {
        int pageSize = limit == null ? 30 : Math.max(1, Math.min(limit, 100));
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .and(w -> w.and(x -> x.eq(Message::getFromUserId, userId).eq(Message::getToUserId, peerId))
                        .or(x -> x.eq(Message::getFromUserId, peerId).eq(Message::getToUserId, userId)))
                .lt(beforeId != null && beforeId > 0, Message::getId, beforeId)
                .orderByDesc(Message::getId)
                .last("LIMIT " + pageSize);
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
        // 回执给发送方：对方已读，发送方前端可刷新“已读/未读”标记
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("peerId", toUserId);
            payload.put("read", true);
            WebSocketServer.sendToUser(fromUserId,
                    WsMessage.ofData(MessageType.READ_RECEIPT.getCode(), payload));
        } catch (Exception e) {
            businessMetrics.messagePushFailed();
            log.warn("已读回执推送失败 from={} to={}", fromUserId, toUserId, e);
        }
    }

    /**
     * 标记单条通知已读
     *
     * <p>功能描述：点赞/评论/关注通知没有自己的已读字段，写入 notification_read 记录；
     * 回复/圈子/系统/举报/审核通知本身存放在 system_msg，直接更新其 is_read</p>
     *
     * @param userId 当前登录用户ID
     * @param type   通知类型（like/comment/follow/reply/circle/system/report/audit）
     * @param refId  通知记录ID：点赞/评论/关注为业务表主键，其余为 system_msg 主键
     * @return 无返回值
     * @throws BusinessException 参数为空或类型不支持时抛出
     * @note 重复标记为幂等操作，不会产生重复记录
     */
    @Override
    public void markNotificationRead(Long userId, String type, Long refId) {
        if (userId == null || refId == null || type == null || type.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "通知类型和ID不能为空");
        }
        switch (type) {
            case "like", "comment", "follow" ->
                    notificationReadMapper.insertIgnore(userId, type, refId);
            case "reply", "circle", "system", "report", "audit" ->
                    // 只允许更新属于自己的系统消息，避免越权标记他人通知
                    systemMsgMapper.update(null, new LambdaUpdateWrapper<SystemMsg>()
                            .eq(SystemMsg::getId, refId)
                            .eq(SystemMsg::getUserId, userId)
                            .set(SystemMsg::getIsRead, Constants.READ_STATUS_READ));
            default -> throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的通知类型");
        }
    }

    /**
     * 回填点赞/评论/关注通知的已读状态
     *
     * <p>功能描述：一次性查询当前用户的全部已读记录，按 type:refId 匹配通知并置为已读；
     * system_msg 类通知已在构建时读取自身 is_read，此处不覆盖</p>
     *
     * @param userId        当前登录用户ID
     * @param notifications 待回填的通知列表（原地修改 read 字段）
     * @return 无返回值
     * @note 已读记录量级与用户通知量级相当，单次查询可接受；列表为空时直接返回
     */
    private void applyReadState(Long userId, List<Map<String, Object>> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        List<NotificationRead> reads = notificationReadMapper.selectList(
                new LambdaQueryWrapper<NotificationRead>()
                        .eq(NotificationRead::getUserId, userId)
                        .select(NotificationRead::getType, NotificationRead::getRefId));
        if (reads.isEmpty()) {
            return;
        }
        Set<String> readKeys = reads.stream()
                .map(r -> r.getType() + ":" + r.getRefId())
                .collect(Collectors.toSet());
        for (Map<String, Object> notice : notifications) {
            if (!Boolean.TRUE.equals(notice.get("read"))) {
                notice.put("read", readKeys.contains(notice.get("type") + ":" + notice.get("id")));
            }
        }
    }

    /**
     * 统计某类型通知的已读记录数
     *
     * @param userId 当前登录用户ID
     * @param type   通知类型（like/comment/follow）
     * @return 已读记录数量
     * @note 用于未读数 = 事件总数 - 已读数，已读记录可能多于现存事件（取消点赞/删除评论）时由调用方截断为0
     */
    private long countReadNotification(Long userId, String type) {
        return notificationReadMapper.selectCount(new LambdaQueryWrapper<NotificationRead>()
                .eq(NotificationRead::getUserId, userId)
                .eq(NotificationRead::getType, type));
    }

    /**
     * 撤回消息（支持普通消息和AI代回复消息）
     *
     * @param messageId 消息ID
     * @param userId    操作用户ID
     * @throws BusinessException 消息不存在或无权操作时抛出
     */
    @Override
    public void revokeAiMessage(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (!message.getFromUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权撤回他人消息");
        }
        messageMapper.deleteById(messageId);
        // 通知接收方实时移除该消息，避免双方视图不一致
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messageId", messageId);
            payload.put("fromUserId", message.getFromUserId());
            WebSocketServer.sendToUser(message.getToUserId(),
                    WsMessage.ofData(MessageType.REVOKE.getCode(), payload));
        } catch (Exception e) {
            businessMetrics.messagePushFailed();
            log.warn("撤回消息推送失败 messageId={}", messageId, e);
        }
    }

    /**
     * 获取当前用户的私信会话列表
     *
     * <p>查询当前用户作为发送方或接收方的所有私信记录，按对方用户ID分组，
     * 取每组最新的一条消息作为会话代表，并统计未读数。
     * 返回字段与前端 ChatItem 接口对齐：userId/nickname/avatar/lastMessage/lastTime/unread</p>
     *
     * @param userId 当前登录用户ID
     * @return 会话列表，每个Map包含前端所需的字段
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

        // 2. 按对方用户ID分组，取每组最新的消息，统计未读数
        Map<Long, Message> latestByPeer = new LinkedHashMap<>();
        Map<Long, Integer> unreadCountByPeer = new HashMap<>();

        for (Message msg : allMessages) {
            Long peerId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            if (!latestByPeer.containsKey(peerId)) {
                latestByPeer.put(peerId, msg);
                unreadCountByPeer.put(peerId, 0);
            }
            // 统计未读：当前用户是接收方且消息未读
            if (msg.getToUserId().equals(userId) && msg.getIsRead().equals(Constants.READ_STATUS_UNREAD)) {
                unreadCountByPeer.merge(peerId, 1, Integer::sum);
            }
        }

        // 3. 批量查询对方用户信息（昵称、头像）
        Set<Long> peerIds = latestByPeer.keySet();
        Map<Long, User> userMap = batchGetUsers(peerIds);

        // 4. 组装会话列表（字段名与前端 ChatItem 接口对齐）
        List<Map<String, Object>> chatList = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : latestByPeer.entrySet()) {
            Long peerId = entry.getKey();
            Message latestMsg = entry.getValue();
            User peerUser = userMap.get(peerId);

            Map<String, Object> chat = new HashMap<>();
            chat.put("userId", peerId);
            chat.put("nickname", peerUser != null ? peerUser.getNickname() : "未知用户");
            chat.put("avatar", peerUser != null ? peerUser.getAvatar() : "");
            chat.put("lastMessage", latestMsg.getContent());
            chat.put("lastTime", latestMsg.getCreateTime());
            chat.put("unread", unreadCountByPeer.getOrDefault(peerId, 0));
            chatList.add(chat);
        }

        return chatList;
    }

    /**
     * 获取通知列表
     *
     * <p>聚合五类通知数据：点赞(like)、评论(comment)、关注(follow)、圈子(circle)、系统(system)。
     * 点赞/评论/关注从各自的业务表实时计算，圈子/系统从 system_msg 表读取。
     * 支持按类型筛选，统一按时间倒序分页返回</p>
     *
     * @param userId   当前用户ID
     * @param type     通知类型（可选），如 like/comment/follow/circle/system，空字符串表示全部
     * @param current  当前页码，默认1
     * @param size     每页条数，默认10
     * @return 分页通知列表，每条通知包含 id/type/fromUserId/fromNickname/fromAvatar/targetId/content/read/createTime
     */
    @Override
    public Page<Map<String, Object>> getNotificationList(Long userId, String type, long page, long size) {
        List<Map<String, Object>> allNotifications = new ArrayList<>();

        // ---------- 1. 点赞通知：别人点赞了我的帖子 ----------
        if (type == null || type.isEmpty() || "like".equals(type)) {
            allNotifications.addAll(buildLikeNotifications(userId));
        }

        // ---------- 2. 评论通知：别人评论了我的帖子 + 回复了我的评论 ----------
        if (type == null || type.isEmpty() || "comment".equals(type) || "reply".equals(type)) {
            allNotifications.addAll(buildCommentNotifications(userId));
        }

        // ---------- 3. 关注通知：别人关注了我 ----------
        if (type == null || type.isEmpty() || "follow".equals(type)) {
            allNotifications.addAll(buildFollowNotifications(userId));
        }

        // ---------- 4. 圈子通知：来自 system_msg (msgType=4) ----------
        if (type == null || type.isEmpty() || "circle".equals(type)) {
            allNotifications.addAll(buildSystemMsgNotifications(userId, 4, "circle"));
        }

        // ---------- 5. 系统通知：来自 system_msg (msgType=5) ----------
        if (type == null || type.isEmpty() || "system".equals(type)) {
            allNotifications.addAll(buildSystemMsgNotifications(userId, 5, "system"));
        }

        // ---------- 6. 举报结果通知：来自 system_msg (msgType=7) ----------
        if (type == null || type.isEmpty() || "report".equals(type)) {
            allNotifications.addAll(buildSystemMsgNotifications(userId, Constants.SYS_MSG_TYPE_REPORT, "report"));
        }

        // ---------- 7. 审核结果通知：来自 system_msg (msgType=8) ----------
        if (type == null || type.isEmpty() || "audit".equals(type)) {
            allNotifications.addAll(buildSystemMsgNotifications(userId, Constants.SYS_MSG_TYPE_AUDIT, "audit"));
        }

        // ---------- 回填点赞/评论/关注通知的已读态 ----------
        applyReadState(userId, allNotifications);

        // ---------- 全局排序（按时间倒序）----------
        allNotifications.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("createTime");
            LocalDateTime tb = (LocalDateTime) b.get("createTime");
            return tb.compareTo(ta);
        });

        // ---------- 手动分页 ----------
        long total = allNotifications.size();
        int fromIndex = (int) ((page - 1) * size);
        int toIndex = Math.min((int) (fromIndex + size), allNotifications.size());
        List<Map<String, Object>> pageRecords = fromIndex < allNotifications.size()
                ? allNotifications.subList(fromIndex, toIndex)
                : Collections.emptyList();

        Page<Map<String, Object>> result = new Page<>(page, size);
        result.setRecords(pageRecords);
        result.setTotal(total);
        return result;
    }

    /**
     * 获取各类型未读消息统计
     *
     * <p>实时统计五类通知的未读数量：
     * like: 别人对我帖子的最新点赞数
     * comment: 别人对我帖子的最新评论数
     * follow: 新关注者数量（简化为全部关注记录数）
     * message: 私信未读数
     * circle: 圈子公告未读数
     * system: 系统公告未读数</p>
     *
     * @param userId 当前用户ID
     * @return 未读统计Map，包含 like/comment/message/circle/system 各类型的未读数
     */
    @Override
    public Map<String, Integer> getUnreadCount(Long userId) {
        Map<String, Integer> unreadMap = new HashMap<>();

        // 1. 私信未读：查询 message 表中当前用户接收的未读消息
        long messageUnread = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getToUserId, userId)
                        .eq(Message::getIsRead, Constants.READ_STATUS_UNREAD));
        unreadMap.put("message", (int) messageUnread);

        // 2. 点赞未读：点赞事件数 - 已标记已读的数量（取消点赞后可能为负，需截断为0）
        List<Long> myPostIds = getMyPostIds(userId);
        if (!myPostIds.isEmpty()) {
            long likeTotal = likeCollectMapper.selectCount(
                    new LambdaQueryWrapper<LikeCollect>()
                            .in(LikeCollect::getTargetId, myPostIds)
                            .eq(LikeCollect::getType, Constants.LC_TYPE_LIKE_POST)
                            .ne(LikeCollect::getUserId, userId));
            unreadMap.put("like", (int) Math.max(0, likeTotal - countReadNotification(userId, "like")));
        } else {
            unreadMap.put("like", 0);
        }

        // 3. 评论未读：帖子评论事件数 - 已读数量 + 别人回复了我评论的系统通知未读数
        long postCommentUnread = 0;
        if (!myPostIds.isEmpty()) {
            long postCommentTotal = commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>()
                            .in(Comment::getPostId, myPostIds)
                            .ne(Comment::getUserId, userId));
            postCommentUnread = Math.max(0, postCommentTotal - countReadNotification(userId, "comment"));
        }
        long replyUnread = systemMsgMapper.selectCount(
                new LambdaQueryWrapper<SystemMsg>()
                        .eq(SystemMsg::getUserId, userId)
                        .eq(SystemMsg::getMsgType, Constants.SYS_MSG_TYPE_REPLY)
                        .eq(SystemMsg::getIsRead, Constants.READ_STATUS_UNREAD));
        unreadMap.put("comment", (int) (postCommentUnread + replyUnread));

        // 4. 关注未读：关注我的人数 - 已标记已读的数量
        long followTotal = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowUserId, userId));
        unreadMap.put("follow", (int) Math.max(0, followTotal - countReadNotification(userId, "follow")));

        // 5. 圈子通知未读：system_msg 中 msgType=4 且 isRead=0
        long circleUnread = systemMsgMapper.selectCount(
                new LambdaQueryWrapper<SystemMsg>()
                        .eq(SystemMsg::getUserId, userId)
                        .eq(SystemMsg::getMsgType, 4)
                        .eq(SystemMsg::getIsRead, Constants.READ_STATUS_UNREAD));
        unreadMap.put("circle", (int) circleUnread);

        // 6. 系统通知未读：system_msg 中 msgType=5 且 isRead=0
        long systemUnread = systemMsgMapper.selectCount(
                new LambdaQueryWrapper<SystemMsg>()
                        .eq(SystemMsg::getUserId, userId)
                        .eq(SystemMsg::getMsgType, 5)
                        .eq(SystemMsg::getIsRead, Constants.READ_STATUS_UNREAD));
        unreadMap.put("system", (int) systemUnread);

        // 7. 举报结果未读：system_msg 中 msgType=7 且 isRead=0
        long reportUnread = systemMsgMapper.selectCount(
                new LambdaQueryWrapper<SystemMsg>()
                        .eq(SystemMsg::getUserId, userId)
                        .eq(SystemMsg::getMsgType, Constants.SYS_MSG_TYPE_REPORT)
                        .eq(SystemMsg::getIsRead, Constants.READ_STATUS_UNREAD));
        unreadMap.put("report", (int) reportUnread);

        // 8. 审核结果未读：system_msg 中 msgType=8 且 isRead=0
        long auditUnread = systemMsgMapper.selectCount(
                new LambdaQueryWrapper<SystemMsg>()
                        .eq(SystemMsg::getUserId, userId)
                        .eq(SystemMsg::getMsgType, Constants.SYS_MSG_TYPE_AUDIT)
                        .eq(SystemMsg::getIsRead, Constants.READ_STATUS_UNREAD));
        unreadMap.put("audit", (int) auditUnread);

        return unreadMap;
    }

    // ==================== 通知构建私有方法 ====================

    /**
     * 构建点赞通知列表
     *
     * <p>查询 like_collect 表中别人对当前用户帖子的点赞记录，
     * 关联获取点赞者昵称/头像和被点赞的帖子ID</p>
     *
     * @param userId 当前用户ID
     * @return 点赞通知Map列表
     */
    private List<Map<String, Object>> buildLikeNotifications(Long userId) {
        List<Map<String, Object>> notifications = new ArrayList<>();

        // ---------- 1) 帖子点赞通知 ----------
        List<Long> myPostIds = getMyPostIds(userId);
        if (!myPostIds.isEmpty()) {
            LambdaQueryWrapper<LikeCollect> postLikeWrapper = new LambdaQueryWrapper<LikeCollect>()
                    .in(LikeCollect::getTargetId, myPostIds)
                    .eq(LikeCollect::getType, Constants.LC_TYPE_LIKE_POST)
                    .ne(LikeCollect::getUserId, userId)
                    .orderByDesc(LikeCollect::getCreateTime)
                    .last("LIMIT 100");
            List<LikeCollect> postLikes = likeCollectMapper.selectList(postLikeWrapper);
            notifications.addAll(buildLikeNoticeItems(postLikes, "赞了你的动态", null));
        }

        // ---------- 2) 评论点赞通知 ----------
        List<Long> myCommentIds = getMyCommentIds(userId);
        if (!myCommentIds.isEmpty()) {
            LambdaQueryWrapper<LikeCollect> commentLikeWrapper = new LambdaQueryWrapper<LikeCollect>()
                    .in(LikeCollect::getTargetId, myCommentIds)
                    .eq(LikeCollect::getType, Constants.LC_TYPE_LIKE_COMMENT)
                    .ne(LikeCollect::getUserId, userId)
                    .orderByDesc(LikeCollect::getCreateTime)
                    .last("LIMIT 100");
            List<LikeCollect> commentLikes = likeCollectMapper.selectList(commentLikeWrapper);
            // 批量查询被点赞评论所属的帖子ID，用于跳转到正确的帖子详情页
            Map<Long, Long> commentToPostMap = getCommentPostMap(commentLikes);
            notifications.addAll(buildLikeNoticeItems(commentLikes, "赞了你的评论", commentToPostMap));
        }

        return notifications;
    }

    /**
     * 构建评论通知列表
     *
     * <p>查询 comment 表中别人对当前用户帖子的评论记录，
     * 关联获取评论者信息和评论内容</p>
     *
     * @param userId 当前用户ID
     * @return 评论通知Map列表
     */
    private List<Map<String, Object>> buildCommentNotifications(Long userId) {
        List<Long> myPostIds = getMyPostIds(userId);
        List<Map<String, Object>> notifications = new ArrayList<>();

        // 1) 别人对我帖子的评论
        if (!myPostIds.isEmpty()) {
            LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                    .in(Comment::getPostId, myPostIds)
                    .ne(Comment::getUserId, userId)
                    .orderByDesc(Comment::getCreateTime)
                    .last("LIMIT 100");
            List<Comment> comments = commentMapper.selectList(wrapper);

            if (!comments.isEmpty()) {
                Set<Long> commenterIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
                Map<Long, User> userMap = batchGetUsers(commenterIds);

                for (Comment c : comments) {
                    User commenter = userMap.get(c.getUserId());
                    Map<String, Object> notice = new HashMap<>();
                    notice.put("id", c.getId());
                    notice.put("type", "comment");
                    notice.put("fromUserId", c.getUserId());
                    notice.put("fromNickname", commenter != null ? commenter.getNickname() : "未知用户");
                    notice.put("fromAvatar", commenter != null ? commenter.getAvatar() : "");
                    notice.put("targetId", c.getPostId());
                    String preview = c.getContent() != null && c.getContent().length() > 50
                            ? c.getContent().substring(0, 50) + "..." : c.getContent();
                    notice.put("content", preview != null ? preview : "评论了你的动态");
                    notice.put("read", false);
                    notice.put("createTime", c.getCreateTime());
                    notifications.add(notice);
                }
            }
        }

        // 2) 别人回复了我评论的通知（来自 system_msg, msgType=6）
        List<SystemMsg> replyMsgs = systemMsgMapper.selectList(
                new LambdaQueryWrapper<SystemMsg>()
                        .eq(SystemMsg::getUserId, userId)
                        .eq(SystemMsg::getMsgType, Constants.SYS_MSG_TYPE_REPLY)
                        .orderByDesc(SystemMsg::getCreateTime)
                        .last("LIMIT 50"));
        for (SystemMsg msg : replyMsgs) {
            Map<String, Object> notice = new HashMap<>();
            notice.put("id", msg.getId());
            notice.put("type", "reply");
            notice.put("fromUserId", 0L);
            notice.put("fromNickname", "");
            notice.put("fromAvatar", "");
            notice.put("targetId", msg.getTargetId() != null ? msg.getTargetId() : 0L);
            notice.put("content", msg.getContent() != null ? msg.getContent() : "回复了你");
            notice.put("read", msg.getIsRead() == Constants.READ_STATUS_READ);
            notice.put("createTime", msg.getCreateTime());
            notifications.add(notice);
        }

        return notifications;
    }

    /**
     * 构建关注通知列表
     *
     * <p>查询 follow 表中新关注当前用户的记录，关联获取关注者信息</p>
     *
     * @param userId 当前用户ID（被关注方）
     * @return 关注通知Map列表
     */
    private List<Map<String, Object>> buildFollowNotifications(Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowUserId, userId)
                .orderByDesc(Follow::getCreateTime)
                .last("LIMIT 100");
        List<Follow> follows = followMapper.selectList(wrapper);

        if (follows.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取关注者信息
        Set<Long> followerIds = follows.stream().map(Follow::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = batchGetUsers(followerIds);

        List<Map<String, Object>> notifications = new ArrayList<>();
        for (Follow f : follows) {
            User follower = userMap.get(f.getUserId());
            Map<String, Object> notice = new HashMap<>();
            notice.put("id", f.getId());
            notice.put("type", "follow");
            notice.put("fromUserId", f.getUserId());
            notice.put("fromNickname", follower != null ? follower.getNickname() : "未知用户");
            notice.put("fromAvatar", follower != null ? follower.getAvatar() : "");
            notice.put("targetId", f.getUserId());
            notice.put("content", "关注了你");
            notice.put("read", false);
            notice.put("createTime", f.getCreateTime());
            notifications.add(notice);
        }
        return notifications;
    }

    /**
     * 构建 system_msg 类型的通知（圈子公告 / 系统公告）
     *
     * <p>从 system_msg 表按 msgType 筛选，直接使用表中的已读状态</p>
     *
     * @param userId  接收用户ID
     * @param msgType 消息类型（4=圈子公告, 5=系统公告）
     * @param typeStr 前端通知类型标识（"circle" / "system"）
     * @return 通知Map列表
     */
    private List<Map<String, Object>> buildSystemMsgNotifications(Long userId, int msgType, String typeStr) {
        LambdaQueryWrapper<SystemMsg> wrapper = new LambdaQueryWrapper<SystemMsg>()
                .eq(SystemMsg::getUserId, userId)
                .eq(SystemMsg::getMsgType, msgType)
                .orderByDesc(SystemMsg::getCreateTime)
                .last("LIMIT 100");
        List<SystemMsg> msgs = systemMsgMapper.selectList(wrapper);

        if (msgs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> notifications = new ArrayList<>();
        for (SystemMsg msg : msgs) {
            Map<String, Object> notice = new HashMap<>();
            notice.put("id", msg.getId());
            notice.put("type", typeStr);
            notice.put("fromUserId", 0);
            notice.put("fromNickname", "系统");
            notice.put("fromAvatar", "");
            notice.put("targetId", msg.getTargetId() != null ? msg.getTargetId() : 0);
            notice.put("content", msg.getContent());
            notice.put("read", Objects.equals(msg.getIsRead(), Constants.READ_STATUS_READ));
            notice.put("createTime", msg.getCreateTime());
            notifications.add(notice);
        }
        return notifications;
    }

    /**
     * 获取当前用户发布的所有帖子ID列表
     *
     * @param userId 用户ID
     * @return 帖子ID列表，无帖子时返回空列表
     */
    private List<Long> getMyPostIds(Long userId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .select(Post::getId);
        List<Post> posts = postMapper.selectList(wrapper);
        return posts.stream().map(Post::getId).collect(Collectors.toList());
    }

    /**
     * 查询当前用户的所有评论ID
     *
     * @param userId 当前用户ID
     * @return 当前用户发布的所有评论ID列表
     */
    private List<Long> getMyCommentIds(Long userId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .select(Comment::getId);
        List<Comment> comments = commentMapper.selectList(wrapper);
        return comments.stream().map(Comment::getId).collect(Collectors.toList());
    }

    /**
     * 通用点赞通知条目构建方法
     *
     * <p>将点赞记录列表转换为通知Map列表，批量查询点赞者信息后填充昵称和头像。
     * 对于评论点赞通知（type=2），通过 commentToPostMap 将评论ID转换为帖子ID，
     * 确保前端点击通知时能跳转到正确的帖子详情页</p>
     *
     * @param likes            点赞记录列表
     * @param content          通知文案（如"赞了你的动态"、"赞了你的评论"）
     * @param commentToPostMap 评论ID→帖子ID的映射（评论点赞时传入，帖子点赞时传null即可）
     * @return 通知Map列表
     */
    private List<Map<String, Object>> buildLikeNoticeItems(List<LikeCollect> likes, String content,
                                                            Map<Long, Long> commentToPostMap) {
        if (likes.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量获取点赞者信息
        Set<Long> likerIds = likes.stream().map(LikeCollect::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = batchGetUsers(likerIds);

        List<Map<String, Object>> notifications = new ArrayList<>();
        for (LikeCollect lc : likes) {
            User liker = userMap.get(lc.getUserId());
            // 评论点赞时：targetId 需要映射为帖子ID；帖子点赞时：直接使用 targetId
            Long targetId = lc.getTargetId();
            if (commentToPostMap != null && commentToPostMap.containsKey(targetId)) {
                targetId = commentToPostMap.get(targetId);
            }
            Map<String, Object> notice = new HashMap<>();
            notice.put("id", lc.getId());
            notice.put("type", "like");
            notice.put("fromUserId", lc.getUserId());
            notice.put("fromNickname", liker != null ? liker.getNickname() : "未知用户");
            notice.put("fromAvatar", liker != null ? liker.getAvatar() : "");
            notice.put("targetId", targetId);
            notice.put("content", content);
            notice.put("read", false);
            notice.put("createTime", lc.getCreateTime());
            notifications.add(notice);
        }
        return notifications;
    }

    /**
     * 批量查询评论所属的帖子ID映射
     *
     * <p>根据点赞记录中的评论ID集合，批量查询每条评论对应的帖子ID，
     * 用于构建评论点赞通知时的跳转目标</p>
     *
     * @param commentLikes 评论点赞记录列表（targetId 为评论ID）
     * @return 评论ID→帖子ID 的映射
     */
    private Map<Long, Long> getCommentPostMap(List<LikeCollect> commentLikes) {
        if (commentLikes == null || commentLikes.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> commentIds = commentLikes.stream()
                .map(LikeCollect::getTargetId)
                .collect(Collectors.toSet());
        List<Comment> comments = commentMapper.selectBatchIds(commentIds);
        return comments.stream()
                .collect(Collectors.toMap(Comment::getId, Comment::getPostId));
    }

    /**
     * 批量获取用户信息
     *
     * @param userIds 用户ID集合
     * @return 用户ID → 用户实体 的映射，不存在的ID不包含在结果中
     */
    private Map<Long, User> batchGetUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .in(User::getId, userIds)
                .select(User::getId, User::getNickname, User::getAvatar);
        List<User> users = userMapper.selectList(wrapper);
        return users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }
}
