import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AxiosAdapter } from 'axios'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn(), success: vi.fn() }
}))

import { ElMessage } from 'element-plus'
import service, { request } from '@/api/request'
import { setToken, clearToken } from '@/utils/token'

/**
 * 用自定义 adapter 替换真实网络请求，只验证拦截器的解包与错误分支
 *
 * @param payload 后端返回的业务响应体
 * @param capture 可选，用于读取最终发出的请求配置
 */
function useAdapter(payload: unknown, capture?: (config: unknown) => void): void {
  const adapter: AxiosAdapter = async (config) => {
    capture?.(config)
    return { data: payload, status: 200, statusText: 'OK', headers: {}, config }
  }
  service.defaults.adapter = adapter
}

describe('用户端请求封装', () => {
  beforeEach(() => {
    clearToken()
    localStorage.clear()
  })

  it('返回业务响应并把 records 分页归一化为 list', async () => {
    useAdapter({ code: 200, msg: 'ok', data: { records: [{ id: 1 }], total: 1, current: 1, size: 10 } })

    const res = await request<{ list: { id: number }[]; total: number; page: number; pageSize: number; hasMore: boolean }>({
      url: '/post/page'
    })

    expect(res.code).toBe(200)
    expect(res.data.list).toEqual([{ id: 1 }])
    expect(res.data.page).toBe(1)
    expect(res.data.hasMore).toBe(false)
  })

  it('注入本地 Token 到 Authorization 头', async () => {
    setToken('jwt-token')
    let authorization: unknown
    useAdapter({ code: 200, msg: 'ok', data: null }, (config) => {
      authorization = (config as { headers: Record<string, unknown> }).headers.Authorization
    })

    await request({ url: '/user/me' })

    expect(authorization).toBe('Bearer jwt-token')
  })

  it('业务码非 200 时抛出错误并提示错误消息', async () => {
    useAdapter({ code: 500, msg: '服务异常', data: null })

    await expect(request({ url: '/post/1' })).rejects.toThrow('服务异常')
    expect(ElMessage.error).toHaveBeenCalledWith('服务异常')
  })
})
