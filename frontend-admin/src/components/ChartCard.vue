<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { registerMintHiveTheme, registerMintHiveLightTheme } from '@/styles/echarts-theme'
import { useAdminStore } from '@/stores/admin'

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

const adminStore = useAdminStore()
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

function initChart() {
  if (!chartRef.value) return
  registerMintHiveTheme()
  registerMintHiveLightTheme()
  const themeName = adminStore.theme === 'light' ? 'minthive-light' : 'minthive'
  chartInstance?.dispose()
  chartInstance = echarts.init(chartRef.value, themeName)
  chartInstance.setOption({
    grid: { top: 40, right: 24, bottom: 32, left: 48 },
    textStyle: { fontFamily: 'JetBrains Mono, monospace' },
    ...props.option
  })
}

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

watch(
  () => adminStore.theme,
  () => {
    initChart()
  }
)

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
  background: $gradient-primary;
  transform: rotate(45deg);
  box-shadow: 0 0 8px rgba(232, 121, 169, 0.5);
  flex-shrink: 0;
}
.chart-body {
  width: 100%;
  overflow: visible;
}
</style>
