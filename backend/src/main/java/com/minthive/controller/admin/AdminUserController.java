package com.minthive.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.User;
import com.minthive.mapper.AdminUserMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.util.BcryptUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台-用户管理控制器
 * <p>功能描述：提供用户列表查询、详情、封禁/解封、密码重置、僵尸清理等管理接口</p>
 */
@Tag(name = "管理后台-用户管理")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final com.minthive.mapper.PostMapper postMapper;
    private final com.minthive.mapper.FollowMapper followMapper;

    /**
     * 分页查询用户列表（含统计子查询，字段对齐前端 UserInfo）
     *
     * @param page    页码（默认1）
     * @param pageSize 每页大小（默认10）
     * @param keyword 搜索关键词（可空）
     * @param status  账号状态（NORMAL/BANNED/DELETED，兼容数字 0/1/2）
     * @return 分页结果 {list, total, page, pageSize}
     */
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        // 将前端字符串状态映射为数据库数字值
        Integer statusInt = mapUserStatusToInt(status);
        IPage<Map<String, Object>> p = adminUserMapper.selectUserListWithStats(
                new Page<>(page, pageSize), keyword, statusInt);
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 用户详情（字段对齐前端 UserInfo）
     *
     * @param userId 用户ID
     * @return 用户信息（含实时统计数据）
     */
    @Operation(summary = "用户详情")
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            return Result.success(new HashMap<>());
        }
        Map<String, Object> m = new HashMap<>(16);
        m.put("userId", u.getId());
        m.put("account", u.getAccount());
        m.put("nickname", u.getNickname());
        m.put("avatar", u.getAvatar() != null ? u.getAvatar() : "");
        m.put("phone", u.getPhone());
        m.put("gender", u.getGender());
        m.put("status", u.getStatus() == 1 ? "NORMAL" : "BANNED");
        // 实时统计：帖子数、关注数、粉丝数
        long postCount = postMapper.selectCount(
                new LambdaQueryWrapper<com.minthive.entity.Post>()
                        .eq(com.minthive.entity.Post::getUserId, userId));
        long followCount = followMapper.selectCount(
                new LambdaQueryWrapper<com.minthive.entity.Follow>()
                        .eq(com.minthive.entity.Follow::getUserId, userId));
        long fanCount = followMapper.selectCount(
                new LambdaQueryWrapper<com.minthive.entity.Follow>()
                        .eq(com.minthive.entity.Follow::getFollowUserId, userId));
        m.put("postCount", postCount);
        m.put("followCount", followCount);
        m.put("fansCount", fanCount);
        m.put("registerTime", u.getRegisterTime() != null ? u.getRegisterTime().toString() : "");
        m.put("lastLoginTime", u.getLastLoginTime() != null ? u.getLastLoginTime().toString() : "");
        m.put("interests", u.getInterestTags() != null && !u.getInterestTags().isBlank()
                ? u.getInterestTags().split(",") : new String[0]);
        return Result.success(m);
    }

    /**
     * 封禁用户
     *
     * @param params 包含 userId 的请求体
     * @return 操作结果
     */
    @Operation(summary = "封禁用户")
    @PostMapping("/ban")
    public Result<Void> ban(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        User update = new User();
        update.setId(userId);
        update.setStatus(0);
        userMapper.updateById(update);
        return Result.success();
    }

    /**
     * 解封用户
     *
     * @param params 包含 userId 的请求体
     * @return 操作结果
     */
    @Operation(summary = "解封用户")
    @PostMapping("/unban")
    public Result<Void> unban(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        User update = new User();
        update.setId(userId);
        update.setStatus(1);
        userMapper.updateById(update);
        return Result.success();
    }

    /**
     * 重置用户密码
     *
     * @param params 包含 userId 和 newPassword
     * @return 操作结果
     */
    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String newPassword = (String) params.get("newPassword");
        User update = new User();
        update.setId(userId);
        update.setPassword(BcryptUtil.encode(newPassword));
        userMapper.updateById(update);
        return Result.success();
    }

    /**
     * 清理僵尸账号（长期未活跃的注销状态用户）
     *
     * @return 清理数量
     */
    @Operation(summary = "清理僵尸账号")
    @PostMapping("/clean-zombie")
    public Result<Map<String, Object>> cleanZombie() {
        // 删除已注销且超过30天的用户
        int cleaned = userMapper.delete(new LambdaQueryWrapper<User>()
                .eq(User::getCancelStatus, 2));
        Map<String, Object> data = new HashMap<>(1);
        data.put("cleaned", cleaned);
        return Result.success(data);
    }

    /**
     * 查询用户行为记录
     *
     * @param userId 用户ID
     * @return 行为摘要信息
     */
    @Operation(summary = "用户行为记录")
    @GetMapping("/actions")
    public Result<Map<String, Object>> actions(@RequestParam Long userId) {
        Map<String, Object> data = new HashMap<>(8);
        data.put("userId", userId);
        // 后续对接 ai_user_log 表统计
        data.put("postCount", 0);
        data.put("commentCount", 0);
        data.put("likeCount", 0);
        data.put("lastActiveTime", null);
        return Result.success(data);
    }

    /**
     * 将前端字符串用户状态映射为数据库数字值
     * <p>功能：兼容前端传递的字符串状态（NORMAL/BANNED/DELETED）和数字字符串（0/1）</p>
     *
     * @param status 前端传入的状态参数（String）
     * @return 数据库数字状态值（0封禁 1正常），null 表示不筛选（含 DELETED 时返回 null 由 SQL 处理）
     */
    private Integer mapUserStatusToInt(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        switch (status.toUpperCase()) {
            case "NORMAL":
            case "1":
                return 1;
            case "BANNED":
            case "0":
                return 0;
            case "DELETED":
            case "2":
                // 已注销用户通过 cancel_status 过滤，此处返回 null 不做 status 筛选
                return null;
            default:
                try {
                    return Integer.parseInt(status);
                } catch (NumberFormatException e) {
                    return null;
                }
        }
    }
}
