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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器(登录/注册)
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtil redisUtil;

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
     * @param dto 登录请求
     * @return JWT Token
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@RequestBody @Validated LoginDto dto) {
        String token = userService.login(dto.getAccount(), dto.getPassword());
        return Result.success(token);
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
