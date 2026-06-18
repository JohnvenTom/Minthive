package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Post;
import com.minthive.mapper.PostMapper;
import com.minthive.service.PostService;
import com.minthive.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 帖子服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;

    private final SensitiveWordService sensitiveWordService;

    /**
     * 发布帖子
     *
     * @param post 帖子实体
     * @return 发布后的帖子
     * @throws BusinessException 内容含敏感词时抛出
     */
    @Override
    public Post publish(Post post) {
        // 敏感词过滤
        if (sensitiveWordService.contains(post.getContent())) {
            throw new BusinessException(ResultCode.SENSITIVE_WORD, "帖子内容包含敏感词，请修改后发布");
        }
        post.setContent(sensitiveWordService.replace(post.getContent()));
        post.setAuditStatus(Constants.AUDIT_PASS);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        postMapper.insert(post);
        log.info("帖子发布成功: id={}, userId={}", post.getId(), post.getUserId());
        return post;
    }

    /**
     * 根据ID查询帖子
     *
     * @param id 帖子ID
     * @return 帖子实体
     */
    @Override
    public Post getById(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_EXISTS);
        }
        return post;
    }

    /**
     * 分页查询帖子
     *
     * @param current  当前页
     * @param size     每页大小
     * @param circleId 圈子ID
     * @return 分页结果
     */
    @Override
    public Page<Post> page(long current, long size, Long circleId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getAuditStatus, Constants.AUDIT_PASS)
                .eq(circleId != null, Post::getCircleId, circleId)
                .orderByDesc(Post::getCreateTime);
        return postMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 删除帖子
     *
     * @param id     帖子ID
     * @param userId 操作用户ID
     */
    @Override
    public void delete(Long id, Long userId) {
        Post post = getById(id);
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除他人帖子");
        }
        postMapper.deleteById(id);
    }

    /**
     * 审核帖子
     *
     * @param id     帖子ID
     * @param status 审核状态
     */
    @Override
    public void audit(Long id, Integer status) {
        Post update = new Post();
        update.setId(id);
        update.setAuditStatus(status);
        postMapper.updateById(update);
    }

    /**
     * 按关键词搜索帖子（内容模糊匹配）
     *
     * @param keyword 搜索关键词(可空，为空时返回全部已审核帖子)
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    @Override
    public Page<Post> searchByKeyword(String keyword, long current, long size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getAuditStatus, Constants.AUDIT_PASS);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Post::getContent, keyword);
        }
        wrapper.orderByDesc(Post::getCreateTime);
        return postMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 获取首页信息流（推荐/最新/最热）
     *
     * @param sortType 排序类型: latest(最新) / hot(最热)
     * @param current  当前页
     * @param size     每页大小
     * @return 分页帖子列表（仅已审核、公开的帖子）
     */
    @Override
    public Page<Post> feed(String sortType, long current, long size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getAuditStatus, Constants.AUDIT_PASS)
                .eq(Post::getVisibility, 0); // 仅公开帖子

        // 根据排序类型排序：hot按点赞数降序，latest按时间降序
        if ("hot".equals(sortType)) {
            wrapper.orderByDesc(Post::getLikeCount)
                  .orderByDesc(Post::getCreateTime);
        } else {
            // 默认按时间倒序
            wrapper.orderByDesc(Post::getCreateTime);
        }
        return postMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 更新帖子点赞数（+1 或 -1）
     *
     * @param postId 帖子ID
     * @param delta  变化量（正数增加，负数减少）
     */
    @Override
    public void updateLikeCount(Long postId, int delta) {
        Post update = new Post();
        update.setId(postId);
        // 防止计数变为负数
        Post existing = postMapper.selectById(postId);
        if (existing == null) return;
        int newVal = Math.max(0, existing.getLikeCount() + delta);
        update.setLikeCount(newVal);
        postMapper.updateById(update);
    }

    /**
     * 更新帖子收藏数（+1 或 -1）
     *
     * @param postId 帖子ID
     * @param delta  变化量（正数增加，负数减少）
     */
    @Override
    public void updateCollectCount(Long postId, int delta) {
        Post update = new Post();
        update.setId(postId);
        // 防止计数变为负数
        Post existing = postMapper.selectById(postId);
        if (existing == null) return;
        int newVal = Math.max(0, existing.getCollectCount() + delta);
        update.setCollectCount(newVal);
        postMapper.updateById(update);
    }
}
