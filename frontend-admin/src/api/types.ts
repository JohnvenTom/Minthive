/**
 * 通用 API 类型定义
 * 功能：定义后端统一响应格式与分页结构
 */

/** 统一响应结构 */
export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 分页响应结构 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

/** 分页查询参数 */
export interface PageQuery {
  page?: number
  pageSize?: number
  keyword?: string
}

/** 风险等级枚举 */
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

/** 用户状态枚举 */
export type UserStatus = 'NORMAL' | 'BANNED' | 'DELETED'

/** 内容审核状态 */
export type AuditStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** 工单状态 */
export type WorkOrderStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED'
