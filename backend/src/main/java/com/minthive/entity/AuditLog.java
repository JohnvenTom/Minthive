package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计主表实体
 *
 * <p>对应数据库 audit_log 表：一次非 GET 写请求（或无请求上下文的后台任务）一行</p>
 */
@Data
@TableName("audit_log")
public class AuditLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 请求链路ID */
    private String traceId;

    /** 操作主体: USER/ADMIN/ANONYMOUS/SYSTEM */
    private String operatorType;

    /** 操作者用户ID（匿名/系统为空） */
    private Long operatorId;

    /** 操作时的数据库角色: 0普通 1圈主 2管理员 */
    private Integer operatorRole;

    /** 匿名尝试账号或系统任务名 */
    private String operatorLabel;

    /** 匿名身份指纹 SHA-256(手机号) */
    private String identityHash;

    /** 来源: HTTP/SYSTEM */
    private String source;

    /** HTTP 方法 */
    private String httpMethod;

    /** 请求路径 */
    private String requestPath;

    /** 查询串 */
    private String requestQuery;

    /** 可信代理解析出的客户端IP */
    private String clientIp;

    /** 直接连接地址 */
    private String remoteAddr;

    /** X-Forwarded-For 原文 */
    private String forwardedFor;

    /** User-Agent */
    private String userAgent;

    /** 业务响应码 */
    private Integer resultCode;

    /** 业务响应消息 */
    private String resultMsg;

    /** HTTP 状态码 */
    private Integer httpStatus;

    /** 请求耗时毫秒 */
    private Integer durationMs;

    /** 变更条数 */
    private Integer changeCount;

    /** 是否存在被截断的快照 */
    private Integer truncated;

    /** 记录时间 */
    private LocalDateTime createTime;
}
