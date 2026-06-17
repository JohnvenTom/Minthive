package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Post;
import com.minthive.security.UserContext;
import com.minthive.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子控制器
 */
@Tag(name = "帖子接口")
@RestController
@RequestMapping("/api/post")
public class PostController {

    private final PostService postService;

    /**
     * 构造器注入 PostService
     *
     * @param postService 帖子服务
     */
    public PostController(PostService postService) {
        this.postService = postService;
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
}
