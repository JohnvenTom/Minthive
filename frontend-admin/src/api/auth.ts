import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types'
import { clearToken } from './request'

/**
 * 管理员认证 API
 * 功能：管理员登录、登出、获取当前登录信息
 * 注意事项：认证接口走 /api/auth（与用户端共用），不走 /api/admin 前缀，
 *           因此使用独立的 axios 实例，baseURL 设为 /api
 */

/** 认证专用 axios 实例（baseURL=/api，与用户端共用认证接口） */
const authService: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
})

/**
 * 请求拦截器：注入 Token
 * 功能：从 localStorage 读取 admin_token 并注入请求头 Authorization
 */
authService.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 响应拦截器：统一处理业务码
 * 功能：code=200 时提取 data，code=401 时清除 token 并提示登录过期，
 *       其他错误码弹窗提示并 reject
 */
authService.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse
    if (res.code === 200) {
      response.data = res.data as any
      return response
    }
    // 401 未登录或 token 过期
    if (res.code === 401) {
      clearToken()
      ElMessage.error('登录已过期，请重新登录')
      setTimeout(() => {
        window.location.href = '/login'
      }, 1000)
      return Promise.reject(new Error(res.msg || '未登录'))
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const message = error.response?.data?.msg || error.message || '网络异常'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

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
 *
 * @param params 账号密码
 * @returns Token 与管理员基础信息
 */
export function login(params: LoginParams) {
  return authService.post<LoginResult>('/auth/login', params).then((res) => res.data as LoginResult)
}

/**
 * 退出登录
 */
export function logout() {
  return authService.post('/auth/logout').then((res) => res.data)
}

/**
 * 获取当前登录管理员信息
 *
 * @returns 当前登录管理员的详细信息（角色、权限等）
 */
export function getAdminInfo() {
  return authService.get<AdminInfo>('/auth/info').then((res) => res.data as AdminInfo)
}
