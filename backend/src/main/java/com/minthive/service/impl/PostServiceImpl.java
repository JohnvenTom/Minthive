package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Comment;
import com.minthive.entity.LikeCollect;
import com.minthive.entity.Post;
import com.minthive.mapper.CommentMapper;
import com.minthive.mapper.LikeCollectMapper;
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

    private final CommentMapper commentMapper;

    private final LikeCollectMapper likeCollectMapper;

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
        enrichPostCounts(post);
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
        Page<Post> result = postMapper.selectPage(new Page<>(current, size), wrapper);
        enrichPageCounts(result);
        return result;
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
        Page<Post> result = postMapper.selectPage(new Page<>(current, size), wrapper);
        enrichPageCounts(result);
        return result;
    }

    /**
     * 获取首页信息流（推荐/最新/最热）
     *
     * <p>三种排序模式：</p>
     * <ul>
     *   <li>recommend: 综合推荐（按互动热度加权，兼顾时间新鲜度）</li>
     *   <li>latest: 最新发布（纯时间倒序）</li>
     *   <li>hot: 最热帖子（按点赞数降序，同赞时新帖优先）</li>
     * </ul>
     *
     * @param sortType 排序类型: recommend(推荐) / latest(最新) / hot(最热)
     * @param current  当前页
     * @param size     每页大小
     * @return 分页帖子列表（仅已审核、公开的帖子，已填充实时计数字段）
     */
    @Override
    public Page<Post> feed(String sortType, long current, long size) {
        Page<Post> result;

        if ("hot".equals(sortType)) {
            // 最热：从 like_collect 表子查询统计点赞数，按点赞数降序
            result = postMapper.selectHotFeed(new Page<>(current, size));
        } else if ("recommend".equals(sortType)) {
            // 推荐：综合排序（评论数权重 + 点赞数权重 + 时间衰减），使用自定义SQL
            result = postMapper.selectRecommendFeed(new Page<>(current, size));
        } else {
            // 最新：默认按发布时间倒序
            LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                    .eq(Post::getAuditStatus, Constants.AUDIT_PASS)
                    .eq(Post::getVisibility, 0)
                    .orderByDesc(Post::getCreateTime);
            result = postMapper.selectPage(new Page<>(current, size), wrapper);
        }

        enrichPageCounts(result);
        return result;
    }

    /**
     * 转发帖子
     *
     * <p>功能描述：创建一条新帖，内容引用原帖，sharePostId 指向原帖ID，
     * 审核状态设为通过（AUDIT_PASS）</p>
     *
     * @param post       转发时的附加内容（可为空）
     * @param sharePostId 原帖ID
     * @param userId     转发用户ID
     * @return 创建的转发帖子
     * @throws BusinessException 原帖不存在时抛出
     */
    @Override
    public Post share(Post post, Long sharePostId, Long userId) {
        // 校验原帖是否存在
        Post original = postMapper.selectById(sharePostId);
        if (original == null) {
            throw new BusinessException(ResultCode.POST_NOT_EXISTS);
        }
        // 构建转发帖
        post.setUserId(userId);
        post.setSharePostId(sharePostId);
        post.setAuditStatus(Constants.AUDIT_PASS);
        // 如果没有附加内容，使用原帖内容作为引用
        if (post.getContent() == null || post.getContent().trim().isEmpty()) {
            post.setContent("转发: " + original.getContent());
        }
        postMapper.insert(post);
        log.info("帖子转发成功: postId={}, originalPostId={}, userId={}", post.getId(), sharePostId, userId);
        return post;
    }

    /**
     * 保存草稿
     *
     * <p>功能描述：将帖子以草稿形式保存（auditStatus=0），不进行敏感词审核</p>
     *
     * @param post   帖子实体（内容、图片等）
     * @param userId 发布用户ID
     * @return 保存后的草稿帖子
     */
    @Override
    public Post saveDraft(Post post, Long userId) {
        post.setUserId(userId);
        post.setAuditStatus(Constants.AUDIT_PENDING); // 0 = 草稿/待审
        postMapper.insert(post);
        log.info("草稿保存成功: postId={}, userId={}", post.getId(), userId);
        return post;
    }

    /**
     * 查询当前用户的草稿列表
     *
     * <p>功能描述：查询指定用户所有 auditStatus=0 的草稿帖子，按更新时间倒序排列</p>
     *
     * @param userId 用户ID
     * @return 草稿帖子列表
     */
    @Override
    public java.util.List<Post> getDraftList(Long userId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getAuditStatus, Constants.AUDIT_PENDING)
                .orderByDesc(Post::getUpdateTime);
        return postMapper.selectList(wrapper);
    }

    // ========== 实时统计计数（从关联表动态计算）==========

    /**
     * 实时统计单个帖子的互动数据
     *
     * @param post 帖子实体
     * @description 从 comment 表和 like_collect 表实时查询评论数、点赞数、收藏数，
     * 覆盖 post 表中的冗余计数字段，确保数据准确一致
     */
    private void enrichPostCounts(Post post) {
        Long postId = post.getId();
        // 评论数：comment 表中未逻辑删除的记录数
        long commentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, postId));
        // 点赞数：like_collect 表中 type=1(点赞帖子) 的记录数
        long likeCount = likeCollectMapper.selectCount(
                new LambdaQueryWrapper<LikeCollect>().eq(LikeCollect::getTargetId, postId).eq(LikeCollect::getType, 1));
        // 收藏数：like_collect 表中 type=3(收藏帖子) 的记录数
        long collectCount = likeCollectMapper.selectCount(
                new LambdaQueryWrapper<LikeCollect>().eq(LikeCollect::getTargetId, postId).eq(LikeCollect::getType, 3));

        post.setCommentCount((int) commentCount);
        post.setLikeCount((int) likeCount);
        post.setCollectCount((int) collectCount);
    }

    /**
     * 实时分页帖子列表的互动数据
     *
     * @param page 分页结果
     * @description 遍历分页结果中的每条帖子，调用 enrichPostCounts 实时统计
     */
    private void enrichPageCounts(Page<Post> page) {
        if (page.getRecords() != null) {
            for (Post post : page.getRecords()) {
                enrichPostCounts(post);
            }
        }
    }
}
