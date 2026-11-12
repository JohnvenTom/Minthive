import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AxiosAdapter } from 'axios'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn(), success: vi.fn() }
}))

import { ElMessage } from 'element-plus'
import service, { del, get, post, put } from '@/api/request'

/**
 * 用自定义 adapter 替换真实网络请求，只验证拦截器与解包逻辑
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

describe('管理端请求封装', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('GET 与 POST 只返回业务 data，不暴露包裹层', async () => {
    useAdapter({ code: 200, msg: 'ok', data: { total: 2, list: [1, 2] } })

    await expect(get('/audit/page', { page: 1 })).resolves.toEqual({ total: 2, list: [1, 2] })
    await expect(post('/audit/query', { id: 1 })).resolves.toEqual({ total: 2, list: [1, 2] })
  })

  it('PUT 与 DELETE 同样解包 data', async () => {
    useAdapter({ code: 200, msg: 'ok', data: true })

    await expect(put('/user/status', { id: 1 })).resolves.toBe(true)
    await expect(del('/post/1')).resolves.toBe(true)
  })

  it('注入 admin_token 到 Authorization 头', async () => {
    localStorage.setItem('admin_token', 'admin-jwt')
    let authorization: unknown
    useAdapter({ code: 200, msg: 'ok', data: null }, (config) => {
      authorization = (config as { headers: Record<string, unknown> }).headers.Authorization
    })

    await get('/stats/overview')

    expect(authorization).toBe('Bearer admin-jwt')
  })

  it('业务码为 500 时抛出错误并提示后端消息', async () => {
    useAdapter({ code: 500, msg: '后端异常', data: null })

    await expect(get('/stats/overview')).rejects.toThrow('后端异常')
    expect(ElMessage.error).toHaveBeenCalledWith('后端异常')
  })
})
