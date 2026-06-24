/**
 * 认证相关 API
 * @module api/auth
 */

import { request } from './request'
import type { User } from '@/types'

/**
 * 账号密码登录
 * @param {string} account - 账号/手机号
 * @param {string} password - 密码
 * @returns {Promise<{token: string, user: User}>}
 */
export function loginByPassword(account: string, password: string) {
  return request<{ token: string; user: User }>({
    url: '/auth/login',
    method: 'post',
    data: { account, password }
  })
}

/**
 * 手机验证码登录
 * @param {string} phone
 * @param {string} code
 */
export function loginBySms(phone: string, code: string) {
  return request<{ token: string; user: User }>({
    url: '/auth/login/sms',
    method: 'post',
    data: { phone, code }
  })
}

/**
 * 注册
 * @param {object} data - 注册信息
 */
export function register(data: {
  account: string
  password: string
  phone: string
  code: string
}) {
  return request<{ token: string; user: User }>({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 发送短信验证码
 * @param {string} phone
 * @param {'register' | 'login' | 'reset'} scene
 */
export function sendSmsCode(phone: string, scene: 'register' | 'login' | 'reset') {
  return request({ url: '/auth/sms', method: 'post', data: { phone, scene } })
}

/**
 * 重置密码
 */
export function resetPassword(phone: string, code: string, password: string) {
  return request({ url: '/auth/reset', method: 'post', data: { phone, code, password } })
}

/**
 * 退出登录
 */
export function logout() {
  return request({ url: '/auth/logout', method: 'post' })
}

/**
 * 修改密码（需登录状态）
 * @param {string} oldPassword - 当前密码
 * @param {string} newPassword - 新密码
 * @returns {Promise<void>}
 * @description 调用后端接口更新用户登录密码，成功后需要重新登录
 */
export function changePassword(oldPassword: string, newPassword: string) {
  return request({ url: '/user/password', method: 'put', data: { oldPassword, newPassword } })
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request<User>({ url: '/user/me', method: 'get' })
}
