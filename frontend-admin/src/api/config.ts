import { get, post, put, del } from './request'

/**
 * 系统配置 API
 * 功能：公告、轮播图、规则、操作员管理、AI 开关
 */

/** 系统公告 */
export interface Announcement {
  id: number
  title: string
  content: string
  status: number
  publishTime: string
}

/** 轮播图 */
export interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  sort: number
  status: string
}

/** 平台规则 */
export interface PlatformRule {
  dailyPostLimit: number
  imageMaxSize: number
  videoMaxDuration: number
  videoMaxSize: number
  aiDailyLimit: number
}

/** 操作员 */
export interface Operator {
  adminId: number
  account: string
  nickname: string
  role: string
  status: number
  createTime: string
}

/** AI 开关配置 */
export interface AiSwitchConfig {
  enabled: boolean
  textGenerate: boolean
  commentGenerate: boolean
  contentDetect: boolean
  recommend: boolean
  imageProcess: boolean
  privateReply: boolean
  qaAssistant: boolean
}

/**
 * 公告相关（后端已迁移至数据库，返回分页数据）
 */
export function getAnnouncements(current = 1, size = 20) {
  return get<{ records: Announcement[]; total: number }>('/config/announcements', { current, size })
}
export function saveAnnouncement(data: Partial<Announcement>) {
  return post<Announcement>('/config/announcement', data)
}
export function deleteAnnouncement(id: number) {
  return del('/config/announcement', { id })
}

/**
 * 轮播图相关
 */
export function getBanners() {
  return get<Banner[]>('/config/banners')
}
export function saveBanner(data: Partial<Banner>) {
  return post('/config/banner', data)
}
export function deleteBanner(id: number) {
  return del('/config/banner', { id })
}

/**
 * 平台规则
 */
export function getPlatformRules() {
  return get<PlatformRule>('/config/rules')
}
export function updatePlatformRules(data: PlatformRule) {
  return put('/config/rules', data)
}

/**
 * 操作员管理
 */
export function getOperators() {
  return get<Operator[]>('/config/operators')
}
export function createOperator(data: Partial<Operator> & { password: string }) {
  return post('/config/operator', data)
}
export function updateOperator(data: Partial<Operator>) {
  return put('/config/operator', data)
}
export function deleteOperator(adminId: number) {
  return del('/config/operator', { adminId })
}

/**
 * AI 开关配置
 */
export function getAiSwitch() {
  return get<AiSwitchConfig>('/config/ai-switch')
}
export function updateAiSwitch(data: AiSwitchConfig) {
  put('/config/ai-switch', data)
}
