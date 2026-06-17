import { get, post } from './request'

/**
 * 管理员认证 API
 * 功能：管理员登录、登出、获取当前登录信息
 */

/** 登录入参 */
export interface LoginParams {
  account: string
  password: string
  captcha?: string
}

/** 登录返回 */
export interface LoginResult {
  token: string
  adminId: number
  account: string
  nickname: string
  avatar?: string
  role: string
}

/** 管理员信息 */
export interface AdminInfo {
  adminId: number
  account: string
  nickname: string
  avatar?: string
  role: string
  permissions: string[]
}

/**
 * 管理员登录
 * @param params 账号密码
 * @returns Token 与管理员基础信息
 */
export function login(params: LoginParams) {
  return post<LoginResult>('/auth/login', params)
}

/**
 * 退出登录
 */
export function logout() {
  return post('/auth/logout')
}

/**
 * 获取当前登录管理员信息
 */
export function getAdminInfo() {
  return get<AdminInfo>('/auth/info')
}
