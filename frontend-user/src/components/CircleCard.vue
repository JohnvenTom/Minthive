<template>
  <div class="circle-card" @click="handleClick">
    <!-- 顶部Banner -->
    <div class="circle-card__banner" :style="bannerStyle">
      <div class="circle-card__banner-overlay"></div>
      <!-- 公开/私密标记 -->
      <span class="circle-card__type-badge" :class="circle.type">
        <svg v-if="circle.type === 'private'" width="10" height="10" viewBox="0 0 24 24" fill="none">
          <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" stroke-width="2"/>
          <path d="M7 11V7a5 5 0 0110 0v4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        {{ circle.type === 'public' ? '公开' : '私密' }}
      </span>
    </div>

    <!-- 圈子信息 -->
    <div class="circle-card__info">
      <!-- 六边形头像 -->
      <div class="circle-card__avatar">
        <img :src="circle.avatar || defaultAvatar" :alt="circle.name" />
      </div>

      <!-- 名称 + 分类 -->
      <div class="circle-card__name-row">
        <h3 class="circle-card__name">{{ circle.name }}</h3>
        <span class="circle-card__category">{{ circle.categoryName || '未分类' }}</span>
      </div>

      <!-- 简介 -->
      <p class="circle-card__intro">{{ circle.intro }}</p>

      <!-- 统计 -->
      <div class="circle-card__stats">
        <div class="circle-card__stat">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M17 20a4 4 0 100-8 4 4 0 000 8zM9 12a4 4 0 100-8 4 4 0 000 8z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <span>{{ formatNumber(circle.memberCount) }} 成员</span>
        </div>
        <div class="circle-card__stat">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M18.5 2.5a2.12 2.12 0 013 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>{{ formatNumber(circle.postCount) }} 帖子</span>
        </div>
      </div>

      <!-- 加入按钮 -->
      <button
        class="circle-card__join-btn"
        :class="{ 'is-joined': circle.joined }"
        @click.stop="handleJoin"
      >
        <template v-if="circle.joined">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          已加入
        </template>
        <template v-else>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
          </svg>
          加入
        </template>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * CircleCard 圈子卡片组件
 * @description 展示圈子信息，包含六边形头像、名称、分类、简介、统计数据和加入按钮
 * @param {Circle} circle - 圈子数据对象
 * @emits join - 加入/退出圈子事件，参数为圈子ID
 * @emits click - 卡片点击事件，参数为圈子ID
 */

import { computed } from 'vue'
import type { Circle } from '@/types'
import { formatNumber } from '@/utils/format'

const props = defineProps<{
  /** 圈子数据 */
  circle: Circle
}>()

const emit = defineEmits<{
  /** 加入/退出圈子事件 */
  (e: 'join', id: number): void
  /** 卡片点击事件 */
  (e: 'click', id: number): void
}>()

/** 默认圈子头像 */
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="14">C</text></svg>')

/** Banner背景样式 */
const bannerStyle = computed(() => ({
  backgroundImage: props.circle.banner
    ? `url(${props.circle.banner})`
    : $gradMint
}))

/** 获取SCSS变量值（用于JS中） */
const $gradMint = 'linear-gradient(135deg, #6BCB77 0%, #4ECDC4 100%)'

/** 处理加入/退出 */
function handleJoin(): void {
  emit('join', props.circle.id)
}

/** 处理卡片点击 */
function handleClick(): void {
  emit('click', props.circle.id)
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.circle-card {
  background: #fff;
  border-radius: $radius-md;
  overflow: hidden;
  box-shadow: $shadow-xs;
  transition: box-shadow $dur-base $ease-out, transform $dur-base $ease-out;
  cursor: pointer;

  &:hover {
    box-shadow: $shadow-sm;
    transform: translateY(-2px);
  }

  // ---------- Banner ----------
  &__banner {
    height: 80px;
    background-size: cover;
    background-position: center;
    position: relative;
  }

  &__banner-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, transparent 40%, rgba(26, 29, 46, 0.3) 100%);
  }

  &__type-badge {
    position: absolute;
    top: $space-2;
    right: $space-2;
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 10px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: $radius-pill;
    backdrop-filter: blur(8px);

    &.public {
      background: rgba(78, 205, 196, 0.2);
      color: $mint-600;
    }

    &.private {
      background: rgba(255, 182, 39, 0.2);
      color: $amber-600;
    }
  }

  // ---------- 信息区 ----------
  &__info {
    padding: 0 $space-4 $space-4;
    position: relative;
  }

  &__avatar {
    @include hexagon(52px);
    margin-top: -26px;
    margin-bottom: $space-2;
    overflow: hidden;
    position: relative;
    z-index: 1;
    border: 3px solid #fff;
    box-shadow: $shadow-xs;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__name-row {
    display: flex;
    align-items: center;
    gap: $space-2;
    margin-bottom: $space-1;
  }

  &__name {
    font-family: $font-heading;
    font-size: 16px;
    font-weight: 700;
    color: $ink-700;
    margin: 0;
    @include ellipsis-1;
  }

  &__category {
    font-size: 11px;
    color: $mint-600;
    background: $mint-50;
    padding: 1px 8px;
    border-radius: $radius-pill;
    flex-shrink: 0;
  }

  &__intro {
    font-size: 13px;
    color: $ink-500;
    line-height: 1.5;
    margin: 0 0 $space-3;
    @include ellipsis(2);
  }

  // ---------- 统计 ----------
  &__stats {
    display: flex;
    gap: $space-4;
    margin-bottom: $space-3;
  }

  &__stat {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: $ink-300;
  }

  // ---------- 加入按钮 ----------
  &__join-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: $space-1;
    padding: $space-2;
    border-radius: $radius-sm;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all $dur-fast ease;
    background: $grad-mint;
    color: #fff;
    border: none;

    &.is-joined {
      background: $ink-50;
      color: $ink-500;
      border: 1px solid $ink-100;
    }

    &:not(.is-joined):hover {
      box-shadow: $shadow-glow-mint;
    }
  }
}
</style>
