import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAdminInfo, logout as logoutApi, type AdminInfo } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/api/request'

const THEME_KEY = 'minthive_admin_theme'

/**
 * 管理员状态 Store
 * 功能：管理登录态、管理员信息、登出、主题切换
 * 参数：无
 * 返回值：Pinia store 实例
 * 注意事项：刷新页面后从 localStorage 恢复 token，并重新拉取管理员信息
 */
export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(getToken())
  const adminInfo = ref<AdminInfo | null>(null)

  /** 当前主题 */
  const theme = ref<'light' | 'dark'>(
    (localStorage.getItem(THEME_KEY) as 'light' | 'dark') || 'dark'
  )
  // 初始化时同步 data-theme 属性
  document.documentElement.setAttribute('data-theme', theme.value)

  /**
   * 设置 Token 并持久化
   * @param value token 字符串
   */
  function setAdminToken(value: string) {
    token.value = value
    setToken(value)
  }

  /**
   * 拉取当前管理员信息
   * @returns 管理员信息对象（含角色、权限等）
   */
  async function fetchAdminInfo() {
    if (!token.value) return
    const info = await getAdminInfo()
    adminInfo.value = info
    return info
  }

  /**
   * 切换主题
   * @param newTheme 目标主题，不传则切换
   */
  function setTheme(newTheme?: 'light' | 'dark'): void {
    theme.value = newTheme || (theme.value === 'light' ? 'dark' : 'light')
    document.documentElement.setAttribute('data-theme', theme.value)
    localStorage.setItem(THEME_KEY, theme.value)
  }

  /**
   * 退出登录
   */
  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // 忽略登出接口错误
    }
    token.value = ''
    adminInfo.value = null
    clearToken()
  }

  return {
    token,
    adminInfo,
    theme,
    setAdminToken,
    fetchAdminInfo,
    setTheme,
    logout
  }
})
