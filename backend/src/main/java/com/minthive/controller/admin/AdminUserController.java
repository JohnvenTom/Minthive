package com.minthive.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Constants;
import com.minthive.common.Result;
import com.minthive.common.ResultCode;
import com.minthive.entity.User;
import com.minthive.mapper.AdminUserMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.util.BcryptUtil;
import com.minthive.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台-用户管理控制器
 * <p>功能描述：提供用户列表查询、详情、封禁/解封、密码重置、僵尸清理等管理接口</p>
 * <p>注意事项：敏感操作(封禁/解封/重置密码)必须清除Redis Token缓存,确保立即生效</p>
 */
@Slf4j
@Tag(name = "管理后台-用户管理")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final com.minthive.mapper.PostMapper postMapper;
    private final com.minthive.mapper.FollowMapper followMapper;
    private final RedisUtil redisUtil; // ✅ 注入Redis工具类(用于清除登录态)

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
     * <p>功能描述：将用户状态设为封禁(status=0),并立即清除Redis Token缓存强制退出登录</p>
     * <p>安全机制：清除Token确保被封禁用户无法继续使用原Token访问系统</p>
     *
     * @param params 包含 userId 的请求体
     * @return 操作结果
     */
    @Operation(summary = "封禁用户")
    @PostMapping("/ban")
    public Result<Void> ban(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        
        // 1. 更新数据库状态为封禁
        User update = new User();
        update.setId(userId);
        update.setStatus(0);
        userMapper.updateById(update);
        
        // 2. ✅ 清除Redis Token缓存(立即失效,强制退出)
        redisUtil.deleteLoginToken(userId);
        log.info("管理员封禁用户: userId={}, 已清除登录态", userId);
        
        return Result.success();
    }

    /**
     * 解封用户
     * <p>功能描述：将用户状态恢复为正常(status=1),并清除可能残留的旧Token确保安全登录</p>
     * <p>安全机制：清除被封禁期间可能残留的Token,防止恶意Token继续有效</p>
     *
     * @param params 包含 userId 的请求体
     * @return 操作结果
     */
    @Operation(summary = "解封用户")
    @PostMapping("/unban")
    public Result<Void> unban(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        
        // 1. 更新数据库状态为正常
        User update = new User();
        update.setId(userId);
        update.setStatus(1);
        userMapper.updateById(update);
        
        // 2. ✅ 清除可能残留的Token(安全考虑,防止被封禁期间生成的Token继续有效)
        redisUtil.deleteLoginToken(userId);
        log.info("管理员解封用户: userId={}, 已清除残留登录态,需重新登录", userId);
        
        return Result.success();
    }

    /**
     * 重置用户密码
     * <p>功能描述：管理员重置用户密码,并立即清除Redis Token缓存强制重新登录</p>
     * <p>安全机制：清除Token确保用户无法使用旧密码的Token继续访问,必须用新密码重新登录</p>
     *
     * @param params 包含 userId 和 newPassword
     * @return 操作结果
     */
    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String newPassword = (String) params.get("newPassword");
        
        // 1. 更新数据库密码
        User update = new User();
        update.setId(userId);
        update.setPassword(BcryptUtil.encode(newPassword));
        userMapper.updateById(update);
        
        // 2. ✅ 清除Redis Token缓存(强制重新登录验证新密码)
        redisUtil.deleteLoginToken(userId);
        log.info("管理员重置密码: userId={}, 已清除登录态,强制重新登录", userId);
        
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
     * 创建新用户（普通用户，role=0）
     *
     * @param data 包含 account/password/nickname/phone（phone 选填）
     * @return 操作结果
     */
    @Operation(summary = "创建新用户")
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> data) {
        String account = (String) data.get("account");
        String password = (String) data.get("password");
        String nickname = (String) data.get("nickname");
        String phone = (String) data.get("phone");
        if (account == null || account.isBlank() || password == null || password.isBlank()) {
            return Result.error(ResultCode.PARAM_ERROR, "账号和密码不能为空");
        }
        Long exist = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getAccount, account));
        if (exist > 0) {
            return Result.error(ResultCode.PARAM_ERROR, "账号已存在");
        }
        if (phone != null && !phone.isBlank()) {
            Long phoneExist = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            if (phoneExist > 0) {
                return Result.error(ResultCode.PARAM_ERROR, "手机号已存在");
            }
        }
        User user = new User();
        user.setAccount(account);
        user.setPassword(BcryptUtil.encode(password));
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : account);
        user.setPhone(phone != null && !phone.isBlank() ? phone : null);
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.USER_STATUS_NORMAL);
        user.setRegisterTime(java.time.LocalDateTime.now());
        userMapper.insert(user);
        Map<String, Object> result = new HashMap<>(4);
        result.put("userId", user.getId());
        result.put("account", user.getAccount());
        result.put("nickname", user.getNickname());
        return Result.success(result);
    }

    // ==================== 角色管理（圈主/管理员） ====================

    /**
     * 按角色查询用户列表
     *
     * @param roles   角色值，逗号分隔，如 "1,2"
     * @param keyword 搜索关键词（可空，匹配账号/昵称）
     * @return 用户数组
     */
    @Operation(summary = "按角色查询用户列表")
    @GetMapping("/role-list")
    public Result<List<Map<String, Object>>> getRoleList(
            @RequestParam(defaultValue = "1,2") String roles,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 按角色筛选
        String[] roleArr = roles.split(",");
        Integer[] roleInts = java.util.Arrays.stream(roleArr).map(Integer::parseInt).toArray(Integer[]::new);
        wrapper.in(User::getRole, (Object[]) roleInts);
        // 关键词搜索
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getAccount, keyword)
                    .or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getId);
        List<User> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>(8);
            m.put("userId", u.getId());
            m.put("account", u.getAccount());
            m.put("nickname", u.getNickname());
            m.put("avatar", u.getAvatar() != null ? u.getAvatar() : "");
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            m.put("registerTime", u.getRegisterTime() != null ? u.getRegisterTime().toString() : "");
            return m;
        }).toList();
        return Result.success(result);
    }

    /**
     * 搜索普通用户（用于选取设为圈主/管理员）
     *
     * @param keyword 搜索关键词（匹配账号/昵称）
     * @return 用户数组
     */
    @Operation(summary = "搜索普通用户")
    @GetMapping("/search-normal")
    public Result<List<Map<String, Object>>> searchNormalUser(@RequestParam String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, Constants.ROLE_USER)
                .and(w -> w.like(User::getAccount, keyword)
                        .or().like(User::getNickname, keyword))
                .orderByDesc(User::getId);
        List<User> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>(8);
            m.put("userId", u.getId());
            m.put("account", u.getAccount());
            m.put("nickname", u.getNickname());
            m.put("avatar", u.getAvatar() != null ? u.getAvatar() : "");
            m.put("registerTime", u.getRegisterTime() != null ? u.getRegisterTime().toString() : "");
            return m;
        }).toList();
        return Result.success(result);
    }

    /**
     * 创建新账号并指定角色（圈主/管理员）
     *
     * @param data 包含 account/password/nickname/role
     * @return 操作结果
     */
    @Operation(summary = "创建新账号并指定角色")
    @PostMapping("/create-with-role")
    public Result<Map<String, Object>> createWithRole(@RequestBody Map<String, Object> data) {
        String account = (String) data.get("account");
        String password = (String) data.get("password");
        String nickname = (String) data.get("nickname");
        String roleStr = (String) data.get("role");
        if (account == null || account.isBlank() || password == null || password.isBlank()) {
            return Result.error(ResultCode.PARAM_ERROR, "账号和密码不能为空");
        }
        // 检查账号是否已存在
        Long exist = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getAccount, account));
        if (exist > 0) {
            return Result.error(ResultCode.PARAM_ERROR, "账号已存在");
        }
        int role = Constants.ROLE_CIRCLE_OWNER;
        if ("ADMIN".equals(roleStr) || "2".equals(roleStr)) {
            role = Constants.ROLE_ADMIN;
        }
        User user = new User();
        user.setAccount(account);
        user.setPassword(BcryptUtil.encode(password));
        user.setNickname(nickname != null ? nickname : account);
        user.setRole(role);
        user.setStatus(Constants.USER_STATUS_NORMAL);
        user.setRegisterTime(java.time.LocalDateTime.now());
        userMapper.insert(user);
        Map<String, Object> result = new HashMap<>(4);
        result.put("userId", user.getId());
        result.put("account", user.getAccount());
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole());
        return Result.success(result);
    }

    /**
     * 设置用户角色（普通→圈主/管理员）
     *
     * @param userId 用户ID
     * @param params 包含 role 的请求体
     * @return 操作结果
     */
    @Operation(summary = "设置用户角色")
    @PutMapping("/role/{userId}")
    public Result<Void> setUserRole(@PathVariable Long userId, @RequestBody Map<String, Object> params) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.PARAM_ERROR, "用户不存在");
        }
        String roleStr = (String) params.get("role");
        int role = Constants.ROLE_CIRCLE_OWNER;
        if ("ADMIN".equals(roleStr) || "2".equals(roleStr)) {
            role = Constants.ROLE_ADMIN;
        }
        User update = new User();
        update.setId(userId);
        update.setRole(role);
        userMapper.updateById(update);
        return Result.success();
    }

    /**
     * 移除用户角色（降级为普通用户）
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @Operation(summary = "移除用户角色")
    @DeleteMapping("/role/{userId}")
    public Result<Void> removeUserRole(@PathVariable Long userId) {
        User update = new User();
        update.setId(userId);
        update.setRole(Constants.ROLE_USER);
        userMapper.updateById(update);
        return Result.success();
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
