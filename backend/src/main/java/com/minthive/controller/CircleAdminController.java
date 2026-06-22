package com.minthive.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Circle;
import com.minthive.security.UserContext;
import com.minthive.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 圈主管理控制器
 *
 * <p>提供圈主对自身圈子的管理能力：信息更新、成员列表、移除成员、转让圈主、发布公告</p>
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
     * 圈子正式成员列表（分页）
     *
     * <p>仅返回 audit_status=1 的正式成员，圈主置顶；操作者必须为该圈圈主</p>
     *
     * @param circleId 圈子ID
     * @param current  当前页码
     * @param size     每页大小
     * @param keyword  昵称关键词（可空）
     * @return 分页结果 {list, total, page, pageSize}
     */
    @Operation(summary = "圈子成员列表")
    @GetMapping("/members")
    public Result<Map<String, Object>> members(
            @RequestParam Long circleId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        // 校验操作者为圈主（listMembers 内部已校验圈子存在）
        assertOwner(circleId);
        IPage<Map<String, Object>> p = circleService.listMembers(circleId, current, size, keyword);
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 移出圈子成员（仅圈主可操作，不能移除圈主）
     *
     * @param circleId 圈子ID
     * @param userId   被移出用户ID
     * @return 操作结果
     */
    @Operation(summary = "移出圈子成员")
    @DeleteMapping("/member")
    public Result<Void> removeMember(@RequestParam Long circleId, @RequestParam Long userId) {
        circleService.removeMember(circleId, UserContext.getUserId(), userId);
        return Result.success();
    }

    /**
     * 转让圈主（仅圈主可操作）
     *
     * @param params 包含 circleId 和 newOwnerId
     * @return 操作结果
     */
    @Operation(summary = "转让圈主")
    @PostMapping("/transfer")
    public Result<Void> transfer(@RequestBody Map<String, Object> params) {
        Long circleId = Long.valueOf(params.get("circleId").toString());
        Long newOwnerId = Long.valueOf(params.get("newOwnerId").toString());
        circleService.transferOwner(circleId, UserContext.getUserId(), newOwnerId);
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

    /**
     * 校验当前登录用户是否为指定圈子的圈主
     *
     * @param circleId 圈子ID
     */
    private void assertOwner(Long circleId) {
        Circle circle = circleService.getById(circleId);
        Long currentUserId = UserContext.getUserId();
        if (circle.getOwnerId() == null || !circle.getOwnerId().equals(currentUserId)) {
            throw new com.minthive.common.BusinessException(
                    com.minthive.common.ResultCode.FORBIDDEN, "只有圈主才能管理成员");
        }
    }
}
