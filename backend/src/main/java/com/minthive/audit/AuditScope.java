package com.minthive.audit;

import com.minthive.entity.AuditLog;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次请求/后台任务的审计采集上下文
 *
 * <p>功能描述：由 Web 拦截器在请求开始时创建，MyBatis 拦截器把已提交的表变更放入其中，
 * 请求结束时组装成审计主记录并投递到异步写队列</p>
 * <p>注意事项：必须在线程复用前调用 {@link #clear()}，避免把上一个请求的审计数据带到下一个请求</p>
 */
@Getter
public class AuditScope {

    /** 线程上下文 */
    private static final ThreadLocal<AuditScope> HOLDER = new ThreadLocal<>();

    /** 来源: HTTP/SYSTEM */
    private final String source;

    /** 是否采集（GET 请求为 false） */
    private final boolean auditable;

    /** 请求开始时间（纳秒） */
    private final long startNanos;

    /** HTTP 方法 */
    private final String httpMethod;

    /** 请求路径 */
    private final String requestPath;

    /** 查询串 */
    private final String requestQuery;

    /** 直接连接地址 */
    private final String remoteAddr;

    /** X-Forwarded-For 原文 */
    private final String forwardedFor;

    /** 可信代理解析出的客户端IP */
    private final String clientIp;

    /** User-Agent */
    private final String userAgent;

    /** 已采集的表变更 */
    private final List<AuditChangeRecord> changes = new ArrayList<>();

    /** 匿名尝试账号或系统任务名 */
    @Setter
    private String operatorLabel;

    /** 匿名身份指纹 SHA-256(手机号) */
    @Setter
    private String identityHash;

    /** 业务响应码 */
    @Setter
    private Integer resultCode;

    /** 业务响应消息 */
    @Setter
    private String resultMsg;

    /** 是否存在截断快照 */
    private boolean truncated;

    private AuditScope(String source, boolean auditable, long startNanos, String httpMethod, String requestPath,
                       String requestQuery, String remoteAddr, String forwardedFor, String clientIp, String userAgent) {
        this.source = source;
        this.auditable = auditable;
        this.startNanos = startNanos;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.requestQuery = requestQuery;
        this.remoteAddr = remoteAddr;
        this.forwardedFor = forwardedFor;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }

    /**
     * 创建 HTTP 请求审计上下文
     *
     * @param auditable    是否为需要审计的写请求
     * @param httpMethod   HTTP 方法
     * @param requestPath  请求路径
     * @param requestQuery 查询串
     * @param remoteAddr   直接连接地址
     * @param forwardedFor X-Forwarded-For 原文
     * @param clientIp     可信代理解析出的客户端IP
     * @param userAgent    User-Agent
     * @return 审计上下文
     */
    public static AuditScope beginHttp(boolean auditable, String httpMethod, String requestPath, String requestQuery,
                                      String remoteAddr, String forwardedFor, String clientIp, String userAgent) {
        AuditScope scope = new AuditScope("HTTP", auditable, System.nanoTime(), httpMethod, requestPath, requestQuery,
                remoteAddr, forwardedFor, clientIp, userAgent);
        HOLDER.set(scope);
        return scope;
    }

    /**
     * 创建后台任务审计上下文
     *
     * @param taskName 任务/线程名
     * @return 审计上下文
     */
    public static AuditScope beginSystem(String taskName) {
        AuditScope scope = new AuditScope("SYSTEM", true, System.nanoTime(), null, null, null,
                null, null, null, null);
        scope.operatorLabel = taskName;
        HOLDER.set(scope);
        return scope;
    }

    /**
     * 获取当前线程的审计上下文
     *
     * @return 上下文，不存在返回 null
     */
    public static AuditScope current() {
        return HOLDER.get();
    }

    /**
     * 记录一条表变更
     *
     * @param change 变更记录
     */
    public void addChange(AuditChangeRecord change) {
        changes.add(change);
        if (change.truncated()) {
            truncated = true;
        }
    }

    /**
     * 取出并清空已采集的变更（后台任务逐次提交时使用）
     *
     * @return 当前已采集的变更副本
     */
    public List<AuditChangeRecord> drainChanges() {
        List<AuditChangeRecord> snapshot = new ArrayList<>(changes);
        changes.clear();
        truncated = false;
        return snapshot;
    }

    /**
     * 清理线程上下文
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 组装审计主记录
     *
     * @param operatorType USER/ADMIN/ANONYMOUS/SYSTEM
     * @param operatorId   用户ID（可为空）
     * @param operatorRole 数据库角色（可为空）
     * @param traceId      链路ID
     * @param httpStatus   HTTP 状态码（可为空）
     * @param durationMs   耗时毫秒
     * @return 审计主记录
     */
    public AuditLog toAuditLog(String operatorType, Long operatorId, Integer operatorRole, String traceId,
                               Integer httpStatus, Integer durationMs) {
        AuditLog log = new AuditLog();
        log.setTraceId(traceId);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole);
        log.setOperatorLabel(operatorLabel);
        log.setIdentityHash(identityHash);
        log.setSource(source);
        log.setHttpMethod(httpMethod);
        log.setRequestPath(requestPath);
        log.setRequestQuery(requestQuery);
        log.setClientIp(clientIp);
        log.setRemoteAddr(remoteAddr);
        log.setForwardedFor(forwardedFor);
        log.setUserAgent(userAgent);
        log.setResultCode(resultCode);
        log.setResultMsg(resultMsg);
        log.setHttpStatus(httpStatus);
        log.setDurationMs(durationMs);
        log.setChangeCount(changes.size());
        log.setTruncated(truncated ? 1 : 0);
        return log;
    }
}
