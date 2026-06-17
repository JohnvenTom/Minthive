package com.minthive.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.User;
import com.minthive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台-用户管理控制器
 */
@Tag(name = "管理后台-用户管理")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     *
     * @param current  当前页
     * @param size     每页大小
     * @param keyword  搜索关键词(可空)
     * @return 分页结果
     */
    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<Page<User>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) String keyword) {
        return Result.success(userService.searchByKeyword(keyword, current, size));
    }

    /**
     * 封禁用户
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @Operation(summary = "封禁用户")
    @PutMapping("/ban/{id}")
    public Result<Void> ban(@PathVariable Long id) {
        User update = new User();
        update.setId(id);
        update.setStatus(0);
        userService.update(update);
        return Result.success();
    }

    /**
     * 解封用户
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @Operation(summary = "解封用户")
    @PutMapping("/unban/{id}")
    public Result<Void> unban(@PathVariable Long id) {
        User update = new User();
        update.setId(id);
        update.setStatus(1);
        userService.update(update);
        return Result.success();
    }
}
