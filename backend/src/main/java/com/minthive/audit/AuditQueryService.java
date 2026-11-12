package com.minthive.audit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.TraceIdFilter;
import com.minthive.entity.AuditLog;
import com.minthive.entity.AuditLogChange;
import com.minthive.mapper.AuditLogChangeMapper;
import com.minthive.mapper.AuditLogMapper;
import com.minthive.security.LoginUser;
import com.minthive.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审计日志查询与导出服务
 *
 * <p>功能描述：提供管理端分页查询、详情查看与 CSV 导出；导出动作本身显式写一条审计记录</p>
 * <p>注意事项：GET 请求不在自动采集范围，因此导出留痕必须显式提交</p>
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    /** 单次导出最大主记录数 */
    private static final int EXPORT_LIMIT = 5000;

    /** 分页大小上限 */
    private static final int MAX_PAGE_SIZE = 100;

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AuditLogMapper auditLogMapper;

    private final AuditLogChangeMapper auditLogChangeMapper;

    private final AuditWriter auditWriter;

    /**
     * 分页查询审计主记录
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public Page<AuditLog> page(AuditQuery query) {
        long size = Math.min(Math.max(1, query.getPageSize()), MAX_PAGE_SIZE);
        long current = Math.max(1, query.getPage());
        return auditLogMapper.selectPage(new Page<>(current, size), wrapper(query));
    }

    /**
     * 查询审计详情（主记录 + 变更子记录）
     *
     * @param id 主记录ID
     * @return 详情 Map；不存在时返回 null
     */
    public Map<String, Object> detail(Long id) {
        AuditLog log = auditLogMapper.selectById(id);
        if (log == null) {
            return null;
        }
        List<AuditLogChange> changes = auditLogChangeMapper.selectList(
                new QueryWrapper<AuditLogChange>().eq("audit_log_id", id).orderByAsc("id"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("log", log);
        data.put("changes", changes);
        return data;
    }

    /**
     * 导出审计日志 CSV
     *
     * @param query    查询条件
     * @param request  HTTP 请求（用于导出留痕）
     * @param response HTTP 响应
     * @throws IOException 写出失败时抛出
     */
    public void export(AuditQuery query, HttpServletRequest request, HttpServletResponse response) throws IOException {
        QueryWrapper<AuditLog> wrapper = wrapper(query);
        wrapper.last("LIMIT " + EXPORT_LIMIT);
        List<AuditLog> logs = auditLogMapper.selectList(wrapper);
        List<Long> ids = logs.stream().map(AuditLog::getId).toList();
        Map<Long, List<AuditLogChange>> changeMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<AuditLogChange> changes = auditLogChangeMapper.selectList(
                    new QueryWrapper<AuditLogChange>().in("audit_log_id", ids).orderByAsc("id"));
            changeMap = changes.stream().collect(Collectors.groupingBy(AuditLogChange::getAuditLogId));
        }

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit-" + LocalDateTime.now().format(FILE_TIME) + ".csv\"");
        PrintWriter writer = response.getWriter();
        // BOM 让 Excel 正确识别 UTF-8
        writer.write('\uFEFF');
        writer.println("log_id,create_time,operator_type,operator_id,operator_label,source,http_method,path,client_ip,"
                + "result_code,result_msg,http_status,duration_ms,trace_id,table_name,record_id,operation,"
                + "affected_rows,before_json,after_json,masked_columns,truncated,sql_digest");
        int rows = 0;
        for (AuditLog log : logs) {
            List<AuditLogChange> changes = changeMap.get(log.getId());
            if (changes == null || changes.isEmpty()) {
                writer.println(String.join(",", csv(log.getId()), csv(log.getCreateTime()),
                        csv(log.getOperatorType()), csv(log.getOperatorId()), csv(log.getOperatorLabel()),
                        csv(log.getSource()), csv(log.getHttpMethod()), csv(log.getRequestPath()),
                        csv(log.getClientIp()), csv(log.getResultCode()), csv(log.getResultMsg()),
                        csv(log.getHttpStatus()), csv(log.getDurationMs()), csv(log.getTraceId()),
                        csv(""), csv(""), csv(""), csv(""), csv(""), csv(""), csv(""), csv(""), csv("")));
                rows++;
                continue;
            }
            for (AuditLogChange change : changes) {
                writer.println(String.join(",", csv(log.getId()), csv(log.getCreateTime()),
                        csv(log.getOperatorType()), csv(log.getOperatorId()), csv(log.getOperatorLabel()),
                        csv(log.getSource()), csv(log.getHttpMethod()), csv(log.getRequestPath()),
                        csv(log.getClientIp()), csv(log.getResultCode()), csv(log.getResultMsg()),
                        csv(log.getHttpStatus()), csv(log.getDurationMs()), csv(log.getTraceId()),
                        csv(change.getTableName()), csv(change.getRecordId()), csv(change.getOperation()),
                        csv(change.getAffectedRows()), csv(change.getBeforeJson()), csv(change.getAfterJson()),
                        csv(change.getMaskedColumns()), csv(change.getTruncated()), csv(change.getSqlDigest())));
                rows++;
            }
        }
        writer.flush();
        recordExport(request, query, rows);
    }

    /**
     * 构造查询条件
     *
     * @param query 查询条件
     * @return QueryWrapper
     */
    private QueryWrapper<AuditLog> wrapper(AuditQuery query) {
        QueryWrapper<AuditLog> wrapper = new QueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getOperatorType()), "operator_type", query.getOperatorType())
                .eq(query.getOperatorId() != null, "operator_id", query.getOperatorId())
                .eq(query.getResultCode() != null, "result_code", query.getResultCode())
                .eq(StringUtils.hasText(query.getTraceId()), "trace_id", query.getTraceId())
                .like(StringUtils.hasText(query.getPath()), "request_path", query.getPath())
                .ge(query.getStartTime() != null, "create_time", query.getStartTime())
                .le(query.getEndTime() != null, "create_time", query.getEndTime())
                .orderByDesc("create_time");
        if (StringUtils.hasText(query.getTableName())) {
            wrapper.apply("EXISTS (SELECT 1 FROM audit_log_change c WHERE c.audit_log_id = audit_log.id AND c.table_name = {0})",
                    query.getTableName());
        }
        if (StringUtils.hasText(query.getOperation())) {
            wrapper.apply("EXISTS (SELECT 1 FROM audit_log_change c WHERE c.audit_log_id = audit_log.id AND c.operation = {0})",
                    query.getOperation().toUpperCase());
        }
        return wrapper;
    }

    /**
     * 记录导出动作
     *
     * @param request HTTP 请求
     * @param query   导出条件
     * @param rows    导出行数
     */
    private void recordExport(HttpServletRequest request, AuditQuery query, int rows) {
        LoginUser user = UserContext.get();
        AuditLog log = new AuditLog();
        log.setTraceId(MDC.get(TraceIdFilter.MDC_KEY));
        log.setOperatorType(user == null ? "ANONYMOUS"
                : (Integer.valueOf(2).equals(user.getRole()) ? "ADMIN" : "USER"));
        log.setOperatorId(user == null ? null : user.getUserId());
        log.setOperatorRole(user == null ? null : user.getRole());
        log.setOperatorLabel(user == null ? null : user.getAccount());
        log.setSource("HTTP");
        log.setHttpMethod("GET");
        log.setRequestPath("/api/admin/audit/export");
        log.setRequestQuery(truncate(request.getQueryString(), 1024));
        log.setRemoteAddr(truncate(request.getRemoteAddr(), 64));
        log.setForwardedFor(truncate(request.getHeader("X-Forwarded-For"), 512));
        log.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        log.setResultCode(200);
        log.setResultMsg("导出审计日志 " + rows + " 行，条件: " + truncate(query.toString(), 180));
        log.setHttpStatus(200);
        log.setChangeCount(0);
        log.setTruncated(0);
        auditWriter.submit(AuditEventFactory.event(log, new ArrayList<>()));
    }

    /**
     * CSV 单元格转义（防公式注入与换行破坏）
     *
     * @param value 原始值
     * @return 安全单元格
     */
    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value)
                .replace('\r', ' ').replace('\n', ' ');
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    /**
     * 字符串截断
     *
     * @param value 原始值
     * @param max   最大长度
     * @return 截断后的值
     */
    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
