/**
 * 应用全局状态
 * @module stores/app
 * @description 管理应用级全局状态：主题、设备类型、AI 功能开关、AI 客服弹窗
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // ---------- 状态 ----------

  /** 当前主题：light / dark */
  const theme = ref<'light' | 'dark'>('dark')

  /** 是否为移动端视图 */
  const isMobile = ref<boolean>(window.innerWidth <= 768)

  /** AI 功能开关（由后端配置控制） */
  const aiEnabled = ref<boolean>(true)

  /** AI 客服弹窗是否显示 */
  const showAiAssistant = ref<boolean>(false)

  /** AI 助手对话历史（内存级，页面刷新自动清空） */
  const aiChatHistory = ref<Array<{ role: 'user' | 'ai'; content: string }>>([
    { role: 'ai', content: '你好！我是 MintHive AI 助手，有什么可以帮你的吗？' }
  ])

  // ---------- Actions ----------

  /**
   * 切换 AI 客服弹窗显示状态
   * @returns {void}
   */
  function toggleAiAssistant(): void {
    showAiAssistant.value = !showAiAssistant.value
  }

  /**
   * 切换应用主题
   * @param {'light' | 'dark'} newTheme - 目标主题，不传则切换
   * @returns {void}
   */
  function setTheme(newTheme?: 'light' | 'dark'): void {
    theme.value = newTheme || (theme.value === 'light' ? 'dark' : 'light')
    document.documentElement.setAttribute('data-theme', theme.value)
  }

  /**
   * 设置 AI 功能开关
   * @param {boolean} enabled - 是否启用 AI 功能
   */
  function setAiEnabled(enabled: boolean): void {
    aiEnabled.value = enabled
  }

  /**
   * 重置 AI 助手对话历史（页面刷新或切换账号时调用）
   * @returns {void}
   */
  function resetAiChatHistory(): void {
    aiChatHistory.value = [
      { role: 'ai', content: '你好！我是 MintHive AI 助手，有什么可以帮你的吗？' }
    ]
  }

  return {
    theme,
    isMobile,
    aiEnabled,
    showAiAssistant,
    aiChatHistory,
    toggleAiAssistant,
    setTheme,
    setAiEnabled,
    resetAiChatHistory
  }
}, {
  persist: {
    key: 'minthive_app',
    pick: ['theme']
  }
})
