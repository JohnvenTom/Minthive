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
  getPeakHours,
  getAuditFunnel,
  getAiDailyReport,
  type CoreMetrics,
  type TrendPoint,
  type MultiTrend,
  type CircleActive,
  type PeakHour,
  type AuditFunnel,
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
  const peakHours = ref<PeakHour[]>([])
  const auditFunnel = ref<AuditFunnel | null>(null)
  const aiReport = ref<AiDailyReport | null>(null)
  const loading = ref(false)
  /** AI 日报独立加载状态，避免阻塞其他数据渲染 */
  const aiLoading = ref(false)

  /**
   * 加载核心指标 + 图表数据（快，通常 <500ms）
   * @param range 时间维度
   */
  async function loadData(range: TimeRange = 'DAY') {
    loading.value = true
    try {
      const [core, reg, active, post, inter, circle, report, peak, funnel] = await Promise.all([
        getCoreMetrics(),
        getRegisterTrend(range),
        getActiveTrend(range),
        getPostTrend(range),
        getInteractionTrend(range),
        getCircleActiveRank(),
        getReportStats(range),
        getPeakHours(range),
        getAuditFunnel(range)
      ])
      coreMetrics.value = core
      registerTrend.value = reg
      activeTrend.value = active
      postTrend.value = post
      interactionTrend.value = inter
      circleRank.value = circle
      reportStats.value = report
      peakHours.value = peak
      auditFunnel.value = funnel
    } finally {
      loading.value = false
    }
  }

  /** 加载 AI 日报（慢，调用大模型，独立加载不阻塞页面） */
  async function loadAiReport() {
    aiLoading.value = true
    try {
      aiReport.value = await getAiDailyReport()
    } catch (e) {
      // AI 接口失败不影响主界面，静默处理（错误已由拦截器弹窗提示）
    } finally {
      aiLoading.value = false
    }
  }

  /**
   * 加载全部大屏数据（核心指标+图表+AI 日报）
   * @param range 时间维度
   */
  async function loadAll(range: TimeRange = 'DAY') {
    await loadData(range)
    await loadAiReport()
  }

  return {
    coreMetrics,
    registerTrend,
    activeTrend,
    postTrend,
    interactionTrend,
    circleRank,
    reportStats,
    peakHours,
    auditFunnel,
    aiReport,
    loading,
    aiLoading,
    loadData,
    loadAiReport,
    loadAll
  }
})
