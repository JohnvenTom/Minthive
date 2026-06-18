<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { saveAs } from 'file-saver'
import * as XLSX from 'xlsx'
import { useStatsStore } from '@/stores/stats'
import type { TimeRange } from '@/api/stats'
import StatCard from '@/components/StatCard.vue'
import ChartCard from '@/components/ChartCard.vue'
import AiReportCard from '@/components/AiReportCard.vue'

/**
 * DataScreenPage 数据大屏页
 * 功能：展示社区健康度监控中心，含6个核心指标卡片、2x3图表网格、AI日报、Excel导出
 * 参数：无
 * 注意事项：支持日/周/月维度切换，数字滚动动画，蜂巢装饰背景
 */
const statsStore = useStatsStore()
const timeRange = ref<TimeRange>('DAY')

/** 时间维度选项 */
const rangeOptions = [
  { label: '日', value: 'DAY' as TimeRange },
  { label: '周', value: 'WEEK' as TimeRange },
  { label: '月', value: 'MONTH' as TimeRange }
]

/** 核心指标卡片配置 */
const statCards = computed(() => {
  const m = statsStore.coreMetrics
  return [
    { title: '累计用户', value: m?.totalUsers ?? 0, suffix: '人', icon: 'User', accent: 'primary' as const, trend: 0 },
    { title: '今日新增', value: m?.todayRegister ?? 0, suffix: '人', icon: 'UserFilled', accent: 'primary' as const, trend: 0 },
    { title: '日活 DAU', value: m?.dau ?? 0, suffix: '人', icon: 'TrendCharts', accent: 'primary' as const, trend: 0 },
    { title: '月活 MAU', value: m?.mau ?? 0, suffix: '人', icon: 'Histogram', accent: 'warning' as const, trend: 0 },
    { title: '今日发帖', value: m?.todayPosts ?? 0, suffix: '条', icon: 'EditPen', accent: 'primary' as const, trend: 0 },
    { title: '待处理工单', value: m?.pendingReports ?? 0, suffix: '单', icon: 'Warning', accent: 'danger' as const, trend: 0 }
  ]
})

// ============ 图表配置 ============

/** 注册趋势折线图 */
const registerChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: statsStore.registerTrend.map((p) => p.date),
    axisLine: { lineStyle: { color: '#3A4270' } },
    axisLabel: { color: '#7A86B8' }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#2F3658' } },
    axisLabel: { color: '#7A86B8' }
  },
  series: [
    {
      name: '注册用户',
      type: 'line',
      smooth: true,
      data: statsStore.registerTrend.map((p) => p.value),
      lineStyle: { color: '#4ECDC4', width: 2 },
      itemStyle: { color: '#4ECDC4' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(78, 205, 196, 0.4)' },
          { offset: 1, color: 'rgba(78, 205, 196, 0)' }
        ])
      }
    }
  ]
}))

/** 日活月活对比柱状图 */
const activeChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['DAU', 'MAU'], textStyle: { color: '#B5BDD4' } },
  xAxis: {
    type: 'category',
    data: statsStore.activeTrend?.dates || [],
    axisLine: { lineStyle: { color: '#3A4270' } },
    axisLabel: { color: '#7A86B8' }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#2F3658' } },
    axisLabel: { color: '#7A86B8' }
  },
  series: [
    {
      name: 'DAU',
      type: 'bar',
      data: statsStore.activeTrend?.series[0]?.data || [],
      itemStyle: { color: '#4ECDC4', borderRadius: [4, 4, 0, 0] }
    },
    {
      name: 'MAU',
      type: 'bar',
      data: statsStore.activeTrend?.series[1]?.data || [],
      itemStyle: { color: '#FFB627', borderRadius: [4, 4, 0, 0] }
    }
  ]
}))

/** 发帖量趋势折线图 */
const postChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: statsStore.postTrend.map((p) => p.date),
    axisLine: { lineStyle: { color: '#3A4270' } },
    axisLabel: { color: '#7A86B8' }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#2F3658' } },
    axisLabel: { color: '#7A86B8' }
  },
  series: [
    {
      name: '发帖量',
      type: 'line',
      smooth: true,
      data: statsStore.postTrend.map((p) => p.value),
      lineStyle: { color: '#FFB627', width: 2 },
      itemStyle: { color: '#FFB627' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255, 182, 39, 0.4)' },
          { offset: 1, color: 'rgba(255, 182, 39, 0)' }
        ])
      }
    }
  ]
}))

/** 互动量趋势（点赞/评论） */
const interactionChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: statsStore.interactionTrend?.series.map((s) => s.name) || [], textStyle: { color: '#B5BDD4' } },
  xAxis: {
    type: 'category',
    data: statsStore.interactionTrend?.dates || [],
    axisLine: { lineStyle: { color: '#3A4270' } },
    axisLabel: { color: '#7A86B8' }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#2F3658' } },
    axisLabel: { color: '#7A86B8' }
  },
  series: (statsStore.interactionTrend?.series || []).map((s, i) => ({
    name: s.name,
    type: 'line',
    smooth: true,
    data: s.data,
    lineStyle: { width: 2 },
    itemStyle: { color: i === 0 ? '#4ECDC4' : '#FFB627' }
  }))
}))

/** 圈子活跃度分布饼图 */
const circleRankChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: {
    type: 'scroll',
    orient: 'vertical',
    right: 5,
    top: 'center',
    textStyle: { color: '#B5BDD4', fontSize: 11 }
  },
  series: [
    {
      name: '圈子活跃度',
      type: 'pie',
      radius: ['32%', '55%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderColor: '#1A1D2E', borderWidth: 2 },
      label: {
        show: true,
        position: 'outside',
        fontSize: 10,
        color: '#B5BDD4',
        formatter: '{b}\n{d}%'
      },
      labelLine: { length: 8, length2: 6, lineStyle: { color: '#3A4270' } },
      emphasis: {
        label: { show: true, fontSize: 12, fontWeight: 'bold', color: '#E8ECF5' }
      },
      data: statsStore.circleRank.slice(0, 8).map((c) => ({
        name: c.name,
        value: c.interactionCount
      }))
    }
  ]
}))

/** 举报工单统计 */
const reportChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: statsStore.reportStats?.series.map((s) => s.name) || [], textStyle: { color: '#B5BDD4' } },
  xAxis: {
    type: 'category',
    data: statsStore.reportStats?.dates || [],
    axisLine: { lineStyle: { color: '#3A4270' } },
    axisLabel: { color: '#7A86B8' }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#2F3658' } },
    axisLabel: { color: '#7A86B8' }
  },
  series: (statsStore.reportStats?.series || []).map((s, i) => ({
    name: s.name,
    type: 'bar',
    stack: 'total',
    data: s.data,
    itemStyle: { color: i === 0 ? '#FF5C6C' : i === 1 ? '#FFB627' : '#4ECDC4', borderRadius: i === 2 ? [4, 4, 0, 0] : 0 }
  }))
}))

/**
 * 加载数据
 */
async function loadData() {
  try {
    await statsStore.loadAll(timeRange.value)
  } catch (e) {
    // 错误已由拦截器提示
  }
}

/**
 * 切换时间维度
 * @param range 维度
 */
async function handleRangeChange(range: string | number | boolean | undefined) {
  timeRange.value = range as TimeRange
  await loadData()
}

/**
 * 导出 Excel 报表
 * 功能：将当前大屏数据导出为 xlsx 文件
 */
function handleExportExcel() {
  const wb = XLSX.utils.book_new()

  // 核心指标 sheet
  const metricsData = [
    ['指标', '数值'],
    ['累计用户', statsStore.coreMetrics?.totalUsers ?? 0],
    ['今日新增', statsStore.coreMetrics?.todayRegister ?? 0],
    ['日活 DAU', statsStore.coreMetrics?.dau ?? 0],
    ['月活 MAU', statsStore.coreMetrics?.mau ?? 0],
    ['今日发帖', statsStore.coreMetrics?.todayPosts ?? 0],
    ['待处理工单', statsStore.coreMetrics?.pendingReports ?? 0]
  ]
  const wsMetrics = XLSX.utils.aoa_to_sheet(metricsData)
  XLSX.utils.book_append_sheet(wb, wsMetrics, '核心指标')

  // 注册趋势 sheet
  const regData = [['日期', '注册数'], ...statsStore.registerTrend.map((p) => [p.date, p.value])]
  XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet(regData), '注册趋势')

  // 发帖量 sheet
  const postData = [['日期', '发帖量'], ...statsStore.postTrend.map((p) => [p.date, p.value])]
  XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet(postData), '发帖量趋势')

  // 圈子活跃 sheet
  const circleData = [['圈子名称', '成员数', '发帖数', '互动数'], ...statsStore.circleRank.map((c) => [c.name, c.memberCount, c.postCount, c.interactionCount])]
  XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet(circleData), '圈子活跃排行')

  const rangeLabel = rangeOptions.find((r) => r.value === timeRange.value)?.label || ''
  const fileName = `MintHive数据报表_${rangeLabel}维度_${new Date().toISOString().slice(0, 10)}.xlsx`
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
  saveAs(new Blob([wbout], { type: 'application/octet-stream' }), fileName)
  ElMessage.success('报表导出成功')
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="data-screen">
    <!-- 蜂巢装饰背景 -->
    <div class="screen-hex-deco hex-1"></div>
    <div class="screen-hex-deco hex-2"></div>
    <div class="screen-hex-deco hex-3"></div>

    <div class="screen-content">
      <!-- 顶部标题栏 -->
      <header class="screen-header">
        <div class="header-left">
          <div class="header-indicator"></div>
          <h1 class="screen-title">社区健康度监控中心</h1>
          <span class="header-badge mono">LIVE</span>
        </div>
        <div class="header-right">
          <el-radio-group v-model="timeRange" @change="handleRangeChange" size="default">
            <el-radio-button v-for="opt in rangeOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
          <el-button type="primary" :icon="'Download'" @click="handleExportExcel">
            导出 Excel
          </el-button>
          <el-button :icon="'Refresh'" @click="loadData" :loading="statsStore.loading">
            刷新
          </el-button>
        </div>
      </header>

      <!-- 核心指标卡片 -->
      <section class="stat-grid" v-loading="statsStore.loading">
        <div
          v-for="(card, i) in statCards"
          :key="card.title"
          class="stat-grid-item fade-in-up"
          :style="{ animationDelay: `${i * 0.08}s` }"
        >
          <StatCard
            :title="card.title"
            :value="card.value"
            :suffix="card.suffix"
            :icon="card.icon"
            :accent="card.accent"
            :trend="card.trend"
          />
        </div>
      </section>

      <!-- 图表网格 2x3 -->
      <section class="chart-grid">
        <div class="chart-grid-item fade-in-up" style="animation-delay: 0.3s">
          <ChartCard title="注册用户趋势" :option="registerChartOption" :loading="statsStore.loading" :height="260" />
        </div>
        <div class="chart-grid-item fade-in-up" style="animation-delay: 0.4s">
          <ChartCard title="日活 / 月活对比" :option="activeChartOption" :loading="statsStore.loading" :height="260" />
        </div>
        <div class="chart-grid-item fade-in-up" style="animation-delay: 0.5s">
          <ChartCard title="发帖量趋势" :option="postChartOption" :loading="statsStore.loading" :height="260" />
        </div>
        <div class="chart-grid-item fade-in-up" style="animation-delay: 0.6s">
          <ChartCard title="互动量趋势" :option="interactionChartOption" :loading="statsStore.loading" :height="260" />
        </div>
        <div class="chart-grid-item fade-in-up" style="animation-delay: 0.7s">
          <ChartCard title="圈子活跃度分布" :option="circleRankChartOption" :loading="statsStore.loading" :height="260" />
        </div>
        <div class="chart-grid-item fade-in-up" style="animation-delay: 0.8s">
          <ChartCard title="举报工单统计" :option="reportChartOption" :loading="statsStore.loading" :height="260" />
        </div>
      </section>

      <!-- 底部 AI 日报 -->
      <section class="ai-section fade-in-up" style="animation-delay: 0.9s">
        <AiReportCard :report="statsStore.aiReport" />
      </section>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.data-screen {
  position: relative;
  width: 100%;
  height: 100%;
  background: $bg-base;
  overflow-y: auto;
  overflow-x: hidden;
}

// 蜂巢装饰
.screen-hex-deco {
  position: absolute;
  pointer-events: none;
  z-index: 0;
  &::before {
    content: '';
    display: block;
    width: 100%; height: 100%;
    background: radial-gradient(circle, rgba(78, 205, 196, 0.08), transparent 70%);
  }
}
.hex-1 {
  width: 300px; height: 300px;
  top: 40px; right: -80px;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: rgba(78, 205, 196, 0.04);
}
.hex-2 {
  width: 200px; height: 200px;
  bottom: 100px; left: -60px;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: rgba(255, 182, 39, 0.04);
}
.hex-3 {
  width: 150px; height: 150px;
  top: 50%; left: 30%;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: rgba(78, 205, 196, 0.03);
}

.screen-content {
  position: relative;
  z-index: 1;
  padding: 24px 28px;
}

// 顶部标题
.screen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-indicator {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: $color-primary;
  box-shadow: $shadow-glow-primary;
  animation: pulse 1.6s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}
.screen-title {
  font-size: 26px;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: 1px;
}
.header-badge {
  padding: 2px 8px;
  background: rgba(78, 205, 196, 0.15);
  border: 1px solid rgba(78, 205, 196, 0.4);
  border-radius: 4px;
  color: $color-primary;
  font-size: 11px;
  letter-spacing: 2px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

// 指标卡片网格
.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-grid-item {
  min-width: 0;
}

// 图表网格 2x3
.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.chart-grid-item {
  min-width: 0;
  overflow: visible;
}

// AI 日报
.ai-section {
  margin-top: 4px;
}

// 响应式
@media (max-width: 1600px) {
  .stat-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .chart-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
