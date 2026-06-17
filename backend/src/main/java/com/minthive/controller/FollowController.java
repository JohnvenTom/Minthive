package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.security.UserContext;
import com.minthive.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

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
}
