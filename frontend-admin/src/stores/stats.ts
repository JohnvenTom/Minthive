import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { TimeRange } from '@/api/stats'
import {
  getCoreMetrics,
  getRegisterTrend,
  getActiveTrend,
  getPostTrend,
  getInteractionTrend,
  getCircleActiveRank,
  getReportStats,
  getAiDailyReport,
  type CoreMetrics,
  type TrendPoint,
  type MultiTrend,
  type CircleActive,
  type AiDailyReport
} from '@/api/stats'

/**
 * 数据统计 Store
 * 功能：集中管理数据大屏所需的各项统计数据
 * 参数：无
 * 返回值：Pinia store 实例
 * 注意事项：所有数据按时间维度刷新，loading 状态由调用方控制
 */
export const useStatsStore = defineStore('stats', () => {
  const coreMetrics = ref<CoreMetrics | null>(null)
  const registerTrend = ref<TrendPoint[]>([])
  const activeTrend = ref<MultiTrend | null>(null)
  const postTrend = ref<TrendPoint[]>([])
  const interactionTrend = ref<MultiTrend | null>(null)
  const circleRank = ref<CircleActive[]>([])
  const reportStats = ref<MultiTrend | null>(null)
  const aiReport = ref<AiDailyReport | null>(null)
  const loading = ref(false)

  /**
   * 加载全部大屏数据
   * @param range 时间维度
   */
  async function loadAll(range: TimeRange = 'DAY') {
    loading.value = true
    try {
      const [core, reg, active, post, inter, circle, report, ai] = await Promise.all([
        getCoreMetrics(),
        getRegisterTrend(range),
        getActiveTrend(range),
        getPostTrend(range),
        getInteractionTrend(range),
        getCircleActiveRank(),
        getReportStats(range),
        getAiDailyReport()
      ])
      coreMetrics.value = core.data
      registerTrend.value = reg.data
      activeTrend.value = active.data
      postTrend.value = post.data
      interactionTrend.value = inter.data
      circleRank.value = circle.data
      reportStats.value = report.data
      aiReport.value = ai.data
    } finally {
      loading.value = false
    }
  }

  return {
    coreMetrics,
    registerTrend,
    activeTrend,
    postTrend,
    interactionTrend,
    circleRank,
    reportStats,
    aiReport,
    loading,
    loadAll
  }
})
