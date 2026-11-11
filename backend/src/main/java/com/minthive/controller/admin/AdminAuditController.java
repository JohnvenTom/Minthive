package com.minthive.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.audit.AuditQuery;
import com.minthive.audit.AuditQueryService;
import com.minthive.common.Result;
import com.minthive.entity.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * 审计日志管理接口（仅管理员）
 *
 * <p>功能描述：分页查询、查看详情与导出 CSV；导出动作会显式写入一条审计记录</p>
 */
@Tag(name = "审计日志接口")
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditQueryService auditQueryService;

    /**
     * 分页查询审计日志
     *
     * @param query 查询条件（分页 + 时间/主体/表/操作/结果/路径/traceId）
     * @return 分页结果
     */
    @Operation(summary = "分页查询审计日志")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(AuditQuery query) {
        Page<AuditLog> page = auditQueryService.page(query);
        Map<String, Object> data = new java.util.HashMap<>(4);
        data.put("list", page.getRecords());
        data.put("total", page.getTotal());
        data.put("page", page.getCurrent());
        data.put("pageSize", page.getSize());
        return Result.success(data);
    }

    /**
     * 查看审计详情（含字段级前后值）
     *
     * @param id 审计主记录ID
     * @return 主记录与变更子记录
     */
    @Operation(summary = "审计日志详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(auditQueryService.detail(id));
    }

    /**
     * 导出审计日志 CSV
     *
     * @param query    查询条件
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @throws IOException 写出失败时抛出
     */
    @Operation(summary = "导出审计日志")
    @GetMapping("/export")
    public void export(AuditQuery query, HttpServletRequest request, HttpServletResponse response) throws IOException {
        auditQueryService.export(query, request, response);
    }
}
