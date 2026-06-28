import { get, post, put } from './request'
import type { PageQuery, PageResult } from './types'

/**
 * 用户管理 API
 * 功能：用户列表、详情、封禁解封、重置密码、清理僵尸号
 */

/** 用户列表查询参数 */
export interface UserQuery extends PageQuery {
  status?: string
  startDate?: string
  endDate?: string
}

/** 用户信息 */
export interface UserInfo {
  userId: number
  account: string
  nickname: string
  avatar: string
  phone: string
  gender: number
  status: string
  postCount: number
  followCount: number
  fansCount: number
  registerTime: string
  lastLoginTime: string
  interests: string[]
}

/** 创建用户入参 */
export interface CreateUserParams {
  account: string
  password: string
  nickname: string
  phone?: string
}

/** 封禁入参 */
export interface BanParams {
  userId: number
  duration: number // 0 表示永久封禁，单位小时
  reason: string
}

/**
 * 分页查询用户列表
 * @param params 查询参数
 */
export function getUserList(params: UserQuery) {
  return get<PageResult<UserInfo>>('/user/list', params)
}

/**
 * 获取用户详情
 * @param userId 用户ID
 */
export function getUserDetail(userId: number) {
  return get<UserInfo>('/user/detail', { userId })
}

/**
 * 封禁用户
 * @param params 封禁参数
 */
export function banUser(params: BanParams) {
  return post('/user/ban', params)
}

/**
 * 解封用户
 * @param userId 用户ID
 */
export function unbanUser(userId: number) {
  return post('/user/unban', { userId })
}

/**
 * 重置用户密码
 * @param userId 用户ID
 * @param newPassword 新密码
 */
export function resetPassword(userId: number, newPassword: string) {
  return post('/user/reset-password', { userId, newPassword })
}

/**
 * 创建新用户（普通用户）
 * @param params 用户信息
 */
export function createUser(params: CreateUserParams) {
  return post('/user/create', params)
}

/**
 * 清理僵尸账号（长期未登录、无发帖）
 */
export function cleanZombieUsers() {
  return post<{ cleaned: number }>('/user/clean-zombie')
}

/**
 * 获取用户发帖/互动记录
 * @param userId 用户ID
 */
export function getUserActions(userId: number) {
  return get('/user/actions', { userId })
}
