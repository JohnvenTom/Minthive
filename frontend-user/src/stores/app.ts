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
  const theme = ref<'light' | 'dark'>('light')

  /** 是否为移动端视图 */
  const isMobile = ref<boolean>(window.innerWidth <= 768)

  /** AI 功能开关（由后端配置控制） */
  const aiEnabled = ref<boolean>(true)

  /** AI 客服弹窗是否显示 */
  const showAiAssistant = ref<boolean>(false)

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
   * @returns {void}
   */
  function setAiEnabled(enabled: boolean): void {
    aiEnabled.value = enabled
  }

  return {
    theme,
    isMobile,
    aiEnabled,
    showAiAssistant,
    toggleAiAssistant,
    setTheme,
    setAiEnabled
  }
}, {
  persist: {
    key: 'minthive_app',
    pick: ['theme']
  }
})
