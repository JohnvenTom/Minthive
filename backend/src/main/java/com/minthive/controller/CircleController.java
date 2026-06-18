package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Circle;
import com.minthive.entity.Post;
import com.minthive.security.UserContext;
import com.minthive.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 圈子控制器
 */
@Tag(name = "圈子接口")
@RestController
@RequestMapping("/api/circle")
public class CircleController {

    private final CircleService circleService;

    /**
     * 构造器注入 CircleService
     *
     * @param circleService 圈子服务
     */
    public CircleController(CircleService circleService) {
        this.circleService = circleService;
    }

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

    /**
     * 查询圈内动态（分页）
     *
     * <p>功能描述：获取指定圈子内已审核通过的帖子列表，支持分页</p>
     *
     * @param id      圈子ID（路径参数）
     * @param current 当前页码，默认为1
     * @param size    每页大小，默认为10
     * @return 分页帖子结果
     */
    @Operation(summary = "查询圈内动态")
    @GetMapping("/{id}/posts")
    public Result<Page<Post>> getCirclePosts(@PathVariable Long id,
                                             @RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.success(circleService.getCirclePosts(id, current, size));
    }

    /**
     * 获取圈子分类列表
     *
     * <p>功能描述：返回系统预设的圈子分类名称列表</p>
     *
     * @return 分类名称列表
     */
    @Operation(summary = "获取圈子分类列表")
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(circleService.getCategories());
    }

    /**
     * 获取推荐圈子列表
     *
     * <p>功能描述：返回成员数最多的前10个推荐圈子</p>
     *
     * @return 推荐圈子列表
     */
    @Operation(summary = "推荐圈子")
    @GetMapping("/recommend")
    public Result<List<Circle>> recommend() {
        return Result.success(circleService.recommendCircles());
    }
}
