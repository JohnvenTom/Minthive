<template>
  <nav class="navbar" :class="{ 'navbar--hidden': isHidden }">
    <div class="navbar__inner">
      <!-- 左侧：返回 / Logo -->
      <div class="navbar__left">
        <button v-if="showBack" class="navbar__back" @click="handleBack">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M15 19l-7-7 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div v-else class="navbar__logo" @click="handleLogoClick">
          <!-- 蜂巢SVG -->
          <svg class="navbar__logo-hex" width="28" height="32" viewBox="0 0 28 32" fill="none">
            <path d="M14 0L27 8V24L14 32L1 24V8L14 0Z" fill="url(#logo-grad)" />
            <path d="M14 6L21 10V18L14 22L7 18V10L14 6Z" fill="rgba(255,255,255,0.3)" />
            <defs>
              <linearGradient id="logo-grad" x1="0" y1="0" x2="28" y2="32">
                <stop offset="0%" stop-color="#6BCB77" />
                <stop offset="100%" stop-color="#4ECDC4" />
              </linearGradient>
            </defs>
          </svg>
          <span class="navbar__logo-text">MintHive</span>
        </div>
      </div>

      <!-- 中间：标题 -->
      <div class="navbar__center">
        <h1 class="navbar__title">{{ title }}</h1>
      </div>

      <!-- 右侧：搜索 + 通知 -->
      <div class="navbar__right">
        <button v-if="showSearch" class="navbar__icon-btn" @click="emit('search')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2"/>
            <path d="M16 16l4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
        <button v-if="showNotify" class="navbar__icon-btn" @click="emit('notify')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="navbar__notify-dot"></span>
        </button>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
/**
 * NavBar 顶部导航栏组件
 * @description 毛玻璃效果固定顶部导航，包含Logo、标题、搜索和通知图标，适配移动端
 * @param {string} title - 页面标题
 * @param {boolean} showBack - 是否显示返回按钮，默认false
 * @param {boolean} showSearch - 是否显示搜索图标，默认true
 * @param {boolean} showNotify - 是否显示通知图标，默认true
 * @emits back - 返回按钮点击事件
 * @emits search - 搜索按钮点击事件
 * @emits notify - 通知按钮点击事件
 */

import { ref, onMounted, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  /** 页面标题 */
  title?: string
  /** 是否显示返回按钮 */
  showBack?: boolean
  /** 是否显示搜索图标 */
  showSearch?: boolean
  /** 是否显示通知图标 */
  showNotify?: boolean
}>(), {
  title: '',
  showBack: false,
  showSearch: true,
  showNotify: true
})

const emit = defineEmits<{
  /** 返回事件 */
  (e: 'back'): void
  /** 搜索事件 */
  (e: 'search'): void
  /** 通知事件 */
  (e: 'notify'): void
}>()

/** 导航栏是否隐藏（滚动隐藏） */
const isHidden = ref(false)

/** 上次滚动位置 */
let lastScrollY = 0

/**
 * 滚动事件处理：向下滚动隐藏，向上滚动显示
 */
function handleScroll(): void {
  const currentY = window.scrollY
  isHidden.value = currentY > lastScrollY && currentY > 60
  lastScrollY = currentY
}

/** 返回操作 */
function handleBack(): void {
  emit('back')
}

/** Logo点击操作 */
function handleLogoClick(): void {
  // 可扩展：跳转首页
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  transition: transform $dur-base $ease-out;

  &--hidden {
    transform: translateY(-100%);
  }

  &__inner {
    @include glass(20px, rgba(255, 255, 255, 0.82));
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 56px;
    padding: 0 $space-4;
    // 底部分割线
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(78, 205, 196, 0.15), transparent);
    }
  }

  // ---------- 左侧 ----------
  &__left {
    display: flex;
    align-items: center;
    min-width: 80px;
  }

  &__back {
    @include center;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: none;
    border: none;
    color: $ink-700;
    cursor: pointer;
    transition: background $dur-fast ease;

    &:hover {
      background: rgba(78, 205, 196, 0.1);
    }
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: $space-2;
    cursor: pointer;
  }

  &__logo-hex {
    filter: drop-shadow(0 2px 6px rgba(78, 205, 196, 0.3));
  }

  &__logo-text {
    font-family: $font-display;
    font-size: 20px;
    font-weight: 700;
    background: $grad-mint;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  // ---------- 中间 ----------
  &__center {
    flex: 1;
    text-align: center;
  }

  &__title {
    font-family: $font-heading;
    font-size: 17px;
    font-weight: 600;
    color: $ink-700;
    margin: 0;
    @include ellipsis-1;
  }

  // ---------- 右侧 ----------
  &__right {
    display: flex;
    align-items: center;
    gap: $space-2;
    min-width: 80px;
    justify-content: flex-end;
  }

  &__icon-btn {
    @include center;
    width: 38px;
    height: 38px;
    border-radius: 50%;
    background: none;
    border: none;
    color: $ink-700;
    cursor: pointer;
    position: relative;
    transition: background $dur-fast ease;

    &:hover {
      background: rgba(78, 205, 196, 0.08);
    }
  }

  &__notify-dot {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 8px;
    height: 8px;
    background: $coral-500;
    border-radius: 50%;
    border: 2px solid #fff;
    animation: hex-breathe 2s ease-in-out infinite;
  }
}

// ---------- 移动端适配 ----------
@include mobile {
  .navbar {
    &__inner {
      height: 50px;
      padding: 0 $space-3;
    }

    &__logo-text {
      font-size: 17px;
    }

    &__title {
      font-size: 15px;
    }
  }
}
</style>
