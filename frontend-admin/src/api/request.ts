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
    const res = response.data as ApiResponse
    if (res.code === 200) {
      // 将业务数据挂回 response.data，保持 AxiosResponse 结构以通过类型校验
      response.data = res.data as any
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
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
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
export function get<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.get(url, { params, ...config }).then((res) => res.data as ApiResponse<T>)
}

/**
 * POST 请求
 * @param url 请求地址
 * @param data 请求体
 */
export function post<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.post(url, data, config).then((res) => res.data as ApiResponse<T>)
}

/**
 * PUT 请求
 * @param url 请求地址
 * @param data 请求体
 */
export function put<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.put(url, data, config).then((res) => res.data as ApiResponse<T>)
}

/**
 * DELETE 请求
 * @param url 请求地址
 * @param params 查询参数
 */
export function del<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.delete(url, { params, ...config }).then((res) => res.data as ApiResponse<T>)
}

export default service
