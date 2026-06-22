<script setup lang="ts">
import { computed } from 'vue'
import type { RiskLevel } from '@/api/types'

/**
 * RiskLevelTag AI 风险等级标签组件
 * 功能：渲染低/中/高三级风险标签，带发光效果
 * Props:
 *   - level: 风险等级 LOW / MEDIUM / HIGH
 */
const props = defineProps<{
  level: RiskLevel
}>()

const config = computed(() => {
  switch (props.level) {
    case 'LOW':
      return { label: '低风险', cls: 'risk-low' }
    case 'MEDIUM':
      return { label: '中风险', cls: 'risk-medium' }
    case 'HIGH':
      return { label: '高风险', cls: 'risk-high' }
    default:
      return { label: '未知', cls: 'risk-unknown' }
  }
})
</script>

<template>
  <span class="risk-tag" :class="config.cls">
    <span class="risk-dot"></span>
    {{ config.label }}
  </span>
</template>

<style lang="scss" scoped>
.risk-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  font-family: $font-mono;
  border: 1px solid;
}
.risk-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 6px currentColor;
  animation: pulse 1.6s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.risk-low {
  color: $color-primary;
  background: rgba(232, 121, 169, 0.12);
  border-color: rgba(232, 121, 169, 0.4);
  box-shadow: $shadow-glow-primary;
}
.risk-medium {
  color: $color-warning;
  background: rgba(255, 182, 39, 0.12);
  border-color: rgba(255, 182, 39, 0.4);
  box-shadow: $shadow-glow-warning;
}
.risk-high {
  color: $color-danger;
  background: rgba(255, 92, 108, 0.12);
  border-color: rgba(255, 92, 108, 0.4);
  box-shadow: $shadow-glow-danger;
}
.risk-unknown {
  color: $text-secondary;
  background: rgba(122, 134, 184, 0.12);
  border-color: rgba(122, 134, 184, 0.3);
}
</style>
