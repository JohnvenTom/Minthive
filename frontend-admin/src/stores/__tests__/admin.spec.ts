import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/auth', () => ({
  getAdminInfo: vi.fn(),
  logout: vi.fn()
}))

import { useAdminStore } from '@/stores/admin'
import { getAdminInfo, logout as logoutApi } from '@/api/auth'

/** 主题存储键，与 store 内保持一致 */
const THEME_KEY = 'minthive_admin_theme'

describe('admin store', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    setActivePinia(createPinia())
  })

  it('从 localStorage 恢复 token 并写入 Authorization 所需的存储', () => {
    localStorage.setItem('admin_token', 'restored-token')
    const store = useAdminStore()

    expect(store.token).toBe('restored-token')

    store.setAdminToken('new-token')
    expect(store.token).toBe('new-token')
    expect(localStorage.getItem('admin_token')).toBe('new-token')
  })

  it('切换主题时同步 data-theme 与本地存储', () => {
    const store = useAdminStore()
    expect(store.theme).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')

    store.setTheme('light')
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    expect(localStorage.getItem(THEME_KEY)).toBe('light')

    store.setTheme()
    expect(store.theme).toBe('dark')
  })

  it('登出即使接口失败也会清空本地登录态', async () => {
    vi.mocked(logoutApi).mockRejectedValue(new Error('network'))
    localStorage.setItem('admin_token', 'token')
    const store = useAdminStore()
    store.adminInfo = { adminId: 1, account: 'admin', nickname: 'A', role: 'ADMIN', permissions: [] }

    await store.logout()

    expect(store.token).toBe('')
    expect(store.adminInfo).toBeNull()
    expect(localStorage.getItem('admin_token')).toBeNull()
  })

  it('没有 token 时不请求管理员信息', async () => {
    const store = useAdminStore()

    await expect(store.fetchAdminInfo()).resolves.toBeUndefined()
    expect(getAdminInfo).not.toHaveBeenCalled()
  })

  it('拉取管理员信息后写入状态', async () => {
    localStorage.setItem('admin_token', 'token')
    const info = { adminId: 2, account: 'root', nickname: 'R', role: 'ADMIN', permissions: ['audit'] }
    vi.mocked(getAdminInfo).mockResolvedValue(info)
    const store = useAdminStore()

    await expect(store.fetchAdminInfo()).resolves.toEqual(info)
    expect(store.adminInfo).toEqual(info)
  })
})
