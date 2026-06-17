<script setup lang="ts">
/**
 * HexagonLogo 蜂巢 Logo 组件
 * 功能：渲染蜂巢六边形 Logo，呼应 MintHive 品牌蜂巢意象
 * Props:
 *   - size: 尺寸（px），默认 40
 *   - withText: 是否展示 MintHive Admin 文字，默认 true
 *   - textLight: 文字是否使用浅色（用于深色背景），默认 true
 */
withDefaults(defineProps<{
  size?: number
  withText?: boolean
  textLight?: boolean
}>(), {
  size: 40,
  withText: true,
  textLight: true
})
</script>

<template>
  <div class="hex-logo" :style="{ '--logo-size': `${size}px` }">
    <svg class="hex-svg" viewBox="0 0 48 48" :width="size" :height="size">
      <defs>
        <linearGradient id="hexGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#4ECDC4" />
          <stop offset="100%" stop-color="#3FB8B0" />
        </linearGradient>
        <filter id="hexGlow">
          <feGaussianBlur stdDeviation="1.2" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <polygon
        points="24,2 44,13 44,35 24,46 4,35 4,13"
        fill="url(#hexGrad)"
        filter="url(#hexGlow)"
      />
      <polygon points="24,10 36,17 36,31 24,38 12,31 12,17" fill="#1A1D2E" />
      <circle cx="24" cy="24" r="3.5" fill="#FFB627" />
    </svg>
    <div v-if="withText" class="hex-text" :class="{ light: textLight }">
      <span class="brand">MintHive</span>
      <span class="sub">Admin</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.hex-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.hex-svg {
  display: block;
}
.hex-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
  .brand {
    font-family: $font-title;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 0.5px;
  }
  .sub {
    font-family: $font-mono;
    font-size: 11px;
    letter-spacing: 2px;
    opacity: 0.7;
  }
  &.light {
    .brand { color: $text-primary; }
    .sub { color: $color-primary; }
  }
  &:not(.light) {
    .brand { color: $bg-base; }
    .sub { color: $color-primary-active; }
  }
}
</style>
