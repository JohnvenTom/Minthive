/**
 * Token 管理工具
 * @module utils/token
 * @description 统一管理用户登录态 Token 的存取与清理，支持 localStorage 持久化
 */

const TOKEN_KEY = 'minthive_token'
const REFRESH_KEY = 'minthive_refresh'

/**
 * 获取访问 Token
 * @returns {string | null} 当前 Token，未登录返回 null
 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置访问 Token
 * @param {string} token - 登录后获得的 JWT
 * @returns {void}
 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 获取刷新 Token
 * @returns {string | null}
 */
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

/**
 * 设置刷新 Token
 * @param {string} token
 */
export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_KEY, token)
}

/**
 * 清除全部 Token（退出登录时调用）
 * @returns {void}
 */
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}
