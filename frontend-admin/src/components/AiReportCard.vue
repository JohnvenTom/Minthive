<script setup lang="ts">
import { computed } from 'vue'
import type { AiDailyReport } from '@/api/stats'

/**
 * AiReportCard AI 日报卡片组件
 * 功能：展示 AI 生成的社区健康度日报，含健康度评分、违规类型、建议
 * Props:
 *   - report: AI 日报数据
 *   - loading: AI 数据加载中（独立于主页面 loading，AI 调用较慢时显示加载态）
 */
const props = defineProps<{
  report: AiDailyReport | null
  loading?: boolean
}>()

/** 健康度评分颜色 — 暮光玫瑰色系 */
const scoreColor = computed(() => {
  const score = props.report?.healthScore ?? 0
  if (score >= 80) return '#E879A9'
  if (score >= 60) return '#F59E0B'
  return '#EF4444'
})

/** 健康度环形进度描述 */
const scoreLabel = computed(() => {
  const score = props.report?.healthScore ?? 0
  if (score >= 80) return '健康'
  if (score >= 60) return '关注'
  return '预警'
})

/** 根据 AI 模式返回不同的底部提示文案 */
const aiSourceText = computed(() => {
  const mode = props.report?.aiMode
  if (mode === 'cloud') {
    return '由云端 AI 大模型自动生成，仅供运营参考'
  }
  return '由本地私有化 AI 大模型自动生成，仅供运营参考'
})
</script>

<template>
  <div class="ai-report-card" v-loading="loading">
    <div class="ai-header">
      <div class="ai-badge">
        <el-icon><MagicStick /></el-icon>
        <span>AI 日报</span>
      </div>
      <span class="ai-date mono">{{ report?.reportDate || '--' }}</span>
    </div>

    <div class="ai-score-section">
      <div class="score-ring" :style="{ '--score-color': scoreColor }">
        <svg viewBox="0 0 120 120">
          <circle cx="60" cy="60" r="52" fill="none" stroke="#3f3f46" stroke-width="8" />
          <circle
            cx="60" cy="60" r="52" fill="none"
            :stroke="scoreColor" stroke-width="8" stroke-linecap="round"
            :stroke-dasharray="`${(report?.healthScore ?? 0) * 3.27} 999`"
            transform="rotate(-90 60 60)"
          />
        </svg>
        <div class="score-text">
          <div class="score-value mono">{{ report?.healthScore ?? '--' }}</div>
          <div class="score-label">{{ scoreLabel }}</div>
        </div>
      </div>
      <div class="score-desc">
        <div class="desc-title">社区健康度</div>
        <div class="desc-text">AI 综合评估当日内容质量、互动健康度、违规趋势</div>
      </div>
    </div>

    <div class="ai-section" v-if="report?.violationTypes?.length">
      <div class="section-title">违规类型分布</div>
      <div class="violation-list">
        <div class="violation-item" v-for="item in report.violationTypes" :key="item.type">
          <span class="v-type">{{ item.type }}</span>
          <span class="v-count mono">{{ item.count }}</span>
        </div>
      </div>
    </div>

    <div class="ai-section" v-if="report?.suggestions?.length">
      <div class="section-title">运营优化建议</div>
      <ul class="suggestion-list">
        <li v-for="(s, i) in report.suggestions" :key="i">{{ s }}</li>
      </ul>
    </div>

    <div class="ai-footer">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ aiSourceText }}</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.ai-report-card {
  background: linear-gradient(135deg, $bg-elevated 0%, $bg-surface 100%);
  border: 1px solid $border-base;
  border-radius: $radius-lg;
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -40px; right: -40px;
    width: 120px; height: 120px;
    background: radial-gradient(circle, rgba(232, 121, 169, 0.15), transparent 70%);
    pointer-events: none;
  }
}
.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: rgba(232, 121, 169, 0.15);
  border: 1px solid rgba(232, 121, 169, 0.4);
  border-radius: 12px;
  color: $color-primary;
  font-size: 12px;
  font-weight: 600;
  box-shadow: $shadow-glow-primary;
}
.ai-date {
  font-size: 12px;
  color: $text-secondary;
}
.ai-score-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid $border-divider;
}
.score-ring {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  svg { width: 100%; height: 100%; }
}
.score-text {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}
.score-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--score-color);
}
.score-label {
  font-size: 11px;
  color: $text-secondary;
  margin-top: 2px;
}
.desc-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}
.desc-text {
  font-size: 12px;
  color: $text-secondary;
  line-height: 1.5;
}
.ai-section {
  margin-top: 14px;
}
.section-title {
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}
.violation-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.violation-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: $bg-input;
  border-radius: 6px;
  font-size: 12px;
  .v-type { color: $text-regular; }
  .v-count { color: $color-warning; font-weight: 600; }
}
.suggestion-list {
  list-style: none;
  padding: 0;
  li {
    position: relative;
    padding-left: 16px;
    font-size: 12px;
    color: $text-regular;
    line-height: 1.7;
    &::before {
      content: '';
      position: absolute;
      left: 0; top: 8px;
      width: 6px; height: 6px;
      background: $color-primary;
      transform: rotate(45deg);
    }
  }
}
.ai-footer {
  margin-top: auto;
  padding-top: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: $text-placeholder;
}
</style>
