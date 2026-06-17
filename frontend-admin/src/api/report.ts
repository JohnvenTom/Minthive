import { get, post } from './request'
import type { PageQuery, PageResult, RiskLevel, WorkOrderStatus } from './types'

/**
 * 举报工单 API
 * 功能：列表、查看、驳回、删除、处罚，按 AI 风险等级排序
 */

/** 举报工单查询参数 */
export interface ReportQuery extends PageQuery {
  status?: WorkOrderStatus
  riskLevel?: RiskLevel
  type?: string
}

/** 举报工单 */
export interface ReportWorkOrder {
  workOrderId: number
  reporterId: number
  reporterName: string
  targetId: number
  targetType: string
  targetContent: string
  accusedId: number
  accusedName: string
  type: string
  reason: string
  status: WorkOrderStatus
  riskLevel: RiskLevel
  aiAnalysis?: string
  handleResult?: string
  createTime: string
  handleTime?: string
}

/** 处罚入参 */
export interface PunishParams {
  workOrderId: number
  action: 'DELETE_CONTENT' | 'BAN_USER' | 'WARN'
  duration?: number
  remark: string
}

/**
 * 分页查询举报工单（默认按风险等级排序）
 */
export function getReportList(params: ReportQuery) {
  return get<PageResult<ReportWorkOrder>>('/report/list', params)
}

/**
 * 获取工单详情
 */
export function getReportDetail(workOrderId: number) {
  return get<ReportWorkOrder>('/report/detail', { workOrderId })
}

/**
 * 驳回举报
 */
export function rejectReport(workOrderId: number, remark: string) {
  return post('/report/reject', { workOrderId, remark })
}

/**
 * 删除违规内容
 */
export function deleteReportContent(workOrderId: number) {
  return post('/report/delete-content', { workOrderId })
}

/**
 * 处罚违规用户
 */
export function punishUser(params: PunishParams) {
  return post('/report/punish', params)
}
