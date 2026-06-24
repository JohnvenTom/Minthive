<!--
  App.vue - MintHive 根组件
  @component App
  @description 应用根组件，包含顶部导航栏（桌面端）、底部 TabBar（移动端）、
    路由视图（带页面切换动画）、全局悬浮 AI 客服入口、蜂巢装饰背景
-->
<template>
  <div class="app-root">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" aria-hidden="true" />

    <!-- 桌面端顶部导航栏 -->
    <header v-if="!isMobile && showNavbar" class="top-nav">
      <div class="top-nav__inner">
        <!-- Logo -->
        <router-link to="/" class="top-nav__logo">
          <svg class="logo-hex" viewBox="0 0 48 56" width="36" height="42">
            <polygon points="24,0 48,14 48,42 24,56 0,42 0,14"
              fill="url(#logo-grad)" />
            <defs>
              <linearGradient id="logo-grad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#6BCB77" />
                <stop offset="100%" stop-color="#4ECDC4" />
              </linearGradient>
            </defs>
          </svg>
          <span class="logo-text">MintHive</span>
        </router-link>

        <!-- 导航链接 -->
        <nav class="top-nav__links">
          <router-link to="/" class="nav-link" active-class="nav-link--active">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12l9-9 9 9M5 10v10a1 1 0 001 1h3a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1h3a1 1 0 001-1V10" />
            </svg>
            首页
          </router-link>
          <router-link to="/circle" class="nav-link" active-class="nav-link--active">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 6v6l4 2" />
            </svg>
            圈子
          </router-link>
          <router-link to="/messages" class="nav-link" active-class="nav-link--active">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
            </svg>
            消息
          </router-link>
        </nav>

        <!-- 右侧操作区 -->
        <div class="top-nav__actions">
          <router-link to="/search" class="action-btn" title="搜索">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" />
              <path d="M21 21l-4.35-4.35" />
            </svg>
          </router-link>
          <router-link to="/profile" class="action-btn" title="我的">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
          </router-link>
        </div>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content" :class="{ 'main-content--with-nav': !isMobile && showNavbar, 'main-content--with-tabbar': isMobile && showTabBar }">
      <router-view v-slot="{ Component, route }">
        <transition :name="route.meta.transition as string || 'fade-slide'" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>

    <!-- 移动端底部 TabBar -->
    <nav v-if="isMobile && showTabBar" class="bottom-tabbar">
      <router-link to="/" class="tab-item" active-class="tab-item--active">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 12l9-9 9 9M5 10v10a1 1 0 001 1h3a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1h3a1 1 0 001-1V10" />
        </svg>
        <span>首页</span>
      </router-link>

      <router-link to="/circle" class="tab-item" active-class="tab-item--active">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <path d="M12 6v6l4 2" />
        </svg>
        <span>圈子</span>
      </router-link>

      <!-- 发帖按钮 - 突出放大带发光 -->
      <router-link to="/post/create" class="tab-item tab-item--create">
        <div class="create-btn">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
        </div>
      </router-link>

      <router-link to="/messages" class="tab-item" active-class="tab-item--active">
        <div class="tab-item__icon-wrap">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
          </svg>
          <span v-if="unreadBadge" class="tab-badge">{{ unreadBadge }}</span>
        </div>
        <span>消息</span>
      </router-link>

      <router-link to="/profile" class="tab-item" active-class="tab-item--active">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
        <span>我的</span>
      </router-link>
    </nav>

    <!-- 全局悬浮 AI 客服入口 -->
    <button v-if="showAiButton" class="ai-fab" :class="{ 'ai-fab--active': appStore.showAiAssistant }"
      @click="appStore.toggleAiAssistant()" aria-label="AI 助手">
      <div class="ai-fab__hex">
        <svg viewBox="0 0 48 56" width="40" height="46">
          <polygon points="24,2 46,15 46,41 24,54 2,41 2,15"
            fill="url(#ai-fab-grad)" stroke="rgba(255,255,255,0.3)" stroke-width="1" />
          <defs>
            <linearGradient id="ai-fab-grad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#4ECDC4" />
              <stop offset="100%" stop-color="#FFB627" />
            </linearGradient>
          </defs>
        </svg>
        <span class="ai-fab__text">AI</span>
      </div>
    </button>

    <!-- AI 助手聊天面板 -->
    <AiAssistant v-if="appStore.showAiAssistant" @close="appStore.toggleAiAssistant()" />
  </div>
</template>

<script setup lang="ts">
/**
 * App 根组件逻辑
 * @description 管理响应式布局检测、导航显隐、未读消息徽标、AI 客服弹窗
 */
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import AiAssistant from '@/components/AiAssistant.vue'

const route = useRoute()
const appStore = useAppStore()
const chatStore = useChatStore()
const userStore = useUserStore()

/** 窗口宽度响应式引用 */
import { ref } from 'vue'
const windowWidth = ref(window.innerWidth)

/**
 * 判断是否为移动端视图
 * @returns {boolean} 窗口宽度 <= 768px 时为 true
 */
const isMobile = computed(() => windowWidth.value <= 768)

/**
 * 判断是否显示顶部导航栏
 * @description 登录页、兴趣选择页不显示
 * @returns {boolean}
 */
const showNavbar = computed(() => {
  const hiddenRoutes = ['/login', '/interest-select']
  return !hiddenRoutes.includes(route.path)
})

/**
 * 判断是否显示底部 TabBar
 * @description 登录页、兴趣选择页、帖子详情、聊天页等不显示
 * @returns {boolean}
 */
const showTabBar = computed(() => {
  const hiddenPrefixes = ['/login', '/interest-select', '/post/', '/chat/', '/settings']
  return !hiddenPrefixes.some(p => route.path.startsWith(p))
})

/**
 * 判断是否显示 AI 悬浮按钮
 * @description 登录页和兴趣选择页不显示
 * @returns {boolean}
 */
const showAiButton = computed(() => {
  const hiddenRoutes = ['/login', '/interest-select']
  return !hiddenRoutes.includes(route.path)
})

/**
 * 未读消息徽标
 * @returns {string | number} 未读数量，超过99显示 99+
 */
const unreadBadge = computed(() => {
  const count = chatStore.unreadCount
  if (count === 0) return ''
  return count > 99 ? '99+' : count
})

/**
 * 窗口尺寸变化处理
 * @returns {void}
 */
function handleResize(): void {
  windowWidth.value = window.innerWidth
  appStore.isMobile = windowWidth.value <= 768
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize()

  // 页面加载时清空 AI 助手历史（确保刷新后不残留）
  appStore.resetAiChatHistory()

  // 监听用户 ID 变化（切换账号时清空 AI 历史）
  watch(() => userStore.userInfo?.id, (newId, oldId) => {
    if (newId && oldId && newId !== oldId) {
      appStore.resetAiChatHistory()
    }
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
@use '@/styles/mixins.scss' as *;

// ============================================================
// 根组件样式
// ============================================================

.app-root {
  position: relative;
  min-height: 100vh;
  overflow-x: hidden;
}

// ---------- 桌面端顶部导航栏 ----------
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 64px;
  @include glass(20px, rgba(255, 255, 255, 0.82));
  border-bottom: 1px solid rgba(78, 205, 196, 0.12);

  &__inner {
    max-width: 1200px;
    margin: 0 auto;
    height: 100%;
    padding: 0 $space-6;
    display: flex;
    align-items: center;
    gap: $space-8;
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: $space-2;
    text-decoration: none;
    flex-shrink: 0;

    .logo-hex {
      animation: hex-breathe 4s ease-in-out infinite;
    }

    .logo-text {
      font-family: $font-display;
      font-size: 22px;
      font-weight: 700;
      background: $grad-aurora;
      -webkit-background-clip: text;
      background-clip: text;
      -webkit-text-fill-color: transparent;
    }
  }

  &__links {
    display: flex;
    gap: $space-1;
    flex: 1;
  }

  &__actions {
    display: flex;
    gap: $space-2;
  }
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: $space-2 $space-4;
  border-radius: $radius-pill;
  font-size: 14px;
  font-weight: 500;
  color: $ink-500;
  transition: all $dur-fast $ease-out;

  &:hover {
    color: $mint-500;
    background: rgba(78, 205, 196, 0.08);
  }

  &--active {
    color: $mint-500;
    background: rgba(78, 205, 196, 0.12);
    font-weight: 600;
  }
}

.action-btn {
  @include center;
  width: 40px;
  height: 40px;
  border-radius: $radius-pill;
  color: $ink-500;
  transition: all $dur-fast $ease-out;

  &:hover {
    color: $mint-500;
    background: rgba(78, 205, 196, 0.1);
  }
}

// ---------- 主内容区域 ----------
.main-content {
  min-height: 100vh;
  padding-bottom: $space-4;
  // 桌面端宽屏留白，内容居中
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;

  &--with-nav {
    padding-top: 64px;
  }

  &--with-tabbar {
    padding-bottom: 72px;
  }
}

// ---------- 页面切换动画 ----------

/** 默认淡入上滑 */
.fade-slide-enter-active {
  animation: fade-up 0.35s $ease-out;
}

.fade-slide-leave-active {
  animation: fade-up 0.25s $ease-out reverse;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/** 发帖页面 - 从按钮位置弹性缩放弹出 */
.scale-in-enter-active {
  animation: scale-pop-in 0.4s $ease-spring both;
}

.scale-in-leave-active {
  animation: scale-pop-out 0.2s ease both;
}

@keyframes scale-pop-in {
  0% {
    opacity: 0;
    transform: scale(0.85) translateY(20px);
  }
  60% {
    opacity: 1;
    transform: scale(1.02) translateY(-3px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes scale-pop-out {
  from {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
  to {
    opacity: 0;
    transform: scale(0.92) translateY(12px);
  }
}

// ---------- 移动端底部 TabBar ----------
.bottom-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 64px;
  padding-bottom: env(safe-area-inset-bottom, 0);
  @include glass(20px, rgba(255, 255, 255, 0.88));
  border-top: 1px solid rgba(78, 205, 196, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 0;
  text-decoration: none;
  color: $ink-300;
  font-size: 10px;
  font-weight: 500;
  transition: color $dur-fast $ease-out;
  position: relative;

  &--active {
    color: $mint-500;

    svg {
      filter: drop-shadow(0 0 6px rgba(78, 205, 196, 0.5));
    }
  }

  // 发帖按钮 - 突出放大
  &--create {
    margin-top: -20px;

    .create-btn {
      @include center;
      width: 52px;
      height: 52px;
      border-radius: 50%;
      background: $grad-mint;
      color: white;
      box-shadow: $shadow-glow-mint;
      animation: ai-glow 3s ease-in-out infinite;
      transition: transform $dur-fast $ease-spring;

      &:active {
        transform: scale(0.92);
      }
    }

    span {
      display: none;
    }
  }

  &__icon-wrap {
    position: relative;
  }
}

.tab-badge {
  position: absolute;
  top: -4px;
  right: -8px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: $radius-pill;
  background: $coral-500;
  color: white;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
}

// ---------- AI 悬浮按钮 ----------
.ai-fab {
  position: fixed;
  bottom: 160px;
  right: 20px;
  z-index: 90;
  border: none;
  background: none;
  cursor: pointer;
  padding: 0;
  animation: ai-glow 3s ease-in-out infinite;
  transition: transform $dur-base $ease-spring;

  &:hover {
    transform: scale(1.1);
  }

  &:active {
    transform: scale(0.95);
  }

  &--active {
    .ai-fab__hex svg polygon {
      fill: url(#ai-fab-grad);
    }
  }

  &__hex {
    position: relative;
    @include center;
  }

  &__text {
    position: absolute;
    font-family: $font-heading;
    font-size: 14px;
    font-weight: 800;
    color: white;
    letter-spacing: 1px;
    text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  }
}
</style>
