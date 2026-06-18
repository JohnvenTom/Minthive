package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.entity.Comment;
import com.minthive.entity.Post;
import com.minthive.mapper.CommentMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 评论服务实现
 */
@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    private final PostMapper postMapper;

    /**
     * 发表评论
     *
     * @param comment 评论实体
     * @return 评论实体
     */
    @Override
    public Comment publish(Comment comment) {
        comment.setLikeCount(0);
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        commentMapper.insert(comment);
        // 同步更新帖子的评论计数 +1
        incrementPostCommentCount(comment.getPostId());
        return comment;
    }

    /**
     * 分页查询帖子评论
     *
     * @param current 当前页
     * @param size    每页大小
     * @param postId  帖子ID
     * @return 分页结果
     */
    @Override
    public Page<Comment> pageByPost(long current, long size, Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .orderByAsc(Comment::getCreateTime);
        return commentMapper.selectPage(new Page<>(current, size), wrapper);
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
        // 同步更新帖子的评论计数 -1
        decrementPostCommentCount(comment.getPostId());
    }

    /**
     * 增加帖子评论计数
     *
     * @param postId 帖子ID
     * @description 将指定帖子的 commentCount 字段 +1，防止负数
     */
    private void incrementPostCommentCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            Post update = new Post();
            update.setId(postId);
            update.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(update);
        }
    }

    /**
     * 减少帖子评论计数
     *
     * @param postId 帖子ID
     * @description 将指定帖子的 commentCount 字段 -1，防止负数
     */
    private void decrementPostCommentCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            Post update = new Post();
            update.setId(postId);
            update.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            postMapper.updateById(update);
        }
    }
}
