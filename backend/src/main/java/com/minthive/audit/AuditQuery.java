package com.minthive.audit;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 审计日志查询条件
 */
@Data
public class AuditQuery {

    /** 当前页 */
    private long page = 1;

    /** 每页条数 */
    private long pageSize = 20;

    /** 操作主体类型 */
    private String operatorType;

    /** 操作者用户ID */
    private Long operatorId;

    /** 被修改表名 */
    private String tableName;

    /** 操作类型 INSERT/UPDATE/DELETE */
    private String operation;

    /** 业务响应码 */
    private Integer resultCode;

    /** 请求路径（模糊） */
    private String path;

    /** 链路ID */
    private String traceId;

    /** 开始时间（ISO-8601，如 2026-11-12T00:00:00） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    /** 结束时间（ISO-8601，如 2026-11-12T23:59:59） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}
