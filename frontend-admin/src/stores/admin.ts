import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAdminInfo, logout as logoutApi, type AdminInfo } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/api/request'

/**
 * 管理员状态 Store
 * 功能：管理登录态、管理员信息、登出
 * 参数：无
 * 返回值：Pinia store 实例
 * 注意事项：刷新页面后从 localStorage 恢复 token，并重新拉取管理员信息
 */
export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(getToken())
  const adminInfo = ref<AdminInfo | null>(null)

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
   */
  async function fetchAdminInfo() {
    if (!token.value) return
    const res = await getAdminInfo()
    adminInfo.value = res.data
    return res.data
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
    setAdminToken,
    fetchAdminInfo,
    logout
  }
})
