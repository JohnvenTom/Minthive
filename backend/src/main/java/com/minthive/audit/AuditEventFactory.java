package com.minthive.audit;

import com.minthive.entity.AuditLog;
import com.minthive.entity.AuditLogChange;

import java.util.ArrayList;
import java.util.List;

/**
 * 审计事件组装工具
 *
 * <p>功能描述：把采集到的变更记录转换为可落库的审计事件</p>
 */
public final class AuditEventFactory {

    private AuditEventFactory() {
    }

    /**
     * 组装审计事件
     *
     * @param log     审计主记录
     * @param changes 变更记录
     * @return 审计事件；无变更时仍保留主记录（用于登录失败等未产生表变更的写请求）
     */
    public static AuditEvent event(AuditLog log, List<AuditChangeRecord> changes) {
        List<AuditLogChange> rows = new ArrayList<>();
        if (changes != null) {
            for (AuditChangeRecord change : changes) {
                AuditLogChange row = new AuditLogChange();
                row.setTableName(change.tableName());
                row.setRecordId(change.recordId());
                row.setOperation(change.operation());
                row.setAffectedRows(change.affectedRows());
                row.setBeforeJson(change.beforeJson());
                row.setAfterJson(change.afterJson());
                row.setMaskedColumns(change.maskedColumns());
                row.setTruncated(change.truncated() ? 1 : 0);
                row.setSqlDigest(change.sqlDigest());
                rows.add(row);
            }
        }
        return new AuditEvent(log, rows);
    }
}
