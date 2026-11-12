package com.minthive.audit;

import com.minthive.entity.AuditLog;
import com.minthive.entity.AuditLogChange;

import java.util.List;

/**
 * 待异步落库的一次审计事件
 *
 * @param log     审计主记录
 * @param changes 变更子记录
 */
public record AuditEvent(AuditLog log, List<AuditLogChange> changes) {
}
