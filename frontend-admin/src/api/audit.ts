import { get } from './request'
import type { PageQuery, PageResult } from './types'

/**
 * 操作审计 API
 * 功能：审计日志分页查询、详情与 CSV 导出
 */

/** 审计查询参数 */
export interface AuditQuery extends PageQuery {
  operatorType?: string
  operatorId?: number
  tableName?: string
  operation?: string
  resultCode?: number
  path?: string
  traceId?: string
  startTime?: string
  endTime?: string
}

/** 审计主记录 */
export interface AuditLog {
  id: number
  traceId?: string
  operatorType: 'USER' | 'ADMIN' | 'ANONYMOUS' | 'SYSTEM'
  operatorId?: number
  operatorRole?: number
  operatorLabel?: string
  source: 'HTTP' | 'SYSTEM'
  httpMethod?: string
  requestPath?: string
  requestQuery?: string
  clientIp?: string
  remoteAddr?: string
  userAgent?: string
  resultCode?: number
  resultMsg?: string
  httpStatus?: number
  durationMs?: number
  changeCount: number
  truncated: number
  createTime: string
}

/** 审计变更记录 */
export interface AuditChange {
  id: number
  auditLogId: number
  tableName: string
  recordId?: string
  operation: 'INSERT' | 'UPDATE' | 'DELETE'
  affectedRows?: number
  beforeJson?: string
  afterJson?: string
  maskedColumns?: string
  truncated: number
  sqlDigest?: string
}

/** 审计详情 */
export interface AuditDetail {
  log: AuditLog
  changes: AuditChange[]
}

/**
 * 分页查询审计日志
 */
export function getAuditPage(params: AuditQuery) {
  return get<PageResult<AuditLog>>('/audit/page', params)
}

/**
 * 查询审计详情
 */
export function getAuditDetail(id: number) {
  return get<AuditDetail>(`/audit/${id}`)
}

/**
 * 导出审计日志 CSV
 */
export function exportAudit(params: AuditQuery): Promise<Blob> {
  return get('/audit/export', params, { responseType: 'blob' })
}
