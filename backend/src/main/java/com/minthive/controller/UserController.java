package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.security.UserContext;
import com.minthive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@Tag(name = "用户接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    /**
     * 构造器注入 UserService
     *
     * @param userService 用户服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询当前登录用户信息
     *
     * @return 用户信息
     */
    @Operation(summary = "查询当前用户信息")
    @GetMapping("/me")
    public Result<User> me() {
        return Result.success(userService.getById(UserContext.getUserId()));
    }

    /**
     * 根据ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * 更新用户信息
     *
     * @param user 用户实体
     * @return 更新后的用户
     */
    @Operation(summary = "更新用户信息")
    @PutMapping
    public Result<User> update(@RequestBody User user) {
        user.setId(UserContext.getUserId());
        return Result.success(userService.update(user));
    }

    /**
     * 更新兴趣标签
     *
     * @param dto 兴趣标签请求体
     * @return 操作结果
     */
    @Operation(summary = "更新兴趣标签")
    @PutMapping("/interests")
    public Result<Void> updateInterests(@RequestBody InterestsDto dto) {
        userService.updateInterests(UserContext.getUserId(), dto.getInterests());
        return Result.success();
    }

    /**
     * 兴趣标签请求 DTO
     */
    @lombok.Data
    public static class InterestsDto {
        private List<String> interests;
    }

    /**
     * 修改密码
     *
     * @param dto 密码请求
     * @return 操作结果
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDto dto) {
        userService.changePassword(UserContext.getUserId(), dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }

    /**
     * 注销账号
     *
     * @return 操作结果
     */
    @Operation(summary = "注销账号")
    @DeleteMapping("/cancel")
    public Result<Void> cancel() {
        userService.cancelAccount(UserContext.getUserId());
        return Result.success();
    }

    /**
     * 修改密码请求 DTO
     */
    @lombok.Data
    public static class ChangePasswordDto {
        private String oldPassword;
        private String newPassword;
    }

    /**
     * 更新头像
     *
     * <p>功能描述：接收头像URL并更新当前登录用户的头像</p>
     *
     * @param dto 头像请求体（包含 avatar 字段）
     * @return 操作结果
     */
    @Operation(summary = "更新头像")
    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestBody AvatarDto dto) {
        userService.updateAvatar(UserContext.getUserId(), dto.getAvatar());
        return Result.success();
    }

    /**
     * 查询指定用户帖子列表
     *
     * <p>功能描述：分页查询指定用户的所有已审核帖子</p>
     *
     * @param id      用户ID
     * @param current 当前页码（默认1）
     * @param size    每页条数（默认10）
     * @return 分页帖子列表
     */
    @Operation(summary = "用户帖子列表")
    @GetMapping("/{id}/posts")
    public Result<Page<Post>> getUserPosts(@PathVariable Long id,
                                           @RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.success(userService.getUserPosts(id, current, size));
    }

    /**
     * 查询当前用户收藏列表
     *
     * <p>功能描述：通过 like_collect 表关联 post 表，获取当前登录用户收藏的帖子列表</p>
     *
     * @param current 当前页码（默认1）
     * @param size    每页条数（默认10）
     * @return 分页收藏帖子列表
     */
    @Operation(summary = "当前用户收藏列表")
    @GetMapping("/collects")
    public Result<Page<Post>> getCollects(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.success(userService.getCollects(UserContext.getUserId(), current, size));
    }

    /**
     * 查询当前用户点赞列表
     *
     * <p>功能描述：通过 like_collect 表关联 post 表，获取当前登录用户点赞的帖子列表</p>
     *
     * @param current 当前页码（默认1）
     * @param size    每页条数（默认10）
     * @return 分页点赞帖子列表
     */
    @Operation(summary = "当前用户点赞列表")
    @GetMapping("/likes")
    public Result<Page<Post>> getLikes(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size) {
        return Result.success(userService.getLikes(UserContext.getUserId(), current, size));
    }

    /**
     * 头像更新请求 DTO
     */
    @lombok.Data
    public static class AvatarDto {
        /** 头像URL地址 */
        private String avatar;
    }
}
