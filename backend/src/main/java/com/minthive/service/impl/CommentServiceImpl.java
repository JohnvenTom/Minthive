package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.entity.Comment;
import com.minthive.entity.LikeCollect;
import com.minthive.mapper.CommentMapper;
import com.minthive.mapper.LikeCollectMapper;
import com.minthive.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评论服务实现
 */
@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    private final LikeCollectMapper likeCollectMapper;

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
     * @return 分页结果
     */
    @Override
    public Page<Comment> pageByPost(long current, long size, Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .orderByAsc(Comment::getCreateTime);
        Page<Comment> result = commentMapper.selectPage(new Page<>(current, size), wrapper);
        // 实时统计每条评论的点赞数（从 like_collect 表查询，不依赖冗余字段）
        enrichPageCommentLikeCounts(result);
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
}
