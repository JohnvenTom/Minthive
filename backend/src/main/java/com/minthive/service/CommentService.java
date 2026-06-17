package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Comment;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 发表评论
     *
     * @param comment 评论实体
     * @return 评论实体
     */
    Comment publish(Comment comment);

    /**
     * 分页查询帖子评论
     *
     * @param current 当前页
     * @param size    每页大小
     * @param postId  帖子ID
     * @return 分页结果
     */
    Page<Comment> pageByPost(long current, long size, Long postId);

    /**
     * 删除评论
     *
     * @param id     评论ID
     * @param userId 操作用户ID
     */
    void delete(Long id, Long userId);
}
