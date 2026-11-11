package com.minthive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.dto.PublicUserDto;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.mapper.FollowMapper;
import com.minthive.entity.Follow;
import com.minthive.security.UserContext;
import com.minthive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 用户控制器
 */
@Tag(name = "用户接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final FollowMapper followMapper;

    public UserController(UserService userService, FollowMapper followMapper) {
        this.userService = userService;
        this.followMapper = followMapper;
    }

    /**
     * 查询当前登录用户信息
     *
     * @return 用户信息（含帖子数/关注数/粉丝数）
     */
    @Operation(summary = "查询当前用户信息")
    @GetMapping("/me")
    public Result<User> me() {
        User user = userService.getById(UserContext.requireUserId());
        enrichUserStats(user);
        return Result.success(user);
    }

    /**
     * 根据ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息（含帖子数/关注数/粉丝数/关注状态）
     */
    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public Result<PublicUserDto> getById(@PathVariable Long id) {
        PublicUserDto user = PublicUserDto.from(userService.getById(id));
        enrichUserStats(user);
        Long currentUserId = UserContext.getUserId();
        // 查询当前登录用户是否已关注该用户，附加 isFollowing 字段
        if (currentUserId != null && !currentUserId.equals(id)) {
            long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getUserId, currentUserId)
                    .eq(Follow::getFollowUserId, id));
            user.setIsFollowing(count > 0);
        } else {
            user.setIsFollowing(false);
        }
        return Result.success(user);
    }

    /**
     * 填充用户统计数据（委托给 UserService）
     *
     * @param user 用户实体
     */
    private void enrichUserStats(User user) {
        userService.enrichUserStats(user);
    }

    /**
     * 更新用户信息
     *
     * @param user 用户实体
     * @return 更新后的用户
     */
    @Operation(summary = "更新用户信息")
    @PutMapping
    public Result<User> update(@RequestBody UserUpdateDto dto) {
        // 严禁直接绑定 User 实体更新：role/status/password 等敏感列一旦随请求体进入
        // updateById（非空字段全更新）即构成批量赋值提权，此处仅映射白名单字段
        User user = new User();
        user.setId(UserContext.requireUserId());
        user.setNickname(dto.getNickname());
        user.setBio(dto.getBio());
        user.setGender(dto.getGender());
        user.setBirthday(dto.getBirthday());
        user.setAvatar(dto.getAvatar());
        return Result.success(userService.update(user));
    }

    /**
     * 更新兴趣标签
     *
     * <p>功能描述：接收前端选中的兴趣标签列表，转为逗号分隔字符串存入数据库</p>
     *
     * @param dto 兴趣标签请求体（包含 interests 列表）
     * @return 操作结果（成功/未登录 401）
     */
    @Operation(summary = "更新兴趣标签")
    @PutMapping("/interests")
    public Result<Void> updateInterests(@RequestBody InterestsDto dto) {
        Long userId = UserContext.requireUserId();
        // 参数校验：防止空指针异常，允许传入空列表（表示清空兴趣标签）
        List<String> interests = dto.getInterests();
        if (interests == null) {
            interests = Collections.emptyList();
        }
        userService.updateInterests(userId, interests);
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
        userService.changePassword(UserContext.requireUserId(), dto.getOldPassword(), dto.getNewPassword());
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
        userService.cancelAccount(UserContext.requireUserId());
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
        userService.updateAvatar(UserContext.requireUserId(), dto.getAvatar());
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
        return Result.success(userService.getCollects(UserContext.requireUserId(), current, size));
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
        return Result.success(userService.getLikes(UserContext.requireUserId(), current, size));
    }

    /**
     * 头像更新请求 DTO
     */
    @lombok.Data
    public static class AvatarDto {
        /** 头像URL地址 */
        private String avatar;
    }

    /**
     * 更新用户信息请求 DTO
     *
     * <p>字段白名单：仅开放个人资料字段，role/status/password/phone 等敏感列一律不可通过本接口修改</p>
     */
    @lombok.Data
    public static class UserUpdateDto {
        /** 昵称 */
        private String nickname;

        /** 个人简介 */
        private String bio;

        /** 性别:0未知 1男 2女 */
        private Integer gender;

        /** 生日 */
        private LocalDate birthday;

        /** 头像URL */
        private String avatar;
    }
}
