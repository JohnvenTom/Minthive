/**
 * 用户状态管理
 * @module stores/user
 * @description 管理用户登录态、Token、用户信息，提供登录/登出/获取用户信息等操作
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken as saveToken, clearToken } from '@/utils/token'
import { getCurrentUser, logout as logoutApi } from '@/api/auth'
import type { User } from '@/types'

export const useUserStore = defineStore('user', () => {
  // ---------- 状态 ----------

  /** 访问 Token */
  const token = ref<string | null>(getToken())

  /** 当前用户信息 */
  const userInfo = ref<User | null>(null)

  // ---------- 计算属性 ----------

  /**
   * 是否已登录
   * @returns {boolean} token 存在即为已登录
   */
  const isLoggedIn = computed(() => !!token.value)

  // ---------- Actions ----------

  /**
   * 设置 Token 并持久化
   * @param {string} newToken - 登录后获得的 JWT
   * @returns {void}
   */
  function setToken(newToken: string): void {
    token.value = newToken
    saveToken(newToken)
  }

  /**
   * 获取当前用户信息
   * @description 从服务端拉取最新用户信息并更新本地状态；
   *   若 Token 无效会触发 401 拦截器自动跳转登录页
   * @returns {Promise<User>} 用户信息
   * @throws {Error} 网络异常或未登录时抛出错误
   */
  async function fetchUser(): Promise<User> {
    const res = await getCurrentUser()
    userInfo.value = res.data
    return res.data
  }

  /**
   * 设置用户信息
   * @param {User} user - 用户信息对象
   * @returns {void}
   */
  function setUser(user: User): void {
    userInfo.value = user
  }

  /**
   * 更新用户兴趣标签
   * @param {string[]} interests - 兴趣标签数组
   * @returns {void}
   */
  function updateInterests(interests: string[]): void {
    if (userInfo.value) {
      userInfo.value.interests = interests
    }
  }

  /**
   * 退出登录
   * @description 调用后端退出接口，清除本地 Token 与用户信息，
   *   并跳转至登录页
   * @returns {Promise<void>}
   */
  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 即使接口失败也继续清理本地状态
    }
    token.value = null
    userInfo.value = null
    clearToken()
    window.location.href = '/login'
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    setUser,
    updateInterests,
    fetchUser,
    logout
  }
}, {
  persist: {
    key: 'minthive_user',
    pick: ['token']
  }
})
