<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="report-dialog__overlay" @click="handleOverlayClick">
        <div class="report-dialog" @click.stop>
          <!-- 头部 -->
          <div class="report-dialog__header">
            <h3 class="report-dialog__title">举报内容</h3>
            <button class="report-dialog__close" @click="handleClose">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </div>

          <!-- 举报类型 -->
          <div class="report-dialog__section">
            <p class="report-dialog__label">举报类型</p>
            <div class="report-dialog__types">
              <button
                v-for="item in reportTypes"
                :key="item.value"
                class="report-dialog__type-btn"
                :class="{ 'is-selected': selectedType === item.value }"
                @click="selectedType = item.value"
              >
                <span class="report-dialog__type-icon">{{ item.icon }}</span>
                <span class="report-dialog__type-text">{{ item.label }}</span>
              </button>
            </div>
          </div>

          <!-- 举报理由 -->
          <div class="report-dialog__section">
            <p class="report-dialog__label">补充说明（可选）</p>
            <textarea
              v-model="reason"
              class="report-dialog__textarea"
              placeholder="请描述举报理由..."
              maxlength="200"
              rows="3"
            ></textarea>
            <span class="report-dialog__counter">{{ reason.length }}/200</span>
          </div>

          <!-- 提交按钮 -->
          <button
            class="report-dialog__submit"
            :disabled="!selectedType || isSubmitting"
            @click="handleSubmit"
          >
            <LoadingSpinner v-if="isSubmitting" size="sm" />
            <span v-else>{{ isSubmitting ? '提交中...' : '提交举报' }}</span>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * ReportDialog 举报弹窗组件
 * @description 举报类型选择+理由输入+提交，调用举报API
 * @param {boolean} visible - 弹窗是否可见
 * @param {number} targetId - 被举报内容ID
 * @param {string} targetType - 被举报内容类型
 * @emits update:visible - 更新弹窗可见状态
 * @emits success - 举报成功事件
 */

import { ref, watch } from 'vue'
import { report } from '@/api/report'
import LoadingSpinner from './LoadingSpinner.vue'

const props = defineProps<{
  /** 弹窗是否可见 */
  visible: boolean
  /** 被举报内容ID */
  targetId: number
  /** 被举报内容类型 */
  targetType: 'post' | 'comment' | 'message' | 'user'
}>()

const emit = defineEmits<{
  /** 更新弹窗可见状态 */
  (e: 'update:visible', val: boolean): void
  /** 举报成功事件 */
  (e: 'success'): void
}>()

/** 举报类型选项 */
const reportTypes = [
  { value: 'porn' as const, label: '低俗色情', icon: '🚫' },
  { value: 'ad' as const, label: '广告引流', icon: '📢' },
  { value: 'attack' as const, label: '人身攻击', icon: '👊' },
  { value: 'illegal' as const, label: '违法内容', icon: '⚠️' },
  { value: 'copy' as const, label: '抄袭搬运', icon: '📋' }
]

/** 选中的举报类型 */
const selectedType = ref<'porn' | 'ad' | 'attack' | 'illegal' | 'copy' | ''>('')

/** 举报理由 */
const reason = ref('')

/** 是否正在提交 */
const isSubmitting = ref(false)

/** 弹窗打开时重置表单 */
watch(() => props.visible, (val) => {
  if (val) {
    selectedType.value = ''
    reason.value = ''
    isSubmitting.value = false
  }
})

/** 点击遮罩关闭 */
function handleOverlayClick(): void {
  handleClose()
}

/** 关闭弹窗 */
function handleClose(): void {
  emit('update:visible', false)
}

/**
 * 提交举报
 * @description 校验并调用举报API
 */
async function handleSubmit(): Promise<void> {
  if (!selectedType.value || isSubmitting.value) return

  isSubmitting.value = true
  try {
    await report({
      targetId: props.targetId,
      targetType: props.targetType,
      type: selectedType.value,
      reason: reason.value
    })
    emit('success')
    handleClose()
  } catch {
    // 错误已由request拦截器统一处理
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.report-dialog {
  &__overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: rgba(26, 29, 46, 0.5);
    @include center;
    padding: $space-4;
  }

  // ---------- 弹窗主体 ----------
  position: relative;
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  padding: $space-5;
  animation: scale-in 0.3s $ease-spring;

  // ---------- 头部 ----------
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $space-5;
  }

  &__title {
    font-family: $font-heading;
    font-size: 18px;
    font-weight: 700;
    color: $ink-700;
    margin: 0;
  }

  &__close {
    @include center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: none;
    border: none;
    color: $ink-300;
    cursor: pointer;
    transition: all $dur-fast ease;

    &:hover {
      background: $ink-50;
      color: $ink-700;
    }
  }

  // ---------- 区块 ----------
  &__section {
    margin-bottom: $space-4;
  }

  &__label {
    font-size: 13px;
    font-weight: 600;
    color: $ink-500;
    margin: 0 0 $space-2;
  }

  // ---------- 类型选择 ----------
  &__types {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: $space-2;

    // 最后一行奇数项居左
    & > :last-child:nth-child(odd) {
      grid-column: 1;
    }
  }

  &__type-btn {
    display: flex;
    align-items: center;
    gap: $space-2;
    padding: $space-2 $space-3;
    border: 1.5px solid $ink-100;
    border-radius: $radius-sm;
    background: #fff;
    cursor: pointer;
    font-size: 13px;
    color: $ink-500;
    transition: all $dur-fast ease;

    &:hover {
      border-color: $mint-300;
      background: $mint-50;
    }

    &.is-selected {
      border-color: $mint-500;
      background: $mint-50;
      color: $mint-600;
      box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.1);
    }
  }

  &__type-icon {
    font-size: 16px;
  }

  &__type-text {
    font-weight: 500;
  }

  // ---------- 理由输入 ----------
  &__textarea {
    width: 100%;
    border: 1.5px solid $ink-100;
    border-radius: $radius-sm;
    padding: $space-3;
    font-size: 14px;
    color: $ink-700;
    resize: none;
    outline: none;
    font-family: $font-body;
    transition: border-color $dur-fast ease;

    &::placeholder {
      color: $ink-300;
    }

    &:focus {
      border-color: $mint-500;
      box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.1);
    }
  }

  &__counter {
    display: block;
    text-align: right;
    font-size: 11px;
    color: $ink-300;
    margin-top: $space-1;
  }

  // ---------- 提交按钮 ----------
  &__submit {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: $space-2;
    padding: $space-3;
    background: $grad-mint;
    color: #fff;
    border: none;
    border-radius: $radius-sm;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all $dur-fast ease;

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    &:not(:disabled):hover {
      box-shadow: $shadow-glow-mint;
    }
  }
}

// ---------- 过渡动画 ----------
.dialog-fade-enter-active {
  .report-dialog__overlay {
    animation: fade-in 0.25s ease both;
  }
}

.dialog-fade-leave-active {
  .report-dialog__overlay {
    animation: fade-in 0.2s ease reverse;
  }
}

// ---------- 移动端适配 ----------
@include mobile {
  .report-dialog {
    max-width: 100%;
    margin: $space-4;
    border-radius: $radius-md;

    &__types {
      grid-template-columns: 1fr;
    }
  }
}
</style>
