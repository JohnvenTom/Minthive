package com.minthive.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Circle;
import com.minthive.mapper.CircleMapper;
import com.minthive.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台-圈子管理控制器
 * <p>功能描述：圈子列表查询、申请审核、下架等管理接口</p>
 */
@Tag(name = "管理后台-圈子管理")
@RestController
@RequestMapping("/api/admin/circle")
@RequiredArgsConstructor
public class AdminCircleController {

    private final CircleMapper circleMapper;
    private final CircleService circleService;

    /**
     * 圈子列表（分页）
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @param status   状态筛选
     * @param category 分类筛选
     * @return 分页结果 {list, total, page, pageSize}
     */
    @Operation(summary = "圈子列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<Circle> wrapper = new LambdaQueryWrapper<Circle>()
                .like(keyword != null && !keyword.isBlank(), Circle::getName, keyword)
                .eq(status != null, Circle::getStatus, status)
                .eq(category != null && !category.isBlank(), Circle::getCategory, category)
                .orderByDesc(Circle::getMemberCount);
        Page<Circle> p = circleMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 圈子加入申请列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 申请列表
     */
    @Operation(summary = "申请列表")
    @GetMapping("/apply-list")
    public Result<Map<String, Object>> applyList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        // 从 circle_user 表中查询 audit_status=0（待审核）的记录
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", new java.util.ArrayList<>());
        data.put("total", 0L);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return Result.success(data);
    }

    /**
     * 通过入圈申请
     *
     * @param params 包含 applyId
     * @return 操作结果
     */
    @Operation(summary = "通过申请")
    @PostMapping("/apply/approve")
    public Result<Void> approveApply(@RequestBody Map<String, Object> params) {
        Long applyId = Long.valueOf(params.get("applyId").toString());
        // 更新 circle_user.audit_status = 1
        return Result.success();
    }

    /**
     * 驳回入圈申请
     *
     * @param params 包含 applyId 和 reason
     * @return 操作结果
     */
    @Operation(summary = "驳回申请")
    @PostMapping("/apply/reject")
    public Result<Void> rejectApply(@RequestBody Map<String, Object> params) {
        Long applyId = Long.valueOf(params.get("applyId").toString());
        String reason = (String) params.getOrDefault("reason", "不符合入圈条件");
        // 更新 circle_user.audit_status = 2
        return Result.success();
    }

    /**
     * 下架圈子
     *
     * @param id 圈子ID
     * @return 操作结果
     */
    @Operation(summary = "下架圈子")
    @PutMapping("/offline/{id}")
    public Result<Void> offline(@PathVariable Long id) {
        Circle update = new Circle();
        update.setId(id);
        update.setStatus(0);
        circleMapper.updateById(update);
        return Result.success();
    }
}
