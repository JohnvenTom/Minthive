<template>
  <div class="not-found-page">
    <div class="not-found-page__bg">
      <!-- 蜂巢装饰 -->
      <svg class="not-found-page__hex not-found-page__hex--1" width="120" height="140" viewBox="0 0 28 32" fill="none">
        <path d="M14 0L27 8V24L14 32L1 24V8L14 0Z" fill="currentColor" opacity="0.06" />
      </svg>
      <svg class="not-found-page__hex not-found-page__hex--2" width="80" height="93" viewBox="0 0 28 32" fill="none">
        <path d="M14 0L27 8V24L14 32L1 24V8L14 0Z" fill="currentColor" opacity="0.04" />
      </svg>
      <svg class="not-found-page__hex not-found-page__hex--3" width="60" height="70" viewBox="0 0 28 32" fill="none">
        <path d="M14 0L27 8V24L14 32L1 24V8L14 0Z" fill="currentColor" opacity="0.05" />
      </svg>
    </div>

    <div class="not-found-page__content">
      <div class="not-found-page__icon">
        <svg width="64" height="74" viewBox="0 0 28 32" fill="none">
          <path d="M14 0L27 8V24L14 32L1 24V8L14 0Z" fill="url(#404-grad)" />
          <path d="M14 8L20 12V20L14 24L8 20V12L14 8Z" fill="rgba(255,255,255,0.25)" />
          <defs>
            <linearGradient id="404-grad" x1="0" y1="0" x2="28" y2="32">
              <stop offset="0%" stop-color="#6BCB77" />
              <stop offset="100%" stop-color="#4ECDC4" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      <h1 class="not-found-page__code">404</h1>
      <p class="not-found-page__title">页面走丢了</p>
      <p class="not-found-page__desc">你访问的页面不存在，可能已被移动或删除</p>

      <button class="not-found-page__btn" @click="goHome">
        返回首页
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * NotFoundPage 404页面
 * @description 当用户访问不存在的路由时展示的404错误页面，提供返回首页按钮
 */

import { useRouter } from 'vue-router'

const router = useRouter()

/**
 * 返回首页
 */
function goHome(): void {
  router.push('/')
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.not-found-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, $ink-50 0%, $mint-50 50%, $ink-50 100%);
  overflow: hidden;

  &__bg {
    position: absolute;
    inset: 0;
    pointer-events: none;
  }

  &__hex {
    position: absolute;
    color: $mint-500;

    &--1 {
      top: 10%;
      right: 8%;
      animation: hex-breathe 6s ease-in-out infinite;
    }

    &--2 {
      bottom: 15%;
      left: 5%;
      animation: hex-breathe 8s ease-in-out infinite 1s;
    }

    &--3 {
      top: 40%;
      left: 15%;
      animation: hex-breathe 7s ease-in-out infinite 2s;
    }
  }

  &__content {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: $space-6;
    animation: fade-up 0.6s $ease-spring;
  }

  &__icon {
    margin-bottom: $space-4;
    animation: hex-breathe 3s ease-in-out infinite;
  }

  &__code {
    font-family: $font-display;
    font-size: 96px;
    font-weight: 900;
    background: $grad-mint;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    line-height: 1;
    margin-bottom: $space-2;
  }

  &__title {
    font-family: $font-heading;
    font-size: 22px;
    font-weight: 700;
    color: $ink-700;
    margin-bottom: $space-2;
  }

  &__desc {
    font-size: 14px;
    color: $ink-400;
    text-align: center;
    margin-bottom: $space-6;
  }

  &__btn {
    @include center;
    height: 44px;
    padding: 0 $space-8;
    border: none;
    border-radius: $radius-pill;
    background: $grad-mint;
    color: #fff;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: transform $dur-fast $ease-spring, box-shadow $dur-fast ease;
    box-shadow: 0 4px 16px rgba(78, 205, 196, 0.3);

    &:hover {
      transform: scale(1.03);
      box-shadow: 0 6px 24px rgba(78, 205, 196, 0.4);
    }

    &:active {
      transform: scale(0.97);
    }
  }
}
</style>
