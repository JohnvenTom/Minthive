package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Post;

/**
 * 帖子服务接口
 */
public interface PostService {

    /**
     * 发布帖子
     *
     * @param post 帖子实体
     * @return 发布后的帖子
     */
    Post publish(Post post);

    /**
     * 根据ID查询帖子
     *
     * @param id 帖子ID
     * @return 帖子实体
     */
    Post getById(Long id);

    /**
     * 分页查询帖子
     *
     * @param current  当前页
     * @param size     每页大小
     * @param circleId 圈子ID(可空)
     * @return 分页结果
     */
    Page<Post> page(long current, long size, Long circleId);

    /**
     * 删除帖子
     *
     * @param id     帖子ID
     * @param userId 操作用户ID
     */
    void delete(Long id, Long userId);

    /**
     * 审核帖子
     *
     * @param id     帖子ID
     * @param status 审核状态
     */
    void audit(Long id, Integer status);

    /**
     * 按关键词搜索帖子（内容模糊匹配）
     *
     * @param keyword 搜索关键词(可空)
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    Page<Post> searchByKeyword(String keyword, long current, long size);
}
