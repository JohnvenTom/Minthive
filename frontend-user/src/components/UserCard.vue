<template>
  <div class="user-card" @click="handleClick">
    <!-- 六边形头像 -->
    <div class="user-card__avatar">
      <img :src="user.avatar || defaultAvatar" :alt="user.nickname" />
    </div>

    <!-- 用户信息 -->
    <div class="user-card__info">
      <h3 class="user-card__nickname">{{ user.nickname }}</h3>
      <p v-if="user.bio" class="user-card__bio">{{ user.bio }}</p>

      <!-- 兴趣标签 -->
      <div v-if="user.interests && user.interests.length" class="user-card__tags">
        <span
          v-for="tag in user.interests.slice(0, 3)"
          :key="tag"
          class="user-card__tag"
        >
          {{ tag }}
        </span>
      </div>
    </div>

    <!-- 关注按钮 -->
    <button
      v-if="showFollow"
      class="user-card__follow-btn"
      :class="{ 'is-followed': isFollowed }"
      @click.stop="handleFollow"
    >
      {{ isFollowed ? '已关注' : '关注' }}
    </button>
  </div>
</template>

<script setup lang="ts">
/**
 * UserCard 用户卡片组件
 * @description 展示用户信息，包含六边形头像、昵称、简介、兴趣标签和关注按钮
 * @param {User} user - 用户数据对象
 * @param {boolean} showFollow - 是否显示关注按钮，默认true
 * @emits follow - 关注/取关事件，参数为用户ID
 * @emits click - 卡片点击事件，参数为用户ID
 */

import { computed } from 'vue'
import type { User } from '@/types'

const props = withDefaults(defineProps<{
  /** 用户数据 */
  user: User
  /** 是否显示关注按钮 */
  showFollow?: boolean
}>(), {
  showFollow: true
})

const emit = defineEmits<{
  /** 关注/取关事件 */
  (e: 'follow', id: number): void
  /** 卡片点击事件 */
  (e: 'click', id: number): void
}>()

/** 默认头像 */
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>')

/**
 * 是否已关注
 * @description 从 user 对象的 isFollowing/isFollowed 字段读取初始状态，
 *   确保关注列表中的用户默认显示为已关注状态
 */
const isFollowed = computed(() => !!(props.user as any).isFollowing || !!(props.user as any).isFollowed)

/**
 * 处理关注/取关
 * @description 向父组件发送关注/取关事件，由父组件管理状态
 */
function handleFollow(): void {
  const willFollow = !isFollowed.value
  emit('follow', props.user.id, willFollow)
}

/** 处理卡片点击 */
function handleClick(): void {
  emit('click', props.user.id)
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.user-card {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  background: #fff;
  border-radius: $radius-md;
  box-shadow: $shadow-xs;
  cursor: pointer;
  transition: box-shadow $dur-base $ease-out, transform $dur-base $ease-out;

  &:hover {
    box-shadow: $shadow-sm;
    transform: translateY(-1px);
  }

  // ---------- 头像 ----------
  &__avatar {
    @include hexagon(48px);
    flex-shrink: 0;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  // ---------- 信息 ----------
  &__info {
    flex: 1;
    min-width: 0;
  }

  &__nickname {
    font-family: $font-heading;
    font-size: 15px;
    font-weight: 600;
    color: $ink-700;
    margin: 0;
    @include ellipsis-1;
  }

  &__bio {
    font-size: 12px;
    color: $ink-300;
    line-height: 1.4;
    margin: $space-1 0 0;
    @include ellipsis(1);
  }

  // ---------- 标签 ----------
  &__tags {
    display: flex;
    gap: $space-1;
    margin-top: $space-1;
    flex-wrap: wrap;
  }

  &__tag {
    font-size: 10px;
    color: $mint-600;
    background: $mint-50;
    padding: 1px 8px;
    border-radius: $radius-pill;
    white-space: nowrap;
  }

  // ---------- 关注按钮 ----------
  &__follow-btn {
    flex-shrink: 0;
    padding: $space-1 $space-4;
    border-radius: $radius-pill;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all $dur-fast ease;
    background: $grad-mint;
    color: #fff;
    border: none;

    &.is-followed {
      background: $ink-50;
      color: $ink-500;
      border: 1px solid $ink-100;
    }

    &:not(.is-followed):hover {
      box-shadow: $shadow-glow-mint;
      transform: scale(1.02);
    }

    &.is-followed:hover {
      color: $danger;
      border-color: $danger;
      background: rgba(255, 107, 107, 0.05);
    }
  }
}
</style>
