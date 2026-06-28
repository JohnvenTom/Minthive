<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { saveAs } from 'file-saver'
import * as XLSX from 'xlsx'
import { useAdminStore } from '@/stores/admin'
import { useStatsStore } from '@/stores/stats'
import type { TimeRange, PeakHour, AuditFunnel } from '@/api/stats'
import StatCard from '@/components/StatCard.vue'
import ChartCard from '@/components/ChartCard.vue'
import AiReportCard from '@/components/AiReportCard.vue'

const adminStore = useAdminStore()
const statsStore = useStatsStore()
const timeRange = ref<TimeRange>('DAY')

const rangeOptions = [
  { label: '日', value: 'DAY' as TimeRange },
  { label: '周', value: 'WEEK' as TimeRange },
  { label: '月', value: 'MONTH' as TimeRange }
]

const statCards = computed(() => {
  const m = statsStore.coreMetrics
  return [
    { title: '累计用户', value: m?.totalUsers ?? 0, suffix: '人', icon: 'User', accent: 'primary' as const, trend: m?.totalUsersTrend ?? 0 },
    { title: '今日新增', value: m?.todayRegister ?? 0, suffix: '人', icon: 'UserFilled', accent: 'primary' as const, trend: m?.todayRegisterTrend ?? 0 },
    { title: '日活 DAU', value: m?.dau ?? 0, suffix: '人', icon: 'TrendCharts', accent: 'primary' as const, trend: m?.dauTrend ?? 0 },
    { title: '月活 MAU', value: m?.mau ?? 0, suffix: '人', icon: 'Histogram', accent: 'warning' as const, trend: m?.mauTrend ?? 0 },
    { title: '今日发帖', value: m?.todayPosts ?? 0, suffix: '条', icon: 'EditPen', accent: 'primary' as const, trend: m?.todayPostsTrend ?? 0 },
    { title: '待处理工单', value: m?.pendingReports ?? 0, suffix: '单', icon: 'Warning', accent: 'danger' as const, trend: m?.pendingReportsTrend ?? 0 }
  ]
})

const registerChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: statsStore.registerTrend.map((p) => p.date) },
  yAxis: { type: 'value' },
  series: [{
    name: '注册用户', type: 'line', smooth: true,
    data: statsStore.registerTrend.map((p) => p.value),
    lineStyle: { color: '#E879A9', width: 2 },
    itemStyle: { color: '#E879A9' },
    areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: 'rgba(232, 121, 169, 0.4)' },
      { offset: 1, color: 'rgba(232, 121, 169, 0)' }
    ]) }
  }]
}))

const activeChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['DAU', 'MAU'] },
  xAxis: { type: 'category', data: statsStore.activeTrend?.dates || [] },
  yAxis: { type: 'value' },
  series: [
    { name: 'DAU', type: 'bar', data: statsStore.activeTrend?.series[0]?.data || [],
      itemStyle: { color: '#E879A9', borderRadius: [4, 4, 0, 0] } },
    { name: 'MAU', type: 'bar', data: statsStore.activeTrend?.series[1]?.data || [],
      itemStyle: { color: '#F59E0B', borderRadius: [4, 4, 0, 0] } }
  ]
}))

const postChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: statsStore.postTrend.map((p) => p.date) },
  yAxis: { type: 'value' },
  series: [{
    name: '发帖量', type: 'line', smooth: true,
    data: statsStore.postTrend.map((p) => p.value),
    lineStyle: { color: '#F59E0B', width: 2 },
    itemStyle: { color: '#F59E0B' },
    areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: 'rgba(255, 182, 39, 0.4)' },
      { offset: 1, color: 'rgba(255, 182, 39, 0)' }
    ]) }
  }]
}))

const interactionChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: statsStore.interactionTrend?.series.map((s) => s.name) || [] },
  xAxis: { type: 'category', data: statsStore.interactionTrend?.dates || [] },
  yAxis: { type: 'value' },
  series: (statsStore.interactionTrend?.series || []).map((s, i) => ({
    name: s.name, type: 'line', smooth: true, data: s.data,
    lineStyle: { width: 2 },
    itemStyle: { color: i === 0 ? '#E879A9' : '#F59E0B' }
  }))
}))

const peakHourTypes = ['发帖', '评论', '点赞']

const heatmapOption = computed<echarts.EChartsOption>(() => {
  const data = statsStore.peakHours
  const maxVal = Math.max(1, ...data.map(p => Math.max(p.postCount, p.commentCount, p.likeCount)))
  const heatData: [number, number, number][] = []
  data.forEach((p, h) => {
    heatData.push([0, h, p.postCount])
    heatData.push([1, h, p.commentCount])
    heatData.push([2, h, p.likeCount])
  })
  return {
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        const d = params.data as [number, number, number]
        return `${peakHourTypes[d[0]]} ${data[d[1]].hour}<br/>活跃量: ${d[2]}`
      }
    },
    grid: { left: 56, right: 20, top: 40, bottom: 36 },
    xAxis: { type: 'category', data: peakHourTypes, splitArea: { show: true } },
    yAxis: { type: 'category', data: data.map(p => p.hour), splitArea: { show: true }, inverse: true },
    visualMap: {
      min: 0, max: maxVal,
      calculable: true,
      orient: 'horizontal', left: 'center', top: 0, inRange: { color: ['rgba(232,121,169,0.05)', '#E879A9'] },
      textStyle: { fontSize: 10 }
    },
    series: [{
      type: 'heatmap', data: heatData,
      label: { show: false },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.3)' }
      }
    }]
  }
})

const funnelOption = computed<echarts.EChartsOption>(() => {
  const s = statsStore.auditFunnel?.snapshot
  if (!s) return {}
  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const pct = params.percent || 0
        return `${params.name}<br/>${params.value} 篇 (${pct}%)`
      }
    },
    series: [{
      type: 'funnel', left: '25%', top: 20, bottom: 20, width: '50%',
      minSize: '10%', maxSize: '100%',
      sort: 'descending', gap: 4,
      label: { show: true, position: 'left', formatter: '{b}', fontSize: 11, color: '#A1A1AA' },
      labelLine: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      emphasis: { label: { fontSize: 12, fontWeight: 'bold' } },
      data: [
        { name: '总发帖', value: s.totalPosts, itemStyle: { color: '#E879A9' } },
        { name: '待审核', value: s.pendingCount, itemStyle: { color: '#F59E0B' } },
        { name: '已通过', value: s.approvedCount, itemStyle: { color: '#34D399' } },
        { name: '已驳回', value: s.rejectedCount, itemStyle: { color: '#EF4444' } }
      ]
    }]
  }
})

const isLight = computed(() => adminStore.theme === 'light')
const labelLineColor = computed(() => isLight.value ? '#D4D4D7' : '#3A4270')
const circleRankChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { type: 'scroll', orient: 'vertical', right: 5, top: 'center', fontSize: 11 },
  series: [{
    name: '圈子活跃度', type: 'pie',
    radius: ['38%', '68%'], center: ['40%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: { borderWidth: 2 },
    label: { show: true, position: 'outside', fontSize: 12, formatter: '{b}\n{d}%' },
    labelLine: { length: 12, length2: 8, lineStyle: { color: labelLineColor.value } },
    emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
    data: statsStore.circleRank.slice(0, 8).map((c) => ({
      name: c.name, value: c.interactionCount
    }))
  }]
}))

const reportChartOption = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: statsStore.reportStats?.series.map((s) => s.name) || [] },
  xAxis: { type: 'category', data: statsStore.reportStats?.dates || [] },
  yAxis: { type: 'value' },
  series: (statsStore.reportStats?.series || []).map((s, i) => ({
    name: s.name, type: 'bar', stack: 'total', data: s.data,
    itemStyle: { color: i === 0 ? '#EF4444' : i === 1 ? '#F59E0B' : '#E879A9', borderRadius: i === 2 ? [4, 4, 0, 0] : 0 }
  }))
}))

async function loadData() {
  try {
    await statsStore.loadData(timeRange.value)
  } catch (e) {}
}

async function loadAll() {
  try {
    await statsStore.loadData(timeRange.value)
    await statsStore.loadAiReport()
  } catch (e) {}
}

async function handleRangeChange(range: string | number | boolean | undefined) {
  timeRange.value = range as TimeRange
  await loadData()
}

function handleExportExcel() {
  const wb = XLSX.utils.book_new()
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
  const regData = [['日期', '注册数'], ...statsStore.registerTrend.map((p) => [p.date, p.value])]
  XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet(regData), '注册趋势')
  const postData = [['日期', '发帖量'], ...statsStore.postTrend.map((p) => [p.date, p.value])]
  XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet(postData), '发帖量趋势')
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
    <div class="screen-hex-deco hex-1"></div>
    <div class="screen-hex-deco hex-2"></div>
    <div class="screen-hex-deco hex-3"></div>

    <div class="screen-content">
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
          <el-button type="primary" :icon="'Download'" @click="handleExportExcel">导出 Excel</el-button>
          <el-button :icon="'Refresh'" @click="loadAll" :loading="statsStore.loading">刷新</el-button>
        </div>
      </header>

      <section class="stat-grid" v-loading="statsStore.loading">
        <div v-for="(card, i) in statCards" :key="card.title"
             class="stat-grid-item fade-in-up" :style="{ animationDelay: `${i * 0.08}s` }">
          <StatCard :title="card.title" :value="card.value" :suffix="card.suffix"
                    :icon="card.icon" :accent="card.accent" :trend="card.trend" />
        </div>
      </section>

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
        <div class="chart-grid-item chart-grid-item--wide fade-in-up" style="animation-delay: 0.7s">
          <ChartCard title="圈子活跃度分布" :option="circleRankChartOption" :loading="statsStore.loading" :height="260" />
        </div>
        <div class="chart-grid-item fade-in-up" style="animation-delay: 0.8s">
          <ChartCard title="举报工单统计" :option="reportChartOption" :loading="statsStore.loading" :height="260" />
        </div>
        <div class="chart-grid-item fade-in-up" style="animation-delay: 0.85s">
          <ChartCard title="活跃高峰时段" :option="heatmapOption" :loading="statsStore.loading" :height="300" />
        </div>
        <div class="chart-grid-item fade-in-up" style="animation-delay: 0.9s">
          <ChartCard title="帖子审核漏斗" :option="funnelOption" :loading="statsStore.loading" :height="300" />
        </div>
      </section>

      <section class="ai-section fade-in-up" style="animation-delay: 0.9s">
        <AiReportCard :report="statsStore.aiReport" :loading="statsStore.aiLoading" />
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

.screen-hex-deco {
  position: absolute;
  pointer-events: none;
  z-index: 0;
  &::before {
    content: '';
    display: block;
    width: 100%; height: 100%;
    background: radial-gradient(circle, rgba(232, 121, 169, 0.08), transparent 70%);
  }
}
.hex-1 {
  width: 300px; height: 300px;
  top: 40px; right: -80px;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: rgba(232, 121, 169, 0.04);
}
.hex-2 {
  width: 200px; height: 200px;
  bottom: 100px; left: -60px;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: rgba(245, 158, 11, 0.04);
}
.hex-3 {
  width: 150px; height: 150px;
  top: 50%; left: 30%;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: rgba(232, 121, 169, 0.03);
}

.screen-content {
  position: relative;
  z-index: 1;
  padding: 24px 28px;
}

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
  background: rgba(232, 121, 169, 0.15);
  border: 1px solid rgba(232, 121, 169, 0.4);
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

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-grid-item {
  min-width: 0;
}

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
.chart-grid-item--wide {
  grid-column: span 2;
}

.ai-section {
  margin-top: 4px;
}

@media (max-width: 1600px) {
  .stat-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .chart-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
