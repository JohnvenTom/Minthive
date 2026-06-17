package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Circle;
import com.minthive.security.UserContext;
import com.minthive.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 圈子控制器
 */
@Tag(name = "圈子接口")
@RestController
@RequestMapping("/api/circle")
public class CircleController {

    @Autowired
    private CircleService circleService;

    /**
     * 创建圈子
     *
     * @param circle 圈子实体
     * @return 圈子实体
     */
    @Operation(summary = "创建圈子")
    @PostMapping
    public Result<Circle> create(@RequestBody Circle circle) {
        circle.setOwnerId(UserContext.getUserId());
        return Result.success(circleService.create(circle));
    }

    /**
     * 查询圈子详情
     *
     * @param id 圈子ID
     * @return 圈子详情
     */
    @Operation(summary = "查询圈子详情")
    @GetMapping("/{id}")
    public Result<Circle> get(@PathVariable Long id) {
        return Result.success(circleService.getById(id));
    }

    /**
     * 分页查询圈子
     *
     * @param current  当前页
     * @param size     每页大小
     * @param category 分类
     * @param keyword  关键词
     * @return 分页结果
     */
    @Operation(summary = "分页查询圈子")
    @GetMapping("/page")
    public Result<Page<Circle>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String keyword) {
        return Result.success(circleService.page(current, size, category, keyword));
    }

    /**
     * 加入圈子
     *
     * @param circleId 圈子ID
     * @return 操作结果
     */
    @Operation(summary = "加入圈子")
    @PostMapping("/join/{circleId}")
    public Result<Void> join(@PathVariable Long circleId) {
        circleService.join(circleId, UserContext.getUserId());
        return Result.success();
    }

    /**
     * 退出圈子
     *
     * @param circleId 圈子ID
     * @return 操作结果
     */
    @Operation(summary = "退出圈子")
    @PostMapping("/leave/{circleId}")
    public Result<Void> leave(@PathVariable Long circleId) {
        circleService.leave(circleId, UserContext.getUserId());
        return Result.success();
    }
}
