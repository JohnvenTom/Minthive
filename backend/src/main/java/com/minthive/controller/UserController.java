package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.entity.User;
import com.minthive.security.UserContext;
import com.minthive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

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
}
