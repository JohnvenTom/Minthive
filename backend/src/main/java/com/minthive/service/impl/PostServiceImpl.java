package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Circle;
import com.minthive.entity.Comment;
import com.minthive.entity.LikeCollect;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.mapper.CircleMapper;
import com.minthive.mapper.CommentMapper;
import com.minthive.mapper.LikeCollectMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.security.UserContext;
import com.minthive.ai.AiContext;
import com.minthive.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;

    private final CircleMapper circleMapper;

    private final UserMapper userMapper;

    private final AiContext aiContext;

    private final CommentMapper commentMapper;

    private final LikeCollectMapper likeCollectMapper;

    private final com.minthive.service.InterestVectorService interestVectorService;

    private final com.minthive.service.TagMappingService tagMappingService;

    private final com.minthive.util.RedisUtil redisUtil;

    private final com.minthive.service.ContentAccessService contentAccessService;

    private final com.minthive.service.ActionRateLimiter actionRateLimiter;

    private final com.minthive.ai.ImageModerationService imageModerationService;

    /**
     * 发布帖子
     *
     * @param post 帖子实体
     * @return 发布后的帖子
     */
    @Override
    public Post publish(Post post) {
        prepareNewPost(post, post.getUserId());
        post.setSharePostId(null);
        actionRateLimiter.reserve(com.minthive.service.ActionRateLimiter.Action.POST, post.getUserId());
        try {
            post.setAuditStatus(moderate(post));
            postMapper.insert(post);
        } catch (RuntimeException e) {
            actionRateLimiter.release(com.minthive.service.ActionRateLimiter.Action.POST, post.getUserId());
            throw e;
        }
        return post;
    }

    private void prepareNewPost(Post post, Long userId) {
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        if (post.getContent() == null || post.getContent().isBlank() || post.getContent().length() > 10000) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "内容不能为空且不能超过10000字");
        }
        post.setId(null);
        post.setUserId(userId);
        post.setDeleted(0);
        post.setCreateTime(null);
        post.setUpdateTime(null);
        if (post.getVisibility() == null) post.setVisibility(0);
        validateVisibility(post.getVisibility());
        if (post.getCircleId() != null && post.getCircleId() == 0L) post.setCircleId(null);
        contentAccessService.requireCanPublish(post.getCircleId(), userId);
    }

    private void validateVisibility(Integer visibility) {
        if (visibility == null || visibility < 0 || visibility > 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的可见范围");
        }
    }

    private int moderate(Post post) {
        // 视频内容审核尚未接入，一律转人工
        if (post.getVideo() != null && !post.getVideo().isBlank()) {
            return Constants.AUDIT_PENDING;
        }
        boolean hasImages = post.getImages() != null && !post.getImages().isBlank()
                && !"[]".equals(post.getImages());
        try {
            var result = hasImages
                    ? imageModerationService.moderate(post.getContent(), post.getImages())
                    : aiContext.detectContent(post.getContent(), null);
            return result != null && result.available && !result.violation
                    ? Constants.AUDIT_PASS : Constants.AUDIT_PENDING;
        } catch (Exception e) {
            log.warn("内容检测不可用，转入人工审核");
            return Constants.AUDIT_PENDING;
        }
    }

    /**
     * 根据ID查询帖子
     *
     * @param id 帖子ID
     * @return 帖子实体
     */
    @Override
    public Post getById(Long id) {
        return getById(id, contentAccessService.currentUserId());
    }

    /**
     * 根据ID查询帖子（带当前用户）
     *
     * @param id             帖子ID
     * @param currentUserId  当前登录用户ID（可能为 null）
     * @return 帖子实体
     */
    @Override
    public Post getById(Long id, Long currentUserId) {
        Post post = postMapper.selectById(id);
        contentAccessService.requireReadable(post, currentUserId);
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
        Long viewerId = contentAccessService.currentUserId();
        if (circleId != null) contentAccessService.requireCircleReadable(circleId, viewerId);
        LambdaQueryWrapper<Post> wrapper = contentAccessService.visiblePosts(viewerId)
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
        Post post = getById(id, userId);
        if (post.getUserId().equals(userId)) {
            postMapper.deleteById(id);
            return;
        }
        if (post.getCircleId() != null) {
            Circle circle = circleMapper.selectById(post.getCircleId());
            if (circle != null && userId.equals(circle.getOwnerId())) {
                postMapper.deleteById(id);
                return;
            }
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权删除他人帖子");
    }

    /**
     * 编辑帖子（修改内容、图片、可见性）
     *
     * <p>功能描述：校验操作权限后，更新帖子的 content/images/visibility 字段，
     * 不允许修改 userId/sharePostId/createTime 等核心字段</p>
     *
     * @param id     帖子ID
     * @param post   包含更新字段的帖子实体
     * @param userId 操作用户ID
     * @return 更新后的完整帖子
     */
    @Override
    public Post update(Long id, Post post, Long userId) {
        Post existing = getById(id, userId);
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权编辑他人帖子");
        }
        boolean contentChanged = post.getContent() != null && !post.getContent().equals(existing.getContent());
        boolean mediaChanged = post.getImages() != null && !post.getImages().equals(existing.getImages());
        if (post.getContent() != null) existing.setContent(post.getContent());
        if (post.getImages() != null) existing.setImages(post.getImages());
        if (post.getVisibility() != null) {
            validateVisibility(post.getVisibility());
            existing.setVisibility(post.getVisibility());
        }
        if (contentChanged || mediaChanged) {
            existing.setAuditStatus(moderate(existing));
            existing.setUpdateTime(null);
        }
        postMapper.updateById(existing);
        return existing;
    }

    /**
     * 切换帖子可见性
     *
     * <p>功能描述：校验权限后直接修改 visibility 字段，
     * 支持公开(0)/仅粉丝(1)/仅自己(隐藏)(2)三种状态切换</p>
     *
     * @param id         帖子ID
     * @param visibility 目标可见性值
     * @param userId     操作用户ID
     * @return 更新后的帖子
     */
    @Override
    public Post toggleVisibility(Long id, Integer visibility, Long userId) {
        Post post = getById(id, userId);
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人帖子");
        }
        validateVisibility(visibility);
        post.setVisibility(visibility);
        postMapper.updateById(post);
        return post;
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
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.length() > 100) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "搜索词过长");
        }
        LambdaQueryWrapper<Post> wrapper = contentAccessService.visiblePosts(contentAccessService.currentUserId());
        if (!normalized.isEmpty()) {
            wrapper.like(Post::getContent, normalized);
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
     *   <li>recommend: 个性化推荐（有行为数据时按兴趣向量排序，否则降级为综合热度）</li>
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
            result = postMapper.selectHotFeed(new Page<>(current, size));
        } else if ("recommend".equals(sortType)) {
            Long userId = null;
            try {
                userId = com.minthive.security.UserContext.getUserId();
            } catch (Exception ignored) {}

            if (userId != null) {
                result = personalizedFeed(userId, current, size);
                if (result != null) {
                    enrichPageCounts(result);
                    return result;
                }
            }
            result = postMapper.selectRecommendFeed(new Page<>(current, size));
        } else {
            LambdaQueryWrapper<Post> wrapper = contentAccessService.visiblePosts(null)
                    .orderByDesc(Post::getCreateTime);
            result = postMapper.selectPage(new Page<>(current, size), wrapper);
        }

        enrichPageCounts(result);
        return result;
    }

    /**
     * 个性化推荐：有相似标签的帖子排前面
     */
    private Page<Post> personalizedFeed(Long userId, long current, long size) {
        // 获取用户的兴趣标签
        User user = userMapper.selectById(userId);
        Set<String> userTags = user != null && user.getInterestTags() != null && !user.getInterestTags().isBlank()
                ? new HashSet<>(Arrays.asList(user.getInterestTags().split(",")))
                : Collections.emptySet();

        String cacheKey = "minthive:feed:personalized:" + userId;
        List<Long> cachedIds = null;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            try { cachedIds = (List<Long>) cached; } catch (Exception ignored) {}
        }

        if (cachedIds == null || cachedIds.isEmpty()) {
            List<Post> candidates = postMapper.selectCandidatePool(200);
            if (candidates.isEmpty()) return null;

            // 按标签匹配数降序 → 热度降序 → 时间降序
            cachedIds = candidates.stream()
                .map(p -> {
                    long match = userTags.isEmpty() ? 0 :
                        tagMappingService.resolvePostTags(p).stream().filter(userTags::contains).count();
                    long pop = (p.getLikeCount() != null ? p.getLikeCount() : 0) * 1
                             + (p.getCommentCount() != null ? p.getCommentCount() : 0) * 2
                             + (p.getCollectCount() != null ? p.getCollectCount() : 0) * 3;
                    return new AbstractMap.SimpleEntry<>(p, new long[]{match, pop});
                })
                .sorted((a, b) -> {
                    long[] va = a.getValue(), vb = b.getValue();
                    int cmp = Long.compare(vb[0], va[0]); // 匹配数
                    if (cmp != 0) return cmp;
                    cmp = Long.compare(vb[1], va[1]);     // 热度
                    if (cmp != 0) return cmp;
                    return b.getKey().getCreateTime().compareTo(a.getKey().getCreateTime()); // 时间
                })
                .map(e -> e.getKey().getId())
                .collect(Collectors.toList());

            redisUtil.set(cacheKey, cachedIds, 5, java.util.concurrent.TimeUnit.MINUTES);
        }

        int total = cachedIds.size();
        int from = (int) ((current - 1) * size);
        int to = Math.min(from + (int) size, total);
        if (from >= total) return new Page<>(current, size, 0);

        List<Post> records = postMapper.selectByIdsOrdered(cachedIds.subList(from, to));
        if (records == null) records = Collections.emptyList();
        records = records.stream().filter(p -> contentAccessService.canRead(p, userId)).collect(Collectors.toList());
        Page<Post> page = new Page<>(current, size, total);
        page.setRecords(records);
        return page;
    }

    private void enrichPageCountsForList(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Post> page = new Page<>();
        page.setRecords(posts);
        enrichPageCounts(page);
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
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        Post original = postMapper.selectById(sharePostId);
        if (original == null || !contentAccessService.isPubliclyShareable(original)) {
            throw new BusinessException(ResultCode.POST_NOT_EXISTS);
        }
        String content = post.getContent();
        if (content == null || content.isBlank()) content = "转发: " + original.getContent();
        Post shared = new Post();
        shared.setUserId(userId);
        shared.setContent(content);
        shared.setSharePostId(sharePostId);
        shared.setVisibility(0);
        shared.setCircleId(null);
        actionRateLimiter.reserve(com.minthive.service.ActionRateLimiter.Action.POST, userId);
        try {
            shared.setAuditStatus(moderate(shared));
            postMapper.insert(shared);
        } catch (RuntimeException e) {
            actionRateLimiter.release(com.minthive.service.ActionRateLimiter.Action.POST, userId);
            throw e;
        }
        return shared;
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

    /**
     * 获取帖子转发链（被转发列表）
     *
     * <p>功能描述：查询所有 sharePostId 指向当前帖子的转发帖，
     * 批量查询 user 表填充转发者的昵称和头像，支持分页</p>
     *
     * @param postId  原帖ID
     * @param current 当前页码
     * @param size    每页大小
     * @return 分页的转发帖子列表（已填充用户昵称、头像等关联数据）
     */
    @Override
    public Page<Post> getShareChain(Long postId, long current, long size) {
        Post original = postMapper.selectById(postId);
        Long viewerId = contentAccessService.currentUserId();
        contentAccessService.requireReadable(original, viewerId);
        LambdaQueryWrapper<Post> wrapper = contentAccessService.visiblePosts(viewerId)
                .eq(Post::getSharePostId, postId)
                .eq(Post::getAuditStatus, Constants.AUDIT_PASS)
                .orderByDesc(Post::getCreateTime);
        Page<Post> result = postMapper.selectPage(new Page<>(current, size), wrapper);

        // 2. 填充实时统计数据
        enrichPageCounts(result);

        // 3. 批量查询用户信息并填充昵称、头像
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            java.util.Set<Long> userIds = result.getRecords().stream()
                    .map(Post::getUserId)
                    .collect(java.util.stream.Collectors.toSet());
            log.debug("[ShareChain] 查询用户ID集合: {}", userIds);
            if (!userIds.isEmpty()) {
                java.util.List<User> users = userMapper.selectBatchIds(userIds);
                log.debug("[ShareChain] 查询到用户数: {}", users.size());
                java.util.Map<Long, User> userMap = users.stream()
                        .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
                for (Post post : result.getRecords()) {
                    User user = userMap.get(post.getUserId());
                    if (user != null) {
                        post.setNickname(user.getNickname());
                        post.setAvatar(user.getAvatar());
                        log.debug("[ShareChain] 填充 postId={} userId={} nickname={} avatar={}",
                                post.getId(), post.getUserId(), user.getNickname(),
                                user.getAvatar() != null ? "(有值)" : "(空)");
                    } else {
                        log.warn("[ShareChain] 未找到用户! postId={} userId={}", post.getId(), post.getUserId());
                    }
                }
            }
        }

        return result;
    }

    // ========== 实时统计计数（从关联表动态计算）==========

    /**
     * 实时统计单个帖子的互动数据
     *
     * @param post 帖子实体
     * @description 从 comment 表和 like_collect 表实时查询评论数、点赞数、收藏数，
     * 以及当前登录用户的点赞/收藏状态，覆盖 post 表中的冗余计数字段，确保数据准确一致
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
        // 转发数：post 表中 share_post_id = 当前帖子ID 且已审核通过的记录数
        long shareCount = postMapper.selectCount(
                new LambdaQueryWrapper<Post>().eq(Post::getSharePostId, postId).eq(Post::getAuditStatus, Constants.AUDIT_PASS));

        post.setCommentCount((int) commentCount);
        post.setLikeCount((int) likeCount);
        post.setCollectCount((int) collectCount);
        post.setShareCount((int) shareCount);

        // 填充发布者昵称和头像
        enrichUserInfo(post);

        // 填充当前用户的点赞/收藏状态（已登录时才查询）
        enrichUserInteractionStatus(post);
    }

    /**
     * 填充帖子的发布者信息（昵称、头像）
     *
     * @param post 帖子实体
     * @description 根据 post.userId 查询 user 表，将 nickname 和 avatar 填充到 Post 的非持久化字段中。
     *              如果 userId 为空或查询不到用户，则跳过
     */
    private void enrichUserInfo(Post post) {
        if (post.getUserId() == null) {
            return;
        }
        try {
            User user = userMapper.selectById(post.getUserId());
            if (user != null) {
                post.setNickname(user.getNickname());
                post.setAvatar(user.getAvatar());
            }
        } catch (Exception e) {
            log.warn("[Enrich] 填充用户信息失败 postId={} userId={}: {}", post.getId(), post.getUserId(), e.getMessage());
        }
    }

    /**
     * 填充当前用户对指定帖子的点赞/收藏状态
     *
     * <p>功能描述：查询 like_collect 表，判断当前登录用户是否已点赞或已收藏该帖子，
     * 将结果写入 Post 的 liked/collected 临时字段</p>
     *
     * <p>实现策略（双通道获取用户ID）：</p>
     * <ol>
     *   <li>优先从 UserContext 获取（适用于经过 JWT 拦截器的认证接口）</li>
     *   <li>UserContext 不可用时，从 HTTP 请求头手动解析 Token 获取用户ID
     *       （适用于 /api/post/feed 等公开接口——这些接口跳过了 JWT 拦截器，
     *       但前端仍会在请求头携带 Token）</li>
     * </ol>
     *
     * @param post 帖子实体（会直接修改其 liked/collected 字段）
     */
    private void enrichUserInteractionStatus(Post post) {
        // 身份统一来自已经完整校验过的 UserContext，不再自行解析请求凭证。
        Long userId = UserContext.getUserId();
        if (userId == null) {
            post.setLiked(false);
            post.setCollected(false);
            return;
        }

        Long postId = post.getId();

        // 查询当前用户是否已点赞（type=1）
        boolean liked = likeCollectMapper.selectCount(
                new LambdaQueryWrapper<LikeCollect>()
                        .eq(LikeCollect::getUserId, userId)
                        .eq(LikeCollect::getTargetId, postId)
                        .eq(LikeCollect::getType, 1)) > 0;
        post.setLiked(liked);

        // 查询当前用户是否已收藏（type=3）
        boolean collected = likeCollectMapper.selectCount(
                new LambdaQueryWrapper<LikeCollect>()
                        .eq(LikeCollect::getUserId, userId)
                        .eq(LikeCollect::getTargetId, postId)
                        .eq(LikeCollect::getType, 3)) > 0;
        post.setCollected(collected);
    }

    /**
     * 实时分页帖子列表的互动数据
     *
     * @param page 分页结果
     * @description 遍历分页结果中的每条帖子，调用 enrichPostCounts 实时统计
     */
    @Override
    public void enrichPageCounts(Page<Post> page) {
        if (page.getRecords() != null) {
            for (Post post : page.getRecords()) {
                enrichPostCounts(post);
            }
        }
    }
}
