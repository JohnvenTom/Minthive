package com.minthive.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Report;
import com.minthive.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台-举报工单管理控制器
 */
@Tag(name = "管理后台-举报工单")
@RestController
@RequestMapping("/api/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    /**
     * 分页查询举报工单
     *
     * @param current 当前页
     * @param size    每页大小
     * @param status  状态
     * @return 分页结果
     */
    @Operation(summary = "分页查询举报工单")
    @GetMapping("/page")
    public Result<Page<Report>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) Integer status) {
        return Result.success(reportService.page(current, size, status));
    }

    /**
     * 处理工单
     *
     * @param id     工单ID
     * @param status 处理状态
     * @param result 处理结果
     * @return 操作结果
     */
    @Operation(summary = "处理举报工单")
    @PutMapping("/handle/{id}")
    public Result<Void> handle(@PathVariable Long id,
                               @RequestParam Integer status,
                               @RequestParam(required = false) String result) {
        reportService.handle(id, status, result);
        return Result.success();
    }

    /**
     * AI 风险等级评估
     *
     * @param id    工单ID
     * @param level 风险等级
     * @return 操作结果
     */
    @Operation(summary = "AI风险等级评估")
    @PutMapping("/assess/{id}")
    public Result<Void> assess(@PathVariable Long id, @RequestParam Integer level) {
        reportService.assessRisk(id, level);
        return Result.success();
    }
}
