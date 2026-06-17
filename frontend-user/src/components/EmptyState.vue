<template>
  <div class="empty-state">
    <!-- 蜂巢SVG插画 -->
    <div class="empty-state__illustration">
      <svg width="120" height="140" viewBox="0 0 120 140" fill="none" class="empty-state__hex-group">
        <!-- 大蜂巢 -->
        <path
          d="M60 10L105 36V88L60 114L15 88V36L60 10Z"
          stroke="url(#empty-grad-1)"
          stroke-width="2"
          fill="none"
          opacity="0.3"
        />
        <!-- 中蜂巢 -->
        <path
          d="M60 30L90 47V81L60 98L30 81V47L60 30Z"
          stroke="url(#empty-grad-1)"
          stroke-width="1.5"
          fill="none"
          opacity="0.5"
        />
        <!-- 小蜂巢 -->
        <path
          d="M60 48L78 58V78L60 88L42 78V58L60 48Z"
          fill="url(#empty-grad-2)"
          opacity="0.15"
        />
        <!-- 装饰点 -->
        <circle cx="60" cy="68" r="4" fill="#4ECDC4" opacity="0.4">
          <animate attributeName="opacity" values="0.2;0.6;0.2" dur="2s" repeatCount="indefinite" />
        </circle>
        <circle cx="42" cy="58" r="2" fill="#FFB627" opacity="0.3">
          <animate attributeName="opacity" values="0.1;0.5;0.1" dur="2.5s" repeatCount="indefinite" />
        </circle>
        <circle cx="78" cy="78" r="2" fill="#6BCB77" opacity="0.3">
          <animate attributeName="opacity" values="0.15;0.45;0.15" dur="3s" repeatCount="indefinite" />
        </circle>
        <defs>
          <linearGradient id="empty-grad-1" x1="15" y1="10" x2="105" y2="114">
            <stop offset="0%" stop-color="#4ECDC4" />
            <stop offset="100%" stop-color="#6BCB77" />
          </linearGradient>
          <linearGradient id="empty-grad-2" x1="42" y1="48" x2="78" y2="88">
            <stop offset="0%" stop-color="#4ECDC4" />
            <stop offset="100%" stop-color="#FFB627" />
          </linearGradient>
        </defs>
      </svg>
    </div>

    <!-- 提示文字 -->
    <h3 class="empty-state__title">{{ title }}</h3>
    <p v-if="description" class="empty-state__desc">{{ description }}</p>

    <!-- 操作按钮 -->
    <button v-if="actionText" class="empty-state__action" @click="emit('action')">
      {{ actionText }}
    </button>
  </div>
</template>

<script setup lang="ts">
/**
 * EmptyState 空状态组件
 * @description 蜂巢SVG插画+提示文字+操作按钮，用于列表为空时的占位展示
 * @param {string} title - 主标题
 * @param {string} description - 描述文字（可选）
 * @param {string} actionText - 操作按钮文字（可选）
 * @emits action - 操作按钮点击事件
 */

defineProps<{
  /** 主标题 */
  title: string
  /** 描述文字 */
  description?: string
  /** 操作按钮文字 */
  actionText?: string
}>()

const emit = defineEmits<{
  /** 操作按钮点击事件 */
  (e: 'action'): void
}>()
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.empty-state {
  @include center(column);
  padding: $space-10 $space-6;
  text-align: center;

  &__illustration {
    margin-bottom: $space-6;
    animation: float-deco 6s ease-in-out infinite;
  }

  &__hex-group {
    filter: drop-shadow(0 4px 12px rgba(78, 205, 196, 0.15));
  }

  &__title {
    font-family: $font-heading;
    font-size: 17px;
    font-weight: 600;
    color: $ink-700;
    margin: 0 0 $space-2;
  }

  &__desc {
    font-size: 14px;
    color: $ink-300;
    line-height: 1.6;
    margin: 0 0 $space-6;
    max-width: 280px;
  }

  &__action {
    display: inline-flex;
    align-items: center;
    gap: $space-1;
    padding: $space-2 $space-6;
    background: $grad-mint;
    color: #fff;
    border: none;
    border-radius: $radius-pill;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: transform $dur-base $ease-spring, box-shadow $dur-base ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: $shadow-glow-mint;
    }

    &:active {
      transform: translateY(0);
    }
  }
}
</style>
