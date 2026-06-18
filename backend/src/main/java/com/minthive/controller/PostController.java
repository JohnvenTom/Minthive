package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.common.Constants;
import com.minthive.entity.Post;
import com.minthive.security.UserContext;
import com.minthive.service.LikeCollectService;
import com.minthive.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帖子控制器
 */
@Tag(name = "帖子接口")
@RestController
@RequestMapping("/api/post")
public class PostController {

    private final PostService postService;
    private final LikeCollectService likeCollectService;

    /**
     * 构造器注入
     *
     * @param postService        帖子服务
     * @param likeCollectService 点赞收藏服务
     */
    public PostController(PostService postService, LikeCollectService likeCollectService) {
        this.postService = postService;
        this.likeCollectService = likeCollectService;
    }

    /**
     * 发布帖子
     *
     * @param post 帖子实体
     * @return 发布后的帖子
     */
    @Operation(summary = "发布帖子")
    @PostMapping
    public Result<Post> publish(@RequestBody Post post) {
        post.setUserId(UserContext.getUserId());
        return Result.success(postService.publish(post));
    }

    /**
     * 查询帖子详情
     *
     * @param id 帖子ID
     * @return 帖子详情
     */
    @Operation(summary = "查询帖子详情")
    @GetMapping("/{id}")
    public Result<Post> get(@PathVariable Long id) {
        return Result.success(postService.getById(id));
    }

    /**
     * 分页查询帖子列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @param circleId 圈子ID(可空)
     * @return 分页结果
     */
    @Operation(summary = "分页查询帖子")
    @GetMapping("/page")
    public Result<Page<Post>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) Long circleId) {
        return Result.success(postService.page(current, size, circleId));
    }

    /**
     * 首页信息流（推荐/最新/最热）
     *
     * @param sortType 排序类型: latest(最新) / hot(最热)
     * @param current  当前页
     * @param size     每页大小
     * @return 分页帖子列表
     */
    @Operation(summary = "首页信息流")
    @GetMapping("/feed")
    public Result<Page<Post>> feed(@RequestParam(defaultValue = "latest") String sortType,
                                   @RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size) {
        return Result.success(postService.feed(sortType, current, size));
    }

    /**
     * 删除帖子
     *
     * @param id 帖子ID
     * @return 操作结果
     */
    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.delete(id, UserContext.getUserId());
        return Result.success();
    }

    /**
     * 点赞帖子
     *
     * @param id 帖子ID
     * @return 操作结果
     */
    @Operation(summary = "点赞帖子")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean isNew = likeCollectService.like(userId, id, Constants.LC_TYPE_LIKE_POST);
        if (isNew) {
            postService.updateLikeCount(id, 1);
        }
        return Result.success();
    }

    /**
     * 取消点赞
     *
     * @param id 帖子ID
     * @return 操作结果
     */
    @Operation(summary = "取消点赞")
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        likeCollectService.unlike(userId, id, Constants.LC_TYPE_LIKE_POST);
        postService.updateLikeCount(id, -1);
        return Result.success();
    }

    /**
     * 收藏帖子
     *
     * @param id 帖子ID
     * @return 操作结果
     */
    @Operation(summary = "收藏帖子")
    @PostMapping("/{id}/collect")
    public Result<Void> collect(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean isNew = likeCollectService.like(userId, id, Constants.LC_TYPE_COLLECT_POST);
        if (isNew) {
            postService.updateCollectCount(id, 1);
        }
        return Result.success();
    }

    /**
     * 取消收藏
     *
     * @param id 帖子ID
     * @return 操作结果
     */
    @Operation(summary = "取消收藏")
    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollect(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        likeCollectService.unlike(userId, id, Constants.LC_TYPE_COLLECT_POST);
        postService.updateCollectCount(id, -1);
        return Result.success();
    }

    /**
     * 转发帖子
     *
     * <p>功能描述：创建一条新帖，内容引用原帖，sharePostId 指向原帖ID</p>
     *
     * @param id   原帖ID
     * @param post 转发时的附加内容（可为空）
     * @return 创建的转发帖子
     */
    @Operation(summary = "转发帖子")
    @PostMapping("/{id}/share")
    public Result<Post> share(@PathVariable Long id, @RequestBody(required = false) Post post) {
        if (post == null) {
            post = new Post();
        }
        return Result.success(postService.share(post, id, UserContext.getUserId()));
    }

    /**
     * 保存草稿
     *
     * <p>功能描述：将帖子以草稿形式保存（auditStatus=0），不进行敏感词审核</p>
     *
     * @param post 帖子实体（内容、图片等）
     * @return 保存后的草稿帖子
     */
    @Operation(summary = "保存草稿")
    @PostMapping("/draft")
    public Result<Post> saveDraft(@RequestBody Post post) {
        return Result.success(postService.saveDraft(post, UserContext.getUserId()));
    }

    /**
     * 查询草稿列表
     *
     * <p>功能描述：查询当前登录用户的所有草稿（auditStatus=0）</p>
     *
     * @return 草稿帖子列表
     */
    @Operation(summary = "草稿列表")
    @GetMapping("/draft")
    public Result<List<Post>> draftList() {
        return Result.success(postService.getDraftList(UserContext.getUserId()));
    }
}
