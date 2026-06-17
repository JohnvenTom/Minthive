package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.entity.User;
import com.minthive.security.UserContext;
import com.minthive.service.UserService;
import com.minthive.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器(登录/注册)
 *
 * <p>功能描述：处理用户/管理员的登录、注册、登出及当前用户信息查询</p>
 * <p>注意事项：登录接口返回 Token 及用户基础信息，/api/auth/info 需携带有效 JWT</p>
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserService userService;
    private final RedisUtil redisUtil;

    /**
     * 构造器注入 UserService 和 RedisUtil
     *
     * @param userService 用户服务
     * @param redisUtil   Redis工具类
     */
    public AuthController(UserService userService, RedisUtil redisUtil) {
        this.userService = userService;
        this.redisUtil = redisUtil;
    }

    /**
     * 用户注册
     *
     * @param dto 注册请求
     * @return 注册用户信息
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<User> register(@RequestBody @Validated RegisterDto dto) {
        User user = userService.register(dto.getAccount(), dto.getPassword(), dto.getPhone());
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 用户登录
     *
     * <p>返回 JWT Token 及用户基础信息（id、账号、昵称、头像、角色），供前端直接使用</p>
     *
     * @param dto 登录请求（账号 + 密码）
     * @return 包含 token 与用户信息的 Map
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated LoginDto dto) {
        String token = userService.login(dto.getAccount(), dto.getPassword());
        // 登录成功后根据账号查询用户信息一并返回，避免前端多一次 /info 请求
        User user = userService.getByAccount(dto.getAccount());
        Map<String, Object> data = new HashMap<>(8);
        data.put("token", token);
        data.put("adminId", user.getId());
        data.put("account", user.getAccount());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());
        return Result.success(data);
    }

    /**
     * 获取当前登录用户信息
     *
     * <p>需携带有效 JWT Token，返回当前用户的 id、账号、昵称、头像、角色等信息</p>
     *
     * @return 当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo() {
        Long userId = UserContext.getUserId();
        User user = userService.getById(userId);
        Map<String, Object> data = new HashMap<>(8);
        data.put("adminId", user.getId());
        data.put("account", user.getAccount());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());
        // TODO: 权限列表可后续根据角色动态加载
        data.put("permissions", java.util.Collections.singletonList("*"));
        return Result.success(data);
    }

    /**
     * 退出登录
     *
     * @return 操作结果
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            redisUtil.deleteLoginToken(userId);
        }
        return Result.success();
    }

    /**
     * 注册请求 DTO
     */
    @Data
    public static class RegisterDto {
        @NotBlank(message = "账号不能为空")
        private String account;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String phone;
    }

    /**
     * 登录请求 DTO
     */
    @Data
    public static class LoginDto {
        @NotBlank(message = "账号不能为空")
        private String account;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
