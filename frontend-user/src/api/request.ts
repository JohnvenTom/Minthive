/**
 * Axios 请求封装
 * @module api/request
 * @description 统一拦截器、Token 注入、错误处理、401 跳转登录
 */

import axios, { AxiosInstance, AxiosRequestConfig, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearToken } from '@/utils/token'
import type { ApiResponse } from '@/types'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// ---------- 请求拦截器：注入 Token ----------
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------- 是否正在跳转登录（避免重复跳转） ----------
let isRedirecting = false

/**
 * 401 跳转登录
 */
function redirectToLogin(): void {
  if (isRedirecting) return
  isRedirecting = true
  clearToken()
  ElMessage.warning('登录已过期，请重新登录')
  const redirect = encodeURIComponent(location.pathname + location.search)
  setTimeout(() => {
    location.href = `/login?redirect=${redirect}`
    isRedirecting = false
  }, 800)
}

// ---------- 响应拦截器：统一错误处理 ----------
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse
    // 二进制流直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    if (res.code === 200) {
      return res
    }
    // 业务错误
    if (res.code === 401) {
      redirectToLogin()
      return Promise.reject(new Error(res.msg || '未登录'))
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
export function request<T = any>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service(config) as unknown as Promise<ApiResponse<T>>
}

export default service
