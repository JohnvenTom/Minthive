package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Comment;
import com.minthive.entity.LikeCollect;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.mapper.CommentMapper;
import com.minthive.mapper.LikeCollectMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.service.CommentService;
import com.minthive.service.SystemMsgService;
import com.minthive.websocket.MessageType;
import com.minthive.websocket.WsMessage;
import com.minthive.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    private final LikeCollectMapper likeCollectMapper;

    private final UserMapper userMapper;

    private final PostMapper postMapper;

    private final SystemMsgService systemMsgService;

    /**
     * 发表评论
     *
     * @param comment 评论实体
     * @return 评论实体
     */
    @Override
    public Comment publish(Comment comment) {
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        comment.setLikeCount(0);
        comment.setLiked(false);
        commentMapper.insert(comment);

        Long fromUserId = comment.getUserId();
        boolean isReply = comment.getParentId() != null && comment.getParentId() != 0L;

        if (isReply) {
            Comment parentComment = commentMapper.selectById(comment.getParentId());
            if (parentComment != null) {
                Long replyTargetUserId = comment.getReplyToId() != null
                        ? comment.getReplyToId() : parentComment.getUserId();
                if (!replyTargetUserId.equals(fromUserId)) {
                    User fromUser = userMapper.selectById(fromUserId);
                    String fromNickname = fromUser != null ? fromUser.getNickname() : "有人";
                    String preview = comment.getContent() != null && comment.getContent().length() > 50
                            ? comment.getContent().substring(0, 50) + "..." : comment.getContent();
                    String content = fromNickname + " 回复了你: " + (preview != null ? preview : "");
                    systemMsgService.push(replyTargetUserId, Constants.SYS_MSG_TYPE_REPLY, content, comment.getPostId());
                    WebSocketServer.sendToUser(replyTargetUserId,
                            WsMessage.of(MessageType.REPLY.getCode(), fromUserId, replyTargetUserId,
                                    Map.of("commentId", comment.getId(),
                                            "postId", comment.getPostId(),
                                            "content", preview)));
                }
            }
        } else {
            if (comment.getPostId() != null) {
                Post post = postMapper.selectById(comment.getPostId());
                if (post != null && !post.getUserId().equals(fromUserId)) {
                    User fromUser = userMapper.selectById(fromUserId);
                    String fromNickname = fromUser != null ? fromUser.getNickname() : "有人";
                    String preview = comment.getContent() != null && comment.getContent().length() > 50
                            ? comment.getContent().substring(0, 50) + "..." : comment.getContent();
                    String content = fromNickname + " 评论了你的动态: " + (preview != null ? preview : "");
                    systemMsgService.push(post.getUserId(), 2, content, comment.getPostId());
                    WebSocketServer.sendToUser(post.getUserId(),
                            WsMessage.of(MessageType.COMMENT.getCode(), fromUserId, post.getUserId(),
                                    Map.of("commentId", comment.getId(),
                                            "postId", comment.getPostId(),
                                            "content", preview)));
                }
            }
        }

        return comment;
    }

    /**
     * 分页查询帖子评论
     *
     * @param current 当前页
     * @param size    每页大小
     * @param postId  帖子ID
     * @param userId  当前登录用户ID（用于查询每条评论的 liked 状态，可为 null）
     * @return 分页结果（每条评论已填充 nickname、avatar、likeCount、liked）
     */
    @Override
    public Page<Comment> pageByPost(long current, long size, Long postId, Long userId) {
        LambdaQueryWrapper<Comment> countWrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getParentId, 0L);
        long total = commentMapper.selectCount(countWrapper);

        List<Comment> allComments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .select(Comment::getId, Comment::getPostId, Comment::getUserId,
                        Comment::getParentId, Comment::getReplyToId, Comment::getContent,
                        Comment::getImages, Comment::getAiGenerated, Comment::getCreateTime)
                .orderByAsc(Comment::getCreateTime));

        enrichListCommentLikeCounts(allComments);
        enrichListCommentUserInfo(allComments);
        enrichListCommentReplyTo(allComments);

        Map<Long, Comment> commentMap = new HashMap<>();
        for (Comment c : allComments) {
            commentMap.put(c.getId(), c);
        }
        List<Comment> roots = new ArrayList<>();
        for (Comment c : allComments) {
            if (c.getParentId() == null || c.getParentId() == 0L) {
                roots.add(c);
            } else {
                Comment parent = commentMap.get(c.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(c);
                }
            }
        }

        if (userId != null) {
            enrichListCommentLikedStatus(allComments, userId);
        }

        int fromIndex = (int) ((current - 1) * size);
        int toIndex = Math.min(fromIndex + (int) size, roots.size());
        List<Comment> pageRecords = fromIndex < roots.size() ? roots.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<Comment> result = new Page<>(current, size, total);
        result.setRecords(pageRecords);
        return result;
    }

    /**
     * 删除评论
     *
     * @param id     评论ID
     * @param userId 操作用户ID
     */
    @Override
    public void delete(Long id, Long userId) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除他人评论");
        }
        commentMapper.deleteById(id);
    }

    /**
     * 实时统计分页评论列表中每条评论的点赞数
     * 从 like_collect 表（type=2）实时查询，确保数据准确一致
     *
     * @param page 评论分页结果
     * @description 遍历分页结果中的每条评论，调用 enrichCommentLikeCount 查询真实点赞数
     */
    private void enrichListCommentLikeCounts(List<Comment> comments) {
        if (comments != null && !comments.isEmpty()) {
            for (Comment comment : comments) {
                enrichCommentLikeCount(comment);
            }
        }
    }

    /**
     * 实时统计单条评论的点赞数
     * 从 like_collect 表查询 type=2 且 target_id=该评论ID 的记录数
     *
     * @param comment 评论实体，查询结果会设置到 comment 的 likeCount 属性中
     * @description likeCount 为 @TableField(exist = false) 非持久化字段，
     *              仅用于 API 响应返回给前端展示，不写入数据库
     */
    private void enrichCommentLikeCount(Comment comment) {
        Long commentId = comment.getId();
        long likeCount = likeCollectMapper.selectCount(
                new LambdaQueryWrapper<LikeCollect>()
                        .eq(LikeCollect::getTargetId, commentId)
                        .eq(LikeCollect::getType, 2));
        comment.setLikeCount((int) likeCount);
    }

    /**
     * 批量填充分页评论列表中每条评论的评论者用户信息
     * 收集所有评论的 userId，批量查询 user 表，将 nickname 和 avatar 写入每条评论
     *
     * @param page 评论分页结果
     * @description nickname 和 avatar 为 Comment 实体的 @TableField(exist = false) 非持久化字段，
     *              仅用于 API 响应返回给前端展示
     */
    private void enrichListCommentUserInfo(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        for (Comment c : comments) {
            if (c.getUserId() != null) {
                userIds.add(c.getUserId());
            }
            if (c.getReplyToId() != null) {
                userIds.add(c.getReplyToId());
            }
        }
        Map<Long, User> userMap = batchGetUsers(userIds);
        for (Comment c : comments) {
            User user = userMap.get(c.getUserId());
            if (user != null) {
                c.setNickname(user.getNickname());
                c.setAvatar(user.getAvatar());
            } else {
                c.setNickname("未知用户");
                c.setAvatar("");
            }
            if (c.getReplyToId() != null) {
                User replyUser = userMap.get(c.getReplyToId());
                c.setReplyTo(replyUser != null ? replyUser.getNickname() : "未知用户");
            }
        }
    }

    /**
     * 批量查询用户信息（仅 id、nickname、avatar 字段）
     *
     * @param userIds 用户ID集合
     * @return userId -> User 的映射
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

    /**
     * 填充分页评论列表中子评论的 replyTo 字段
     * 对于 replyTo 为空的子评论（parentId != 0），从父评论作者信息中补充
     *
     * @param page 评论分页结果
     */
    private void enrichListCommentReplyTo(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        Map<Long, Comment> commentMap = new HashMap<>();
        for (Comment c : comments) {
            commentMap.put(c.getId(), c);
        }
        for (Comment c : comments) {
            if (c.getParentId() != null && c.getParentId() != 0L
                    && c.getReplyToId() == null && (c.getReplyTo() == null || c.getReplyTo().isEmpty())) {
                Comment parent = commentMap.get(c.getParentId());
                if (parent != null && parent.getNickname() != null) {
                    c.setReplyTo(parent.getNickname());
                }
            }
        }
    }

    /**
     * 批量填充评论列表中当前用户对每条评论的点赞状态
     */
    private void enrichListCommentLikedStatus(List<Comment> comments, Long userId) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        Set<Long> commentIds = new HashSet<>();
        for (Comment c : comments) {
            if (c.getId() != null) {
                commentIds.add(c.getId());
            }
        }
        Set<Long> likedCommentIds = new HashSet<>();
        if (!commentIds.isEmpty()) {
            LambdaQueryWrapper<LikeCollect> wrapper = new LambdaQueryWrapper<LikeCollect>()
                    .eq(LikeCollect::getUserId, userId)
                    .eq(LikeCollect::getType, Constants.LC_TYPE_LIKE_COMMENT)
                    .in(LikeCollect::getTargetId, commentIds)
                    .select(LikeCollect::getTargetId);
            List<LikeCollect> likes = likeCollectMapper.selectList(wrapper);
            for (LikeCollect lc : likes) {
                likedCommentIds.add(lc.getTargetId());
            }
        }
        for (Comment c : comments) {
            c.setLiked(likedCommentIds.contains(c.getId()));
        }
    }
}
