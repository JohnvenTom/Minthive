import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'

/**
 * 用户端单元测试配置
 *
 * 功能：在 jsdom 环境下运行 src 下的 *.spec.ts，复用 Vite 的 @ 别名
 * 注意：仅覆盖契约归一化与关键 store，不做端到端 UI 测试
 */
export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.spec.ts'],
    clearMocks: true,
    restoreMocks: true
  }
})
