<template>
  <div class="comment-item" :class="{ 'is-ai': comment.aiGenerated }">
    <div class="comment-item__main">
      <!-- 头像 -->
      <div class="comment-item__avatar" @click="handleClickUser">
        <img :src="comment.avatar || defaultAvatar" :alt="comment.nickname" />
      </div>

      <div class="comment-item__body">
        <!-- 昵称 + AI标记 -->
        <div class="comment-item__header">
          <span class="comment-item__nickname" @click="handleClickUser">{{ comment.nickname }}</span>
          <span v-if="comment.aiGenerated" class="comment-item__ai-tag">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="currentColor"/>
            </svg>
            AI
          </span>
          <span v-if="comment.replyTo" class="comment-item__reply-to">
            回复 <span class="comment-item__reply-name">@{{ comment.replyTo }}</span>
          </span>
        </div>

        <!-- 内容 -->
        <div class="comment-item__content">{{ comment.content }}</div>

        <!-- 评论图片 -->
        <div v-if="comment.images && comment.images.length" class="comment-item__images">
          <img
            v-for="(img, idx) in comment.images"
            :key="idx"
            :src="img"
            :alt="`评论图片${idx + 1}`"
            class="comment-item__img"
          />
        </div>

        <!-- 底部操作 -->
        <div class="comment-item__footer">
          <span class="comment-item__time">{{ formattedTime }}</span>
          <div class="comment-item__ops">
            <button
              class="comment-item__op"
              :class="{ 'is-liked': comment.liked }"
              @click="handleLike"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path
                  d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                  :fill="comment.liked ? 'currentColor' : 'none'"
                  stroke="currentColor"
                  stroke-width="2"
                />
              </svg>
              <span v-if="comment.likeCount">{{ formatNumber(comment.likeCount) }}</span>
            </button>
            <button class="comment-item__op" @click="handleReply">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              回复
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 楼中楼回复 -->
    <div v-if="comment.children && comment.children.length" class="comment-item__children">
      <CommentItem
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        @reply="(id: number) => emit('reply', id)"
        @like="(id: number) => emit('like', id)"
        @delete="(id: number) => emit('delete', id)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * CommentItem 评论项组件
 * @description 展示单条评论，支持楼中楼递归、AI标记、点赞动画和回复操作
 * @param {Comment} comment - 评论数据对象
 * @emits reply - 回复事件，参数为评论ID
 * @emits like - 点赞事件，参数为评论ID
 * @emits delete - 删除事件，参数为评论ID
 */

import { computed } from 'vue'
import type { Comment } from '@/types'
import { formatRelativeTime, formatNumber } from '@/utils/format'

const props = defineProps<{
  /** 评论数据 */
  comment: Comment
}>()

const emit = defineEmits<{
  /** 回复事件 */
  (e: 'reply', id: number): void
  /** 点赞事件 */
  (e: 'like', id: number): void
  /** 删除事件 */
  (e: 'delete', id: number): void
  /** 点击用户（头像/昵称），参数为用户ID */
  (e: 'click-user', userId: number): void
}>()

/** 默认头像 */
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>')

/** 格式化评论时间 */
const formattedTime = computed(() => formatRelativeTime(props.comment.createTime))

/** 处理点赞 */
function handleLike(): void {
  emit('like', props.comment)
}

/** 处理回复 */
function handleReply(): void {
  emit('reply', props.comment.id)
}

/** 处理头像/昵称点击，跳转用户主页 */
function handleClickUser(): void {
  emit('click-user', props.comment.userId)
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.comment-item {
  position: relative;

  &.is-ai {
    .comment-item__main {
      background: linear-gradient(135deg, rgba(78, 205, 196, 0.04), rgba(255, 182, 39, 0.04));
      border-radius: $radius-sm;
      padding: $space-2 $space-3;
      margin: -$space-2 -$space-3;
    }
  }

  &__main {
    display: flex;
    gap: $space-3;
  }

  &__avatar {
    @include hexagon(36px);
    flex-shrink: 0;
    overflow: hidden;
    cursor: pointer;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: $space-1;
    flex-wrap: wrap;
  }

  &__nickname {
    font-size: 13px;
    font-weight: 600;
    color: $ink-700;
    cursor: pointer;
    transition: color $dur-fast ease;

    &:hover {
      color: $mint-600;
    }
  }

  &__ai-tag {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-size: 10px;
    font-weight: 600;
    color: $amber-500;
    background: rgba(255, 182, 39, 0.1);
    padding: 1px 6px;
    border-radius: $radius-pill;
  }

  &__reply-to {
    font-size: 12px;
    color: $ink-300;
  }

  &__reply-name {
    color: $mint-600;
  }

  &__content {
    font-size: 14px;
    line-height: 1.6;
    color: $ink-700;
    margin-top: $space-1;
    word-break: break-word;
  }

  &__images {
    display: flex;
    gap: $space-1;
    margin-top: $space-2;
    flex-wrap: wrap;
  }

  &__img {
    width: 80px;
    height: 80px;
    object-fit: cover;
    border-radius: $radius-xs;
    cursor: zoom-in;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: $space-2;
  }

  &__time {
    font-size: 11px;
    color: $ink-300;
  }

  &__ops {
    display: flex;
    align-items: center;
    gap: $space-3;
  }

  &__op {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    background: none;
    border: none;
    font-size: 12px;
    color: $ink-300;
    cursor: pointer;
    padding: 0;
    transition: color $dur-fast ease;

    &:hover {
      color: $ink-500;
    }

    &.is-liked {
      color: $coral-500;

      svg {
        animation: like-burst 0.4s $ease-spring;
      }
    }
  }

  // ---------- 楼中楼 ----------
  &__children {
    margin-left: 48px;
    margin-top: $space-2;
    padding-left: $space-3;
    border-left: 2px solid $ink-50;
    display: flex;
    flex-direction: column;
    gap: $space-3;

    .comment-item__main {
      // 子评论稍小
      .comment-item__avatar {
        @include hexagon(28px);
      }
    }
  }
}
</style>
