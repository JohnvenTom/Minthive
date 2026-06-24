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

// ---------- 响应拦截器：统一错误处理 + 分页格式兼容 ----------
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse
    // 二进制流直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    if (res.code === 200) {
      /**
       * 兼容后端 MyBatis-Plus Page 返回格式
       * 后端返回: { records, total, current, size, pages }
       * 前端期望: { list, total, page, pageSize, hasMore }
       */
      const data = res.data
      if (data && typeof data === 'object' && 'records' in data && !('list' in data)) {
        const page = data as any
        // 兼容后端 MyBatis-Plus Page 格式，同时保留 records 和 list 字段
        Object.assign(page, {
          list: page.records || [],
          page: page.current || 1,
          pageSize: page.size || 10,
          hasMore: (page.current || 1) < (page.pages || 1)
        })
      }
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
