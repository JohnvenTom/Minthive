import { get } from './request'

/**
 * 数据统计 API
 * 功能：注册趋势、日活月活、发帖量、互动量、圈子活跃、举报工单、AI 日报
 */

/** 时间维度 */
export type TimeRange = 'DAY' | 'WEEK' | 'MONTH'

/** 核心指标 */
export interface CoreMetrics {
  totalUsers: number
  todayRegister: number
  dau: number
  mau: number
  totalPosts: number
  todayPosts: number
  totalInteractions: number
  pendingReports: number
}

/** 趋势数据点 */
export interface TrendPoint {
  date: string
  value: number
}

/** 多系列趋势 */
export interface MultiTrend {
  dates: string[]
  series: { name: string; data: number[] }[]
}

/** 圈子活跃排行 */
export interface CircleActive {
  circleId: number
  name: string
  memberCount: number
  postCount: number
  interactionCount: number
}

/** AI 日报 */
export interface AiDailyReport {
  reportDate: string
  healthScore: number
  violationTypes: { type: string; count: number }[]
  peakHours: { hour: string; count: number }[]
  suggestions: string[]
  riskDistribution: { level: string; count: number }[]
  /** AI 模式: cloud=云端大模型, local=本地私有化模型 */
  aiMode?: 'cloud' | 'local'
}

/**
 * 获取核心指标
 */
export function getCoreMetrics() {
  return get<CoreMetrics>('/stats/metrics')
}

/**
 * 注册趋势
 */
export function getRegisterTrend(range: TimeRange) {
  return get<TrendPoint[]>('/stats/register-trend', { range })
}

/**
 * 日活月活趋势
 */
export function getActiveTrend(range: TimeRange) {
  return get<MultiTrend>('/stats/active-trend', { range })
}

/**
 * 发帖量趋势
 */
export function getPostTrend(range: TimeRange) {
  return get<TrendPoint[]>('/stats/post-trend', { range })
}

/**
 * 互动量趋势
 */
export function getInteractionTrend(range: TimeRange) {
  return get<MultiTrend>('/stats/interaction-trend', { range })
}

/**
 * 圈子活跃排行
 */
export function getCircleActiveRank() {
  return get<CircleActive[]>('/stats/circle-active')
}

/**
 * 举报工单统计
 */
export function getReportStats(range: TimeRange) {
  return get<MultiTrend>('/stats/report', { range })
}

/**
 * 获取 AI 运营日报
 * 注意：此接口调用大模型生成建议，响应较慢，单独设置 60 秒超时
 */
export function getAiDailyReport() {
  return get<AiDailyReport>('/stats/ai-report', undefined, { timeout: 60_000 })
}

/**
 * 导出 Excel 报表
 * @param range 时间维度
 */
export function exportStatsExcel(range: TimeRange) {
  return get('/stats/export', { range }, { responseType: 'blob' })
}
