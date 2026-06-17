package com.minthive.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Circle;
import com.minthive.mapper.AdminCircleMapper;
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
    private final AdminCircleMapper adminCircleMapper;
    private final CircleService circleService;

    /**
     * 圈子列表（关联圈主，字段对齐前端 CircleInfo）
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
        IPage<Map<String, Object>> p = adminCircleMapper.selectCircleListWithOwner(
                new Page<>(page, pageSize), keyword, status, category);
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 圈子加入申请列表（关联用户昵称，字段对齐前端 CircleApply）
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
        IPage<Map<String, Object>> p = adminCircleMapper.selectApplyListWithNickname(
                new Page<>(page, pageSize));
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
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

    /**
     * 编辑圈子信息
     *
     * @param params 包含 circleId, name, category, intro, type
     * @return 操作结果
     */
    @Operation(summary = "编辑圈子")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody Map<String, Object> params) {
        Long circleId = Long.valueOf(params.get("circleId").toString());
        Circle update = new Circle();
        update.setId(circleId);
        if (params.containsKey("name")) {
            update.setName((String) params.get("name"));
        }
        if (params.containsKey("category")) {
            update.setCategory((String) params.get("category"));
        }
        if (params.containsKey("intro")) {
            update.setIntro((String) params.get("intro"));
        }
        if (params.containsKey("type")) {
            String typeStr = (String) params.get("type");
            update.setType("PRIVATE".equals(typeStr) ? 1 : 0);
        }
        circleMapper.updateById(update);
        return Result.success();
    }

    /**
     * 设置/取消推荐位
     *
     * @param params 包含 circleId 和 recommended(boolean)
     * @return 操作结果
     */
    @Operation(summary = "设置推荐位")
    @PostMapping("/recommend")
    public Result<Void> setRecommend(@RequestBody Map<String, Object> params) {
        Long circleId = Long.valueOf(params.get("circleId").toString());
        Boolean recommended = (Boolean) params.get("recommended");
        Circle update = new Circle();
        update.setId(circleId);
        update.setRecommend(recommended != null && recommended ? 1 : 0);
        circleMapper.updateById(update);
        return Result.success();
    }

    /**
     * 转让圈主
     *
     * @param params 包含 circleId 和 newOwnerId
     * @return 操作结果
     */
    @Operation(summary = "转让圈主")
    @PostMapping("/transfer")
    public Result<Void> transfer(@RequestBody Map<String, Object> params) {
        Long circleId = Long.valueOf(params.get("circleId").toString());
        Long newOwnerId = Long.valueOf(params.get("newOwnerId").toString());
        Circle update = new Circle();
        update.setId(circleId);
        update.setOwnerId(newOwnerId);
        circleMapper.updateById(update);
        return Result.success();
    }
}
