package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.entity.Circle;
import com.minthive.security.UserContext;
import com.minthive.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 圈主管理控制器
 */
@Tag(name = "圈主管理接口")
@RestController
@RequestMapping("/api/circle-admin")
@RequiredArgsConstructor
public class CircleAdminController {

    private final CircleService circleService;

    /**
     * 更新圈子信息(圈主权限)
     *
     * @param circle 圈子实体
     * @return 更新后的圈子
     */
    @Operation(summary = "圈主更新圈子信息")
    @PutMapping
    public Result<Circle> update(@RequestBody Circle circle) {
        return Result.success(circleService.update(circle));
    }

    /**
     * 移出圈子成员(占位)
     *
     * @param circleId 圈子ID
     * @param userId   被移出用户ID
     * @return 操作结果
     */
    @Operation(summary = "移出圈子成员")
    @DeleteMapping("/member")
    public Result<Void> removeMember(@RequestParam Long circleId, @RequestParam Long userId) {
        circleService.leave(circleId, userId);
        return Result.success();
    }

    /**
     * 发布圈子公告
     *
     * @param circleId 圈子ID
     * @param notice   公告内容
     * @return 操作结果
     */
    @Operation(summary = "发布圈子公告")
    @PutMapping("/notice")
    public Result<Void> publishNotice(@RequestParam Long circleId, @RequestParam String notice) {
        Circle update = new Circle();
        update.setId(circleId);
        update.setNotice(notice);
        circleService.update(update);
        return Result.success();
    }
}
