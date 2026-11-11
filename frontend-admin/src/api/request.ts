import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types'

/**
 * Axios 请求封装
 * 功能：统一 baseURL、Token 注入、错误处理、401 跳转登录
 * 参数：无
 * 返回值：封装后的请求方法集合
 * 异常：网络错误/超时统一弹窗提示，401 清除 token 并跳转登录
 * 注意事项：Token 从 localStorage 读取，键名 admin_token
 */

const TOKEN_KEY = 'admin_token'

/** 获取本地存储的 Token */
export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 设置 Token */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 清除 Token */
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

const service: AxiosInstance = axios.create({
  baseURL: '/api/admin',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

/** 请求拦截器：注入 Token */
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应拦截器：统一处理业务码 */
service.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.config.responseType === 'blob') return response
    const res = response.data as ApiResponse
    if (res.code === 200) {
      return response
    }
    // 401 未登录
    if (res.code === 401) {
      clearToken()
      ElMessage.error('登录已过期，请重新登录')
      setTimeout(() => {
        window.location.href = '/login'
      }, 1000)
      return Promise.reject(new Error(res.msg || '未登录'))
    }
    
    /**
     * 401 未登录或登录失效处理(区分不同场景)
     * - 1006 TOKEN_EXPIRED: Token自然过期或已登出
     * - 1007 ACCOUNT_KICKED_OUT: 账号在其他设备登录(被踢下)
     */
    if (res.code === 1006 || res.code === 1007) {
      clearToken()
      // 根据错误码显示不同提示
      const message = res.code === 1007 
        ? '账号在其他设备登录,当前设备已退出' 
        : '登录已过期,请重新登录'
      ElMessage.error(message)
      setTimeout(() => {
        window.location.href = '/login'
      }, 1000)
      return Promise.reject(new Error(res.msg || message))
    }
    // 403 无权限
    if (res.code === 403) {
      clearToken()
      ElMessage.error('无权限访问后台')
      setTimeout(() => {
        window.location.href = '/login'
      }, 1000)
      return Promise.reject(new Error(res.msg || '无权限'))
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    // HTTP 层面 401：Token 过期或无效，清除并跳转登录页
    if (error.response?.status === 401) {
      clearToken()
      ElMessage.error('登录已过期，请重新登录')
      setTimeout(() => {
        window.location.href = '/login'
      }, 1000)
      return Promise.reject(error)
    }
    // HTTP 层面 403：无权限
    if (error.response?.status === 403) {
      clearToken()
      ElMessage.error('无权限访问后台')
      setTimeout(() => {
        window.location.href = '/login'
      }, 1000)
      return Promise.reject(error)
    }
    const message = error.response?.data?.msg || error.message || '网络异常'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/**
 * GET 请求
 * @param url 请求地址
 * @param params 查询参数
 */
export function get(url: string, params: object | undefined, config: AxiosRequestConfig & { responseType: 'blob' }): Promise<Blob>
export function get<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T>
export function get<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T | Blob> {
  if (config?.responseType === 'blob') {
    return service.get<Blob>(url, { params, ...config }).then(res => res.data)
  }
  return service.get<ApiResponse<T>>(url, { params, ...config }).then((res) => res.data.data)
}

/**
 * POST 请求
 * @param url 请求地址
 * @param data 请求体
 */
export function post<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.post<ApiResponse<T>>(url, data, config).then((res) => res.data.data)
}

/**
 * PUT 请求
 * @param url 请求地址
 * @param data 请求体
 */
export function put<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.put<ApiResponse<T>>(url, data, config).then((res) => res.data.data)
}

/**
 * DELETE 请求
 * @param url 请求地址
 * @param params 查询参数
 */
export function del<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.delete<ApiResponse<T>>(url, { params, ...config }).then((res) => res.data.data)
}

export default service
