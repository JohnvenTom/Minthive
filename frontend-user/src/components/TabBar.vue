<template>
  <nav class="tabbar">
    <div class="tabbar__inner">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tabbar__item"
        :class="{
          'is-active': activeTab === tab.key,
          'is-center': tab.key === 'post'
        }"
        @click="handleChange(tab.key)"
      >
        <!-- 发帖按钮特殊设计 -->
        <template v-if="tab.key === 'post'">
          <div class="tabbar__post-btn">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
            </svg>
            <!-- 蜂巢纹理装饰 -->
            <svg class="tabbar__post-hex" width="52" height="60" viewBox="0 0 52 60" fill="none">
              <path d="M26 0L52 15V45L26 60L0 45V15L26 0Z" stroke="rgba(255,255,255,0.2)" stroke-width="1"/>
              <path d="M26 12L40 20V36L26 44L12 36V20L26 12Z" stroke="rgba(255,255,255,0.1)" stroke-width="0.5"/>
            </svg>
          </div>
        </template>

        <template v-else>
          <!-- 图标 -->
          <span class="tabbar__icon">
            <!-- 首页 -->
            <svg v-if="tab.key === 'home'" width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-4 0a1 1 0 01-1-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 01-1 1" :stroke="activeTab === tab.key ? 'currentColor' : 'currentColor'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- 圈子 -->
            <svg v-if="tab.key === 'circle'" width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M17 20a4 4 0 100-8 4 4 0 000 8zM9 12a4 4 0 100-8 4 4 0 000 8zM7 20c0-3.3 2.7-6 6-6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- 消息 -->
            <svg v-if="tab.key === 'message'" width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- 我的 -->
            <svg v-if="tab.key === 'me'" width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </span>
          <!-- 标签文字 -->
          <span class="tabbar__label">{{ tab.label }}</span>
          <!-- 未读红点 -->
          <span v-if="tab.key === 'message' && hasUnread" class="tabbar__dot"></span>
        </template>
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
/**
 * TabBar 底部导航栏组件
 * @description 移动端固定底部导航，5个Tab含特殊发帖按钮，选中高亮动画和未读红点
 * @param {string} activeTab - 当前激活的Tab键名
 * @emits change - Tab切换事件，参数为Tab键名
 */

import { computed } from 'vue'

const props = defineProps<{
  /** 当前激活Tab */
  activeTab: 'home' | 'circle' | 'post' | 'message' | 'me'
}>()

const emit = defineEmits<{
  /** Tab切换事件 */
  (e: 'change', key: string): void
}>()

/** Tab配置列表 */
const tabs = [
  { key: 'home', label: '首页' },
  { key: 'circle', label: '圈子' },
  { key: 'post', label: '' },
  { key: 'message', label: '消息' },
  { key: 'me', label: '我的' }
]

/** 是否有未读消息（可扩展为实际逻辑） */
const hasUnread = computed(() => true)

/**
 * 处理Tab切换
 * @param {string} key - Tab键名
 */
function handleChange(key: string): void {
  emit('change', key)
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom, 0px);

  &__inner {
    @include glass(20px, rgba(255, 255, 255, 0.88));
    display: flex;
    align-items: flex-end;
    justify-content: space-around;
    height: 60px;
    padding: 0 $space-2;
    // 顶部分割线
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(78, 205, 196, 0.15), transparent);
    }
  }

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: none;
    border: none;
    cursor: pointer;
    padding: $space-1 0;
    min-width: 56px;
    position: relative;
    color: $ink-300;
    transition: color $dur-fast ease;

    &.is-active {
      color: $mint-600;

      .tabbar__icon {
        transform: scale(1.1);
      }

      .tabbar__label {
        font-weight: 600;
      }
    }

    &.is-center {
      margin-top: -16px;
    }
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: transform $dur-base $ease-spring;
  }

  &__label {
    font-size: 10px;
    margin-top: 2px;
    font-family: $font-heading;
    transition: font-weight $dur-fast ease;
  }

  &__dot {
    position: absolute;
    top: 4px;
    right: 12px;
    width: 7px;
    height: 7px;
    background: $coral-500;
    border-radius: 50%;
    border: 1.5px solid #fff;
  }

  // ---------- 发帖按钮 ----------
  &__post-btn {
    @include center;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: $grad-mint;
    color: #fff;
    position: relative;
    box-shadow: $shadow-glow-mint;
    animation: ai-glow 3s ease-in-out infinite;
    transition: transform $dur-base $ease-spring;

    &:active {
      transform: scale(0.92);
    }
  }

  &__post-hex {
    position: absolute;
    inset: -2px;
    margin: auto;
    width: 52px;
    height: 60px;
    pointer-events: none;
    opacity: 0.6;
  }
}
</style>
