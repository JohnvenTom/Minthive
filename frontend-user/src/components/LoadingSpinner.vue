<template>
  <div class="loading-spinner" :class="[`is-${size}`, { 'is-ai': aiMode }]">
    <div class="loading-spinner__hex-wrap">
      <!-- 外层蜂巢 -->
      <svg class="loading-spinner__hex loading-spinner__hex--outer" :style="sizeStyle" viewBox="0 0 56 64" fill="none">
        <path d="M28 0L56 16V48L28 64L0 48V16L28 0Z" stroke="url(#spin-grad)" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-dasharray="80 200" />
      </svg>
      <!-- 内层蜂巢 -->
      <svg class="loading-spinner__hex loading-spinner__hex--inner" :style="innerSizeStyle" viewBox="0 0 56 64" fill="none">
        <path d="M28 8L48 20V44L28 56L8 44V20L28 8Z" :fill="aiMode ? 'url(#spin-ai-fill)' : 'url(#spin-fill)'" opacity="0.2" />
      </svg>
      <!-- 渐变定义 -->
      <svg class="loading-spinner__defs" width="0" height="0">
        <defs>
          <linearGradient id="spin-grad" x1="0" y1="0" x2="56" y2="64">
            <stop offset="0%" stop-color="#4ECDC4" />
            <stop offset="100%" stop-color="#6BCB77" />
          </linearGradient>
          <linearGradient id="spin-fill" x1="0" y1="0" x2="56" y2="64">
            <stop offset="0%" stop-color="#4ECDC4" />
            <stop offset="100%" stop-color="#6BCB77" />
          </linearGradient>
          <linearGradient id="spin-ai-fill" x1="0" y1="0" x2="56" y2="64">
            <stop offset="0%" stop-color="#4ECDC4" />
            <stop offset="50%" stop-color="#FFB627" />
            <stop offset="100%" stop-color="#4ECDC4" />
          </linearGradient>
        </defs>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * LoadingSpinner 加载动画组件
 * @description 蜂巢旋转加载动画，支持AI模式（薄荷绿→琥珀色脉冲切换）和三种尺寸
 * @param {string} size - 尺寸：sm/md/lg，默认md
 * @param {boolean} aiMode - 是否为AI专属加载模式，默认false
 */

import { computed } from 'vue'

const props = withDefaults(defineProps<{
  /** 尺寸：sm/md/lg */
  size?: 'sm' | 'md' | 'lg'
  /** 是否为AI专属加载模式 */
  aiMode?: boolean
}>(), {
  size: 'md',
  aiMode: false
})

/** 尺寸映射 */
const sizeMap = {
  sm: 32,
  md: 48,
  lg: 72
}

/** 外层尺寸样式 */
const sizeStyle = computed(() => ({
  width: `${sizeMap[props.size]}px`,
  height: `${sizeMap[props.size] * 1.14}px`
}))

/** 内层尺寸样式 */
const innerSizeStyle = computed(() => ({
  width: `${sizeMap[props.size] * 0.65}px`,
  height: `${sizeMap[props.size] * 0.65 * 1.14}px`
}))
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.loading-spinner {
  @include center;

  &__hex-wrap {
    position: relative;
    @include center;
  }

  &__hex {
    position: absolute;

    &--outer {
      animation: hex-spin 2s linear infinite;
      filter: drop-shadow(0 0 8px rgba(78, 205, 196, 0.3));
    }

    &--inner {
      animation: hex-breathe 2s ease-in-out infinite;
    }
  }

  &__defs {
    position: absolute;
    width: 0;
    height: 0;
    overflow: hidden;
  }

  // ---------- AI模式 ----------
  &.is-ai {
    .loading-spinner__hex--outer {
      animation: hex-spin 1.8s linear infinite, ai-pulse 2.4s ease-in-out infinite;
    }

    .loading-spinner__hex--inner {
      animation: hex-breathe 1.5s ease-in-out infinite;
      filter: drop-shadow(0 0 12px rgba(255, 182, 39, 0.3));
    }
  }

  // ---------- 尺寸 ----------
  &.is-sm {
    .loading-spinner__hex--outer {
      animation-duration: 1.5s;
    }
  }

  &.is-lg {
    .loading-spinner__hex--outer {
      animation-duration: 2.5s;
    }
  }
}
</style>
