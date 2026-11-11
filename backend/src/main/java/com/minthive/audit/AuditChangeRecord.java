package com.minthive.audit;

/**
 * 单条表变更审计记录
 *
 * @param tableName     被修改的表名
 * @param recordId      主键值（无法解析时为空）
 * @param operation     INSERT/UPDATE/DELETE
 * @param affectedRows  影响行数
 * @param beforeJson    变更前快照（已脱敏/截断，可为空）
 * @param afterJson     变更后快照（已脱敏/截断，可为空）
 * @param maskedColumns 被脱敏列名，逗号分隔
 * @param truncated     快照是否被截断
 * @param sqlDigest     无法解析主键时的 SQL 模板摘要
 */
public record AuditChangeRecord(
        String tableName,
        String recordId,
        String operation,
        int affectedRows,
        String beforeJson,
        String afterJson,
        String maskedColumns,
        boolean truncated,
        String sqlDigest) {
}
