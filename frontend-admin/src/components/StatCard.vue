<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'

/**
 * StatCard 统计卡片组件
 * 功能：展示单个核心指标，支持数字滚动动画、图标、趋势
 * Props:
 *   - title: 指标标题
 *   - value: 当前数值
 *   - suffix: 单位后缀（如 "人"、"条"）
 *   - icon: Element Plus 图标组件名
 *   - trend: 趋势百分比（正数上升绿色，负数下降红色）
 *   - accent: 强调色类型 primary / warning / danger
 *   - duration: 滚动动画时长（ms），默认 1200
 */
const props = withDefaults(defineProps<{
  title: string
  value: number
  suffix?: string
  icon?: string
  trend?: number
  accent?: 'primary' | 'warning' | 'danger'
  duration?: number
}>(), {
  suffix: '',
  icon: 'DataLine',
  accent: 'primary',
  duration: 1200
})

const displayValue = ref(0)

/**
 * 数字滚动动画
 * @param from 起始值
 * @param to 目标值
 * @param dur 动画时长
 */
function animate(from: number, to: number, dur: number) {
  const start = performance.now()
  const step = (now: number) => {
    const progress = Math.min((now - start) / dur, 1)
    // easeOutCubic
    const eased = 1 - Math.pow(1 - progress, 3)
    displayValue.value = Math.round(from + (to - from) * eased)
    if (progress < 1) {
      requestAnimationFrame(step)
    }
  }
  requestAnimationFrame(step)
}

watch(
  () => props.value,
  (newVal, oldVal) => {
    animate(oldVal || 0, newVal, props.duration)
  }
)

onMounted(() => {
  animate(0, props.value, props.duration)
})

/** 格式化数字（千分位） */
function formatNumber(num: number): string {
  return num.toLocaleString('zh-CN')
}
</script>

<template>
  <div class="stat-card" :class="`accent-${accent}`">
    <div class="stat-hex-deco" aria-hidden="true">
      <svg viewBox="0 0 100 100" width="80" height="80">
        <polygon points="50,5 90,28 90,72 50,95 10,72 10,28" fill="none" stroke="currentColor" stroke-width="1" />
      </svg>
    </div>
    <div class="stat-header">
      <span class="stat-title">{{ title }}</span>
      <el-icon class="stat-icon"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-value mono">
      {{ formatNumber(displayValue) }}<span class="stat-suffix">{{ suffix }}</span>
    </div>
    <div v-if="trend !== undefined" class="stat-trend" :class="trend >= 0 ? 'up' : 'down'">
      <el-icon><component :is="trend >= 0 ? 'CaretTop' : 'CaretBottom'" /></el-icon>
      <span>{{ Math.abs(trend) }}%</span>
      <span class="trend-label">较昨日</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.stat-card {
  position: relative;
  background: $bg-elevated;
  border: 1px solid $border-base;
  border-radius: $radius-lg;
  padding: 20px 22px;
  overflow: hidden;
  transition: $transition-base;
  min-height: 120px;

  &:hover {
    transform: translateY(-3px);
    border-color: $color-primary;
  }

  &.accent-primary {
    .stat-icon { color: $color-primary; }
    .stat-value { color: $color-primary; text-shadow: $shadow-glow-primary; }
    .stat-hex-deco { color: rgba(78, 205, 196, 0.12); }
  }
  &.accent-warning {
    .stat-icon { color: $color-warning; }
    .stat-value { color: $color-warning; text-shadow: $shadow-glow-warning; }
    .stat-hex-deco { color: rgba(255, 182, 39, 0.12); }
  }
  &.accent-danger {
    .stat-icon { color: $color-danger; }
    .stat-value { color: $color-danger; text-shadow: $shadow-glow-danger; }
    .stat-hex-deco { color: rgba(255, 92, 108, 0.12); }
  }
}
.stat-hex-deco {
  position: absolute;
  right: -10px;
  top: -10px;
  opacity: 0.6;
  pointer-events: none;
}
.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.stat-title {
  font-size: 13px;
  color: $text-secondary;
  letter-spacing: 0.5px;
}
.stat-icon {
  font-size: 20px;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
  .stat-suffix {
    font-size: 14px;
    margin-left: 4px;
    opacity: 0.8;
    font-weight: 400;
  }
}
.stat-trend {
  margin-top: 8px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  &.up { color: $color-primary; }
  &.down { color: $color-danger; }
  .trend-label {
    color: $text-secondary;
    margin-left: 4px;
  }
}
</style>
