import { get, post, put, del } from './request'

/**
 * 系统配置 API
 * 功能：公告、轮播图、规则、圈主·管理员管理、AI 开关
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

/** 角色用户（圈主/管理员） */
export interface RoleUser {
  userId: number
  account: string
  nickname: string
  avatar: string
  role: number
  status: number
  registerTime: string
}

/** 可选取的普通用户 */
export interface NormalUser {
  userId: number
  account: string
  nickname: string
  avatar: string
  registerTime: string
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
 * 圈主·管理员管理（对接 AdminUserController，baseURL 已含 /api/admin）
 */
export function getRoleList(roles = '1,2', keyword?: string) {
  return get<RoleUser[]>('/user/role-list', { roles, keyword })
}
export function searchNormalUser(keyword: string) {
  return get<NormalUser[]>('/user/search-normal', { keyword })
}
export function createUserWithRole(data: { account: string; password: string; nickname: string; role: string }) {
  return post<{ userId: number; account: string; nickname: string; role: number }>('/user/create-with-role', data)
}
export function setUserRole(userId: number, role: string) {
  return put(`/user/role/${userId}`, { role })
}
export function removeUserRole(userId: number) {
  return del(`/user/role/${userId}`)
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
