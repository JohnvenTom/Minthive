package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计变更子表实体
 *
 * <p>对应数据库 audit_log_change 表：一条 SQL 变更一行，保存表名、主键与前后快照</p>
 */
@Data
@TableName("audit_log_change")
public class AuditLogChange implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属审计主记录ID */
    private Long auditLogId;

    /** 被修改的表名 */
    private String tableName;

    /** 主键值（复合主键或无法解析时为空） */
    private String recordId;

    /** INSERT/UPDATE/DELETE */
    private String operation;

    /** 影响行数 */
    private Integer affectedRows;

    /** 变更前快照（超限截断） */
    private String beforeJson;

    /** 变更后快照（超限截断） */
    private String afterJson;

    /** 被脱敏的列名，逗号分隔 */
    private String maskedColumns;

    /** 快照是否被截断 */
    private Integer truncated;

    /** 无法解析主键时的 SQL 模板摘要 */
    private String sqlDigest;

    /** 记录时间 */
    private LocalDateTime createTime;
}
