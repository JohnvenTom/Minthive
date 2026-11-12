/**
 * Axios 请求封装
 * @module api/request
 * @description 统一拦截器、Token 注入、错误处理、401 跳转登录
 */

import axios, { AxiosInstance, AxiosRequestConfig, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearToken } from '@/utils/token'
import type { ApiResponse } from '@/types'
import { normalizePagination } from './normalize'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? `${import.meta.env.VITE_API_BASE_URL}/api` : '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// ---------- 请求拦截器：注入 Token + 处理 FormData ----------
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // FormData 请求必须删除 Content-Type，让浏览器自动设置含 boundary 的 multipart 头
    // 手动设置 Content-Type: multipart/form-data 会缺少 boundary，导致后端无法解析
    // Axios 1.x 使用 AxiosHeaders 类，需用 delete() 方法而非 JS delete 操作符
    if (config.data instanceof FormData) {
      config.headers.delete('Content-Type')
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------- 是否正在跳转登录（避免重复跳转） ----------
let isRedirecting = false

/**
 * 401 跳转登录
 * @param {string} message - 自定义提示信息(默认: "登录已过期,请重新登录")
 */
function redirectToLogin(message: string = '登录已过期,请重新登录'): void {
  if (isRedirecting) return
  isRedirecting = true
  clearToken()
  ElMessage.warning(message)
  const redirect = encodeURIComponent(location.pathname + location.search)
  setTimeout(() => {
    location.href = `/login?redirect=${redirect}`
    isRedirecting = false
  }, 800)
}

// ---------- 响应拦截器：统一错误处理 + 分页格式兼容 ----------
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse
    // 二进制流直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    if (res.code === 200) {
      res.data = normalizePagination(res.data)
      return response
    }
    // 业务错误
    if (res.code === 401) {
      redirectToLogin()
      return Promise.reject(new Error(res.msg || '未登录'))
    }
    
    /**
     * 401 未登录或登录失效处理(区分不同场景)
     * - 1006 TOKEN_EXPIRED: Token自然过期或已登出
     * - 1007 ACCOUNT_KICKED_OUT: 账号在其他设备登录(被踢下)
     */
    if (res.code === 1006 || res.code === 1007) {
      // 根据错误码显示不同提示
      const message = res.code === 1007 
        ? '账号在其他设备登录,当前设备已退出' 
        : '登录已过期,请重新登录'
      redirectToLogin(message)
      return Promise.reject(new Error(res.msg || message))
    }
    if (res.code === 403) {
      ElMessage.warning(res.msg || 'AI 调用次数已达上限')
      return Promise.reject(new Error(res.msg))
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || 'Error'))
  },
  (error: AxiosError) => {
    const status = error.response?.status
    if (status === 401) {
      // HTTP 401状态码统一使用默认提示
      redirectToLogin()
    } else if (status === 403) {
      ElMessage.warning('AI 调用次数已达今日上限')
    } else if (status && status >= 500) {
      ElMessage.error('服务器开小差了，请稍后再试')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络')
    } else {
      ElMessage.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

/**
 * 通用请求方法
 * @param {AxiosRequestConfig} config
 * @returns {Promise<ApiResponse>}
 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.request<ApiResponse<T>>(config).then(response => response.data)
}

/** 在 API 边界将后端 DTO 转为视图模型，保留统一业务响应。 */
export async function mappedRequest<T, R>(config: AxiosRequestConfig, normalize: (data: T) => R): Promise<ApiResponse<R>> {
  const response = await request<T>(config)
  return { ...response, data: normalize(response.data) }
}

export default service
