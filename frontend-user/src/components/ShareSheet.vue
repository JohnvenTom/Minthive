<template>
  <Teleport to="body">
    <Transition name="share-sheet-fade">
      <div v-if="show" class="share-sheet-overlay" @click="close">
        <div class="share-sheet-panel" @click.stop>
          <!-- 标题栏 -->
          <div class="share-sheet__header">
            <h3 class="share-sheet__title">分享到</h3>
            <button class="share-sheet__close" @click="close">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </div>

          <!-- 分享选项列表 -->
          <div class="share-sheet__options">
            <!-- 转发到动态 -->
            <button class="share-sheet__option" @click="handleReshare">
              <span class="share-sheet__icon share-sheet__icon--reshare">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13"
                    stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
              <span class="share-sheet__label">转发到我的动态</span>
            </button>

            <!-- 复制链接 -->
            <button class="share-sheet__option" @click="handleCopyLink">
              <span class="share-sheet__icon share-sheet__icon--link">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"
                    stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"
                    stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
              <span class="share-sheet__label">复制链接</span>
            </button>

            <!-- 分享给好友（预留） -->
            <button class="share-sheet__option" @click="handleShareFriend">
              <span class="share-sheet__icon share-sheet__icon--friend">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"
                    stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
              <span class="share-sheet__label">分享给好友</span>
            </button>
          </div>

          <!-- 取消按钮 -->
          <button class="share-sheet__cancel" @click="close">取消</button>
        </div>
      </div>
    </Transition>

    <!-- 转发文案输入弹窗 -->
    <Teleport to="body">
      <Transition name="share-input-fade">
        <div v-if="showInput" class="share-input-overlay" @click="showInput = false">
          <div class="share-input-dialog" @click.stop>
            <h4 class="share-input__title">转发到动态</h4>
            <p class="share-input__hint">添加转发文案（可选）</p>
            <textarea
              ref="textareaRef"
              v-model="reshareText"
              class="share-input__textarea"
              placeholder="说说你的想法..."
              rows="3"
              maxlength="500"
            />
            <div class="share-input__footer">
              <span class="share-input__count">{{ reshareText.length }}/500</span>
              <div class="share-input__actions">
                <button class="share-input__btn share-input__btn--cancel" @click="showInput = false">
                  取消
                </button>
                <button
                  class="share-input__btn share-input__btn--confirm"
                  :class="{ 'is-loading': submitting }"
                  :disabled="submitting"
                  @click="confirmReshare"
                >
                  {{ submitting ? '发布中...' : '发布' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * ShareSheet 分享面板组件
 *
 * @description 基于 Vant 设计风格的自定义分享面板，提供三种分享操作：
 *   1. 转发到动态 - 弹出输入框让用户填写转发文案后调用接口创建新帖
 *   2. 复制链接 - 将帖子链接复制到剪贴板
 *   3. 分享给好友 - 功能预留（当前仅 toast 提示）
 *
 * @prop {boolean} show - 控制面板显示/隐藏（v-model 双向绑定）
 * @prop {number} postId - 目标帖子 ID
 * @prop {string} postContent - 原帖内容摘要，用于生成默认转发文案
 *
 * @emits update:show - 更新显示状态
 * @emits shared - 转发成功事件，通知父组件刷新数据
 */
import { ref, watch, nextTick } from 'vue'
import { showToast } from 'vant'
import { sharePost } from '@/api/post'

const props = defineProps<{
  /** 是否显示面板（v-model） */
  show: boolean
  /** 目标帖子ID */
  postId: number
  /** 原帖内容，用于生成默认转发文案 */
  postContent?: string
}>()

const emit = defineEmits<{
  /** 更新显示状态 */
  (e: 'update:show', value: boolean): void
  /** 转发成功回调 */
  (e: 'shared'): void
}>()

// ---------- 状态 ----------

/** 是否显示转发输入弹窗 */
const showInput = ref(false)

/** 转发文案内容 */
const reshareText = ref('')

/** 是否正在提交转发请求 */
const submitting = ref(false)

/** textarea DOM 引用（用于自动聚焦） */
const textareaRef = ref<HTMLTextAreaElement | null>(null)

// ---------- 方法 ----------

/**
 * 关闭分享面板
 * @description 重置内部状态并通知父组件关闭
 */
function close(): void {
  emit('update:show', false)
}

/**
 * 处理"转发到动态"选项点击
 * @description 打开转发文案输入弹窗，设置默认文案为 "转发: 原帖内容前50字"
 */
function handleReshare(): void {
  // 生成默认转发文案：原帖内容截取前50字符
  const defaultText = props.postContent
    ? `转发: ${props.postContent.slice(0, 50)}${props.postContent.length > 50 ? '...' : ''}`
    : ''
  reshareText.value = defaultText
  showInput.value = true

  // 下一帧自动聚焦 textarea
  nextTick(() => {
    textareaRef.value?.focus()
  })
}

/**
 * 处理"复制链接"选项点击
 * @description 使用 Clipboard API 复制当前页面链接到剪贴板，
 *   若 API 不可用则降级为传统 execCommand 方式
 */
async function handleCopyLink(): Promise<void> {
  const url = `${window.location.origin}/post/${props.postId}`

  try {
    // 优先使用现代 Clipboard API
    await navigator.clipboard.writeText(url)
    showToast('链接已复制')
    close()
    return
  } catch {
    // API 不可用时降级处理
  }

  // 降级方案：创建临时 textarea 用 execCommand 复制
  try {
    const textarea = document.createElement('textarea')
    textarea.value = url
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    showToast('链接已复制')
    close()
  } catch {
    showToast('复制失败，请手动复制链接')
  }
}

/**
 * 处理"分享给好友"选项点击（功能预留）
 * @description 当前版本仅展示开发中提示，后续可接入好友选择或第三方 SDK
 */
function handleShareFriend(): void {
  showToast('该功能开发中')
  close()
}

/**
 * 确认转发操作
 * @description 调用 sharePost 接口创建转发帖，成功后关闭所有弹窗并通知父组件
 * @returns {Promise<void>}
 */
async function confirmReshare(): Promise<void> {
  if (submitting.value) return

  submitting.value = true
  try {
    // 将用户填写的转发文案传给后端；空字符串不传，让后端生成默认文案
    const content = reshareText.value.trim() || undefined
    await sharePost(props.postId, content)
    showToast('转发成功')
    showInput.value = false
    close()
    emit('shared')
  } catch {
    showToast('转发失败，请重试')
  } finally {
    submitting.value = false
  }
}

// ---------- 监听 ----------

/**
 * 监听面板打开状态重置输入框
 * @description 每次打开时清空之前的输入内容
 */
watch(() => props.show, (val) => {
  if (!val) {
    showInput.value = false
    reshareText.value = ''
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

// ========== 遮罩层 + 面板容器 ==========
.share-sheet-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(26, 29, 46, 0.55);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.share-sheet-panel {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border-radius: $radius-lg $radius-lg 0 0;
  padding: $space-5 $space-5 $space-4;
  box-shadow: 0 -4px 24px rgba(26, 29, 46, 0.12);
  animation: slide-up 0.32s $ease-spring both;
}

// ========== 标题栏 ==========
.share-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-5;
}

.share-sheet__title {
  font-size: 16px;
  font-weight: 600;
  color: $ink-700;
  margin: 0;
}

.share-sheet__close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: none;
  border: none;
  color: $ink-500;
  cursor: pointer;
  transition: all $dur-fast ease;

  &:hover {
    background: $ink-50;
    color: $ink-700;
  }
}

// ========== 选项列表 ==========
.share-sheet__options {
  display: flex;
  gap: $space-4;
  margin-bottom: $space-5;
}

.share-sheet__option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-2;
  padding: $space-4 $space-2;
  background: $ink-50;
  border: none;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $dur-fast ease;

  &:hover {
    background: $mint-50;
    transform: translateY(-2px);

    .share-sheet__icon--reshare { color: $mint-600; }
    .share-sheet__icon--link { color: $sky-500; }
    .share-sheet__icon--friend { color: $amber-600; }
  }

  &:active {
    transform: scale(0.97);
  }
}

.share-sheet__icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-sm;
  transition: color $dur-fast ease;

  &--reshare {
    color: $mint-500;
    background: rgba(78, 205, 196, 0.1);
  }

  &--link {
    color: $info;
    background: rgba(77, 163, 255, 0.1);
  }

  &--friend {
    color: $amber-500;
    background: rgba(255, 182, 39, 0.1);
  }
}

.share-sheet__label {
  font-size: 13px;
  font-weight: 500;
  color: $ink-700;
}

// ========== 取消按钮 ==========
.share-sheet__cancel {
  width: 100%;
  padding: $space-3 0;
  font-size: 15px;
  font-weight: 500;
  color: $ink-700;
  background: $ink-50;
  border: none;
  border-radius: $radius-pill;
  cursor: pointer;
  transition: all $dur-fast ease;

  &:hover {
    background: $ink-100;
    color: $ink-900;
  }

  &:active {
    transform: scale(0.98);
  }
}

// ========== 转发输入弹窗 ==========
.share-input-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  background: rgba(26, 29, 46, 0.55);
  @include center;
}

.share-input-dialog {
  width: 88%;
  max-width: 420px;
  background: #fff;
  border-radius: $radius-lg;
  padding: $space-6;
  box-shadow: $shadow-lg;
  animation: scale-in 0.28s $ease-spring both;
}

.share-input__title {
  font-size: 17px;
  font-weight: 600;
  color: $ink-900;
  margin: 0 0 $space-1;
}

.share-input__hint {
  font-size: 13px;
  color: $ink-500;
  margin: 0 0 $space-4;
}

.share-input__textarea {
  width: 100%;
  min-height: 88px;
  padding: $space-3;
  font-size: 14px;
  line-height: 1.6;
  color: $ink-900;
  background: $ink-50;
  border: 1.5px solid transparent;
  border-radius: $radius-sm;
  resize: vertical;
  outline: none;
  transition: border-color $dur-fast ease;
  font-family: inherit;

  &:focus {
    border-color: $mint-500;
    background: #fff;
  }

  &::placeholder {
    color: $ink-300;
  }
}

.share-input__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $space-4;
}

.share-input__count {
  font-size: 12px;
  color: $ink-500;
}

.share-input__actions {
  display: flex;
  gap: $space-2;
}

.share-input__btn {
  padding: $space-2 $space-5;
  font-size: 14px;
  font-weight: 600;
  border-radius: $radius-pill;
  border: none;
  cursor: pointer;
  transition: all $dur-fast ease;

  &--cancel {
    color: $ink-700;
    background: $ink-50;

    &:hover {
      background: $ink-100;
      color: $ink-900;
    }
  }

  &--confirm {
    color: #fff;
    background: $grad-mint;

    &:hover:not(:disabled) {
      box-shadow: $shadow-glow-mint;
      transform: translateY(-1px);
    }

    &:active:not(:disabled) {
      transform: scale(0.96);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    &.is-loading {
      opacity: 0.8;
    }
  }
}

// ========== 过渡动画 ==========
.share-sheet-fade-enter-active {
  transition: opacity $dur-base $ease-out;
}
.share-sheet-fade-leave-active {
  transition: opacity 0.2s ease-in;
}
.share-sheet-fade-enter-from,
.share-sheet-fade-leave-to {
  opacity: 0;

  .share-sheet-panel {
    transform: translateY(100%);
  }
}

.share-input-fade-enter-active {
  transition: opacity $dur-base $ease-out;
}
.share-input-fade-leave-active {
  transition: opacity 0.18s ease-in;
}
.share-input-fade-enter-from,
.share-input-fade-leave-to {
  opacity: 0;
}

@keyframes slide-up {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

@keyframes scale-in {
  from {
    opacity: 0;
    transform: scale(0.92);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
