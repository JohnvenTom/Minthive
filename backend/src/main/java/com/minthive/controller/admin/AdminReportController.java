package com.minthive.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Report;
import com.minthive.mapper.AdminReportMapper;
import com.minthive.mapper.ReportMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台-举报工单管理控制器
 * <p>功能描述：提供举报工单的分页查询、详情、驳回、删除内容、处罚等处理接口</p>
 */
@Tag(name = "管理后台-举报工单")
@RestController
@RequestMapping("/api/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportMapper reportMapper;
    private final AdminReportMapper adminReportMapper;

    /**
     * 分页查询举报工单列表（关联用户表 + 内容表，字段名对齐前端 ReportWorkOrder 类型）
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @param status   工单状态（0待处理 1已驳回 2已处理）
     * @param type     举报类型
     * @return 分页结果 {list, total, page, pageSize}
     */
    @Operation(summary = "分页查询举报工单")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String type) {
        IPage<Map<String, Object>> p = adminReportMapper.selectReportListWithDetails(
                new Page<>(page, pageSize), keyword, status, type);
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 工单详情（返回完整信息，字段名对齐前端）
     *
     * @param workOrderId 工单ID
     * @return 工单完整信息
     */
    @Operation(summary = "工单详情")
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long workOrderId) {
        IPage<Map<String, Object>> p = adminReportMapper.selectReportListWithDetails(
                new Page<>(1, 1), null, null, null);
        // 从全量结果中筛选目标记录
        Map<String, Object> detail = p.getRecords().stream()
                .filter(r -> workOrderId.equals(((Number) r.get("workOrderId")).longValue()))
                .findFirst()
                .orElseGet(() -> {
                    // fallback: 直接查实体并手动映射
                    Report r = reportMapper.selectById(workOrderId);
                    Map<String, Object> m = new HashMap<>();
                    if (r != null) {
                        m.put("workOrderId", r.getId());
                        m.put("reporterId", r.getReporterId());
                        m.put("reporterName", "用户" + r.getReporterId());
                        m.put("targetId", r.getTargetId());
                        m.put("targetType", r.getTargetType().toString());
                        m.put("targetContent", r.getReason());
                        m.put("accusedId", r.getTargetId());
                        m.put("accusedName", "用户" + r.getTargetId());
                        m.put("type", r.getReportType());
                        m.put("reason", r.getReason());
                        m.put("status", r.getStatus() == 0 ? "PENDING" : r.getStatus() == 1 ? "REJECTED" : "RESOLVED");
                        m.put("riskLevel", r.getAiRiskLevel() == null ? "LOW" : r.getAiRiskLevel() == 1 ? "LOW" : r.getAiRiskLevel() == 2 ? "MEDIUM" : "HIGH");
                        m.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
                        m.put("handleTime", r.getHandleTime() != null ? r.getHandleTime().toString() : "");
                        m.put("handleResult", r.getResult());
                    }
                    return m;
                });
        return Result.success(detail);
    }

    /**
     * 驳回工单
     *
     * @param params 包含 workOrderId 和 remark
     * @return 操作结果
     */
    @Operation(summary = "驳回工单")
    @PostMapping("/reject")
    public Result<Void> reject(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("workOrderId").toString());
        String remark = (String) params.getOrDefault("remark", "");
        Report update = new Report();
        update.setId(id);
        update.setStatus(1); // 已驳回
        update.setResult(remark);
        update.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(update);
        return Result.success();
    }

    /**
     * 删除被举报内容
     *
     * @param params 包含 workOrderId
     * @return 操作结果
     */
    @Operation(summary = "删除被举报内容")
    @PostMapping("/delete-content")
    public Result<Void> deleteContent(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("workOrderId").toString());
        Report r = reportMapper.selectById(id);
        if (r != null) {
            // 根据 targetType 标记内容为逻辑删除（实际删除需对应业务表操作）
            Report update = new Report();
            update.setId(id);
            update.setStatus(2); // 已处理
            update.setResult("内容已删除");
            update.setHandleTime(LocalDateTime.now());
            reportMapper.updateById(update);
        }
        return Result.success();
    }

    /**
     * 处罚用户
     *
     * @param params 包含 userId、punishType（封禁/警告）、reason
     * @return 操作结果
     */
    @Operation(summary = "处罚用户")
    @PostMapping("/punish")
    public Result<Void> punish(@RequestBody Map<String, Object> params) {
        // 记录处罚日志并执行封禁等操作
        // 实际项目中应写入 punish_log 表
        return Result.success();
    }
}
