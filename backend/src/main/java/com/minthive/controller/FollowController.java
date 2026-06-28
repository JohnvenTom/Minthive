package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.User;
import com.minthive.security.UserContext;
import com.minthive.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关注控制器
 */
@Tag(name = "关注接口")
@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;

    /**
     * 构造器注入 FollowService
     *
     * @param followService 关注服务
     */
    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /**
     * 关注用户
     *
     * @param followUserId 被关注用户ID
     * @return 操作结果
     */
    @Operation(summary = "关注用户")
    @PostMapping("/{followUserId}")
    public Result<Void> follow(@PathVariable Long followUserId) {
        followService.follow(UserContext.getUserId(), followUserId);
        return Result.success();
    }

    /**
     * 取消关注
     *
     * @param followUserId 被关注用户ID
     * @return 操作结果
     */
    @Operation(summary = "取消关注")
    @DeleteMapping("/{followUserId}")
    public Result<Void> unfollow(@PathVariable Long followUserId) {
        followService.unfollow(UserContext.getUserId(), followUserId);
        return Result.success();
    }

    /**
     * 查询是否已关注
     *
     * @param followUserId 被关注用户ID
     * @return 是否已关注
     */
    @Operation(summary = "查询是否已关注")
    @GetMapping("/{followUserId}")
    public Result<Boolean> isFollowing(@PathVariable Long followUserId) {
        return Result.success(followService.isFollowing(UserContext.getUserId(), followUserId));
    }

    /**
     * 获取用户的关注列表
     *
     * <p>查询指定用户关注的用户列表，分页返回</p>
     *
     * @param userId  目标用户ID
     * @param current 当前页码，默认1
     * @param size    每页条数，默认20
     * @return 分页用户列表
     */
    @Operation(summary = "获取关注列表")
    @GetMapping("/{userId}/following")
    public Result<Page<User>> followingList(@PathVariable Long userId,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size) {
        return Result.success(followService.getFollowingList(userId, current, size));
    }

    /**
     * 获取用户的粉丝列表
     *
     * <p>查询关注指定用户的粉丝列表，分页返回</p>
     *
     * @param userId  目标用户ID
     * @param current 当前页码，默认1
     * @param size    每页条数，默认20
     * @return 分页用户列表
     */
    @Operation(summary = "获取粉丝列表")
    @GetMapping("/{userId}/followers")
    public Result<Page<User>> followersList(@PathVariable Long userId,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size) {
        return Result.success(followService.getFollowersList(userId, UserContext.getUserId(), current, size));
    }

    /**
     * 推荐好友
     *
     * <p>简单实现：返回最近注册的10个非当前用户（后续可接入AI推荐）</p>
     *
     * @return 推荐用户列表，最多10条
     */
    @Operation(summary = "推荐好友")
    @GetMapping("/recommend")
    public Result<List<User>> recommend() {
        return Result.success(followService.recommendUsers(UserContext.getUserId()));
    }
}
