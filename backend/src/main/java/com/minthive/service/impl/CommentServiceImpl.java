package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Comment;
import com.minthive.entity.LikeCollect;
import com.minthive.entity.User;
import com.minthive.mapper.CommentMapper;
import com.minthive.mapper.LikeCollectMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.service.CommentService;
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
        commentMapper.insert(comment);
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
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .orderByAsc(Comment::getCreateTime);
        Page<Comment> result = commentMapper.selectPage(new Page<>(current, size), wrapper);
        enrichPageCommentLikeCounts(result);
        enrichPageCommentUserInfo(result);
        enrichPageCommentReplyTo(result);
        buildCommentTree(result);
        if (userId != null) {
            enrichPageCommentLikedStatus(result, userId);
        }
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
    private void enrichPageCommentLikeCounts(Page<Comment> page) {
        List<Comment> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            for (Comment comment : records) {
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
    private void enrichPageCommentUserInfo(Page<Comment> page) {
        List<Comment> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        for (Comment c : records) {
            if (c.getUserId() != null) {
                userIds.add(c.getUserId());
            }
            if (c.getReplyToId() != null) {
                userIds.add(c.getReplyToId());
            }
        }
        Map<Long, User> userMap = batchGetUsers(userIds);
        for (Comment c : records) {
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
    private void enrichPageCommentReplyTo(Page<Comment> page) {
        List<Comment> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, Comment> commentMap = new HashMap<>();
        for (Comment c : records) {
            commentMap.put(c.getId(), c);
        }
        for (Comment c : records) {
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
     * 将扁平评论列表构建为树形结构
     * parentId=0 的为一级评论，其余为子评论，挂载到对应父评论的 children 列表
     * 构建完成后只保留一级评论在分页结果中
     *
     * @param page 评论分页结果
     */
    private void buildCommentTree(Page<Comment> page) {
        List<Comment> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, Comment> commentMap = new HashMap<>();
        for (Comment c : records) {
            commentMap.put(c.getId(), c);
        }
        List<Comment> roots = new ArrayList<>();
        for (Comment c : records) {
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
        page.setRecords(roots);
    }

    /**
     * 批量填充分页评论列表中当前用户对每条评论的点赞状态
     * 批量查询 like_collect 表（type=2），将结果写入每条评论的 liked 字段
     *
     * @param page   评论分页结果
     * @param userId 当前登录用户ID
     * @description liked 为 Comment 实体的 @TableField(exist = false) 非持久化字段，
     *              仅用于 API 响应返回给前端展示，标识当前用户是否已点赞该评论
     */
    private void enrichPageCommentLikedStatus(Page<Comment> page, Long userId) {
        List<Comment> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> commentIds = new HashSet<>();
        collectAllCommentIds(records, commentIds);
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
        setLikedStatus(records, likedCommentIds);
    }

    private void collectAllCommentIds(List<Comment> comments, Set<Long> ids) {
        for (Comment c : comments) {
            if (c.getId() != null) {
                ids.add(c.getId());
            }
            if (c.getChildren() != null && !c.getChildren().isEmpty()) {
                collectAllCommentIds(c.getChildren(), ids);
            }
        }
    }

    private void setLikedStatus(List<Comment> comments, Set<Long> likedIds) {
        for (Comment c : comments) {
            c.setLiked(likedIds.contains(c.getId()));
            if (c.getChildren() != null && !c.getChildren().isEmpty()) {
                setLikedStatus(c.getChildren(), likedIds);
            }
        }
    }
}
