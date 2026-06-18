package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.common.ResultCode;
import com.minthive.entity.User;
import com.minthive.security.UserContext;
import com.minthive.service.SmsService;
import com.minthive.service.UserService;
import com.minthive.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
 * 认证控制器(登录/注册/验证码)
 *
 * <p>功能描述：处理用户的登录、注册、短信验证码发送/登录、重置密码、登出及当前用户信息查询</p>
 * <p>注意事项：登录接口返回 Token 及用户基础信息，/api/auth/info 需携带有效 JWT</p>
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserService userService;
    private final SmsService smsService;
    private final RedisUtil redisUtil;

    /** 手机号正则校验 */
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 构造器注入 UserService、SmsService 和 RedisUtil
     *
     * @param userService 用户服务
     * @param smsService  短信验证码服务
     * @param redisUtil   Redis工具类
     */
    public AuthController(UserService userService, SmsService smsService, RedisUtil redisUtil) {
        this.userService = userService;
        this.smsService = smsService;
        this.redisUtil = redisUtil;
    }

    /**
     * 发送短信验证码
     *
     * <p>生成6位随机验证码，打印到后端控制台（开发模式），存入Redis（5分钟有效期）</p>
     *
     * @param dto 发送请求（手机号 + 场景）
     * @return 操作结果
     */
    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms")
    public Result<Void> sendSms(@RequestBody @Validated SmsDto dto) {
        smsService.sendCode(dto.getPhone(), dto.getScene());
        return Result.success();
    }

    /**
     * 用户注册（含验证码校验）
     *
     * @param dto 注册请求
     * @return 包含 token 与用户信息的 Map
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody @Validated RegisterDto dto) {
        // 先注册获取用户
        User user = userService.registerWithCode(dto.getAccount(), dto.getPassword(), dto.getPhone(), dto.getCode());
        // 注册成功后生成Token
        String token = userService.login(dto.getAccount(), dto.getPassword());
        user.setPassword(null);
        Map<String, Object> data = buildLoginData(token, user);
        return Result.success(data);
    }

    /**
     * 用户账号密码登录
     *
     * <p>返回 JWT Token 及用户基础信息（id、账号、昵称、头像、角色），供前端直接使用</p>
     *
     * @param dto 登录请求（账号 + 密码）
     * @return 包含 token 与用户信息的 Map
     */
    @Operation(summary = "用户登录（密码模式）")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated LoginDto dto) {
        String token = userService.login(dto.getAccount(), dto.getPassword());
        User user = userService.getByAccount(dto.getAccount());
        Map<String, Object> data = buildLoginData(token, user);
        return Result.success(data);
    }

    /**
     * 用户验证码登录
     *
     * <p>通过手机号 + 短信验证码进行登录，返回与密码登录相同格式的数据</p>
     *
     * @param dto 登录请求（手机号 + 验证码）
     * @return 包含 token 与用户信息的 Map
     */
    @Operation(summary = "用户登录（验证码模式）")
    @PostMapping("/login/sms")
    public Result<Map<String, Object>> loginBySms(@RequestBody @Validated SmsLoginDto dto) {
        String token = userService.loginBySms(dto.getPhone(), dto.getCode());
        // 通过手机号查用户信息返回
        User user = userService.getByPhone(dto.getPhone());
        Map<String, Object> data = buildLoginData(token, user);
        return Result.success(data);
    }

    /**
     * 重置密码
     *
     * <p>通过手机号 + 验证码校验身份后重置密码，成功后需重新登录</p>
     *
     * @param dto 重置密码请求
     * @return 操作结果
     */
    @Operation(summary = "重置密码")
    @PostMapping("/reset")
    public Result<Void> resetPassword(@RequestBody @Validated ResetPasswordDto dto) {
        userService.resetPassword(dto.getPhone(), dto.getCode(), dto.getPassword());
        return Result.success();
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
        Map<String, Object> data = buildUserInfoData(user);
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

    // ==================== 内部辅助方法 ====================

    /**
     * 构建登录响应数据
     *
     * @param token JWT Token
     * @param user  用户实体
     * @return 包含 token 与用户信息的 Map
     */
    private Map<String, Object> buildLoginData(String token, User user) {
        Map<String, Object> data = new HashMap<>(8);
        data.put("token", token);
        data.put("user", user);
        return data;
    }

    /**
     * 构建用户信息响应数据
     *
     * @param user 用户实体
     * @return 用户信息的 Map
     */
    private Map<String, Object> buildUserInfoData(User user) {
        Map<String, Object> data = new HashMap<>(8);
        data.put("adminId", user.getId());
        data.put("account", user.getAccount());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());
        data.put("permissions", java.util.Collections.singletonList("*"));
        return data;
    }

    // ==================== 内部 DTO 类 ====================

    /**
     * 发送短信验证码请求 DTO
     */
    @Data
    public static class SmsDto {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "场景不能为空")
        private String scene; // login / register / reset
    }

    /**
     * 验证码登录请求 DTO
     */
    @Data
    public static class SmsLoginDto {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "验证码不能为空")
        private String code;
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
        private String code; // 短信验证码
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

    /**
     * 重置密码请求 DTO
     */
    @Data
    public static class ResetPasswordDto {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "验证码不能为空")
        private String code;

        @NotBlank(message = "新密码不能为空")
        private String password;
    }
}
