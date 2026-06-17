package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.entity.Report;
import com.minthive.security.UserContext;
import com.minthive.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 举报控制器
 */
@Tag(name = "举报接口")
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 提交举报
     *
     * @param report 举报实体
     * @return 举报实体
     */
    @Operation(summary = "提交举报")
    @PostMapping
    public Result<Report> submit(@RequestBody Report report) {
        report.setReporterId(UserContext.getUserId());
        return Result.success(reportService.submit(report));
    }
}
