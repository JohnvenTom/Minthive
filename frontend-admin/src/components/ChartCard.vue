<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * ChartCard 图表卡片组件
 * 功能：封装 ECharts 实例，自适应容器尺寸，支持暗色主题
 * Props:
 *   - title: 卡片标题
 *   - option: ECharts 配置项
 *   - height: 图表高度，默认 280
 *   - loading: 加载状态
 */
const props = withDefaults(defineProps<{
  title?: string
  option: echarts.EChartsOption
  height?: number
  loading?: boolean
}>(), {
  title: '',
  height: 280,
  loading: false
})

const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

/** 初始化图表 */
function initChart() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value, 'dark')
  chartInstance.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: '#B5BDD4', fontFamily: 'JetBrains Mono, monospace' },
    grid: { top: 40, right: 24, bottom: 32, left: 48 },
    ...props.option
  })
}

/** 监听配置变化更新图表 */
watch(
  () => props.option,
  (newOpt) => {
    if (chartInstance) {
      chartInstance.setOption(newOpt, true)
    }
  },
  { deep: true }
)

watch(
  () => props.loading,
  (val) => {
    if (chartInstance) {
      val ? chartInstance.showLoading('default', { color: '#E879A9', textColor: '#D4D4D8' }) : chartInstance.hideLoading()
    }
  }
)

/** 窗口尺寸变化自适应 */
function handleResize() {
  chartInstance?.resize()
}

onMounted(async () => {
  await nextTick()
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<template>
  <div class="chart-card base-card">
    <div v-if="title" class="chart-header">
      <span class="chart-title">{{ title }}</span>
      <span class="chart-deco"></span>
    </div>
    <div ref="chartRef" class="chart-body" :style="{ height: `${height}px` }"></div>
  </div>
</template>

<style lang="scss" scoped>
.chart-card {
  height: 100%;
  overflow: visible;
}
.chart-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid $border-divider;
}
.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}
.chart-deco {
  width: 6px;
  height: 6px;
  background: $color-primary;
  transform: rotate(45deg);
  box-shadow: $shadow-glow-primary;
}
.chart-body {
  width: 100%;
  overflow: visible;
}
</style>
