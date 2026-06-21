<template>
  <div class="ai-assistant">
    <!-- 聊天面板 -->
    <Transition name="panel-slide" appear>
      <div v-show="visible" class="ai-assistant__panel">
        <!-- 面板头部 -->
        <div class="ai-assistant__panel-header">
          <div class="ai-assistant__panel-title">
            <svg width="18" height="20" viewBox="0 0 28 32" fill="none">
              <path d="M14 0L27 8V24L14 32L1 24V8L14 0Z" fill="url(#panel-grad)" />
              <defs>
                <linearGradient id="panel-grad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#6BCB77" />
                  <stop offset="100%" stop-color="#4ECDC4" />
                </linearGradient>
              </defs>
            </svg>
            <span>MintHive AI</span>
          </div>
          <button class="ai-assistant__close" @click="handleClose">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-cap="round"/>
            </svg>
          </button>
        </div>

        <!-- 消息列表 -->
        <div class="ai-assistant__messages" ref="messagesRef">
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="ai-assistant__msg"
            :class="`is-${msg.role}`"
          >
            <div v-if="msg.role === 'ai'" class="ai-assistant__msg-avatar">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="#FFB627"/>
              </svg>
            </div>
            <div class="ai-assistant__msg-bubble">
              {{ msg.content }}
            </div>
          </div>

          <!-- 流式输出中 -->
          <div v-if="isLoading" class="ai-assistant__msg is-ai">
            <div class="ai-assistant__msg-avatar">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="#FFB627"/>
              </svg>
            </div>
            <div class="ai-assistant__msg-bubble">
              {{ streamingText }}<span class="ai-assistant__cursor"></span>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="ai-assistant__input-area">
          <input
            v-model="inputText"
            class="ai-assistant__input"
            type="text"
            placeholder="问我任何问题..."
            @keyup.enter="sendMessage"
          />
          <button
            class="ai-assistant__send"
            :disabled="!inputText.trim() || isLoading"
            @click="sendMessage"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
/**
 * AiAssistant AI助手聊天面板组件
 * @description 纯面板组件，由父组件控制显隐，点击关闭按钮通知父组件关闭
 * @emits close - 用户点击关闭按钮时触发，通知父组件隐藏此组件
 */

import { ref, nextTick } from 'vue'
import { aiChatStream } from '@/api/ai'

/** 面板是否可见（用于 v-show + Transition 动画） */
const visible = ref(true)

/** 输入框文本 */
const inputText = ref('')

/** 是否正在加载AI回复（流式输出中） */
const isLoading = ref(false)

/** 流式输出控制器（用于中断请求） */
let abortController: AbortController | null = null

/** 消息列表 */
const messages = ref<Array<{ role: 'user' | 'ai'; content: string }>>([
  { role: 'ai', content: '你好！我是 MintHive AI 助手，有什么可以帮你的吗？' }
])

/** 消息列表DOM引用 */
const messagesRef = ref<HTMLElement | null>(null)

/**
 * 流式输出中的文本（独立于 messages 数组）
 * @description 流式接收期间实时追加内容，结束后推入 messages 并清空
 */
const streamingText = ref('')

/** 流式文本缓冲区 */
let streamBuffer = ''

const emit = defineEmits<{
  (e: 'close'): void
}>()

/**
 * 关闭面板（先播放退出动画，动画结束后通知父组件销毁）
 * @description 设置 visible=false 触发 v-show 隐藏 + Transition 退出动画，300ms 后通知父组件
 */
function handleClose(): void {
  visible.value = false
  setTimeout(() => {
    emit('close')
  }, 300)
}

/**
 * 发送消息（流式输出）
 * @description 发送用户消息后通过 SSE 流式接收 AI 回复，逐字显示打字机效果
 */
async function sendMessage(): Promise<void> {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return

  // 如果有正在进行的流式请求，先中断它
  if (abortController) {
    abortController.abort()
    abortController = null
  }

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  isLoading.value = true
  streamingText.value = ''
  streamBuffer = ''

  await scrollToBottom()

  abortController = aiChatStream(
    text,
    // onChunk：每收到一段文本就追加到缓冲区
    (chunk) => {
      streamBuffer = streamBuffer + chunk
      streamingText.value = streamBuffer
      autoScroll()
    },
    // onDone：流结束
    () => {
      abortController = null
      if (streamBuffer) {
        messages.value.push({ role: 'ai', content: streamBuffer })
        streamBuffer = ''
        streamingText.value = ''
      }
      isLoading.value = false
      scrollToBottom()
    },
    // onError：错误处理
    (err) => {
      abortController = null
      console.error('[AI] 流式请求失败:', err)
      const finalText = streamBuffer || '抱歉，我暂时无法回答这个问题，请稍后再试。'
      messages.value.push({ role: 'ai', content: finalText })
      streamBuffer = ''
      streamingText.value = ''
      isLoading.value = false
      scrollToBottom()
    }
  )
}

/**
 * 滚动消息列表到底部
 */
async function scrollToBottom(): Promise<void> {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

/**
 * 流式输出时自动滚动（节流，每 100ms 最多滚动一次）
 */
let lastScrollTime = 0
function autoScroll(): void {
  const now = Date.now()
  if (now - lastScrollTime > 100) {
    lastScrollTime = now
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.ai-assistant {
  position: fixed;
  bottom: 80px;
  right: 84px; /* 避免与发帖 FAB 重叠 */
  z-index: 200;

  // ---------- 聊天面板 ----------
  &__panel {
    position: relative;
    width: 340px;
    height: 480px;
    @include glass(24px, rgba(255, 255, 255, 0.92));
    border-radius: $radius-lg;
    box-shadow: $shadow-lg;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  &__panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-3 $space-4;
    border-bottom: 1px solid $ink-50;
  }

  &__panel-title {
    display: flex;
    align-items: center;
    gap: $space-2;
    font-family: $font-heading;
    font-size: 15px;
    font-weight: 600;
    color: $ink-700;
  }

  &__close {
    @include center;
    width: 28px;
    height: 28px;
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

  // ---------- 消息列表 ----------
  &__messages {
    flex: 1;
    overflow-y: auto;
    padding: $space-3 $space-4;
    @include custom-scrollbar;
  }

  &__msg {
    display: flex;
    gap: $space-2;
    margin-bottom: $space-3;

    &.is-user {
      flex-direction: row-reverse;

      .ai-assistant__msg-bubble {
        background: $grad-mint;
        color: #fff;
        border-radius: $radius-md $radius-xs $radius-md $radius-md;
      }
    }

    &.is-ai {
      .ai-assistant__msg-bubble {
        background: $ink-50;
        color: $ink-700;
        border-radius: $radius-xs $radius-md $radius-md $radius-md;
      }
    }
  }

  &__msg-avatar {
    @include center;
    width: 28px;
    height: 28px;
    flex-shrink: 0;
  }

  &__msg-bubble {
    max-width: 75%;
    padding: $space-2 $space-3;
    font-size: 13px;
    line-height: 1.6;
    word-break: break-word;
  }

  /** 流式输出时的闪烁光标 */
  &__cursor {
    display: inline-block;
    width: 2px;
    height: 14px;
    background: $mint-500;
    margin-left: 1px;
    vertical-align: text-bottom;
    animation: cursor-blink 1s step-end infinite;
  }

  @keyframes cursor-blink {
    0%, 100% { opacity: 1; }
    50% { opacity: 0; }
  }

  // ---------- 输入区域 ----------
  &__input-area {
    display: flex;
    align-items: center;
    gap: $space-2;
    padding: $space-3 $space-4;
    border-top: 1px solid $ink-50;
  }

  &__input {
    flex: 1;
    height: 36px;
    border: 1px solid $ink-100;
    border-radius: $radius-pill;
    padding: 0 $space-4;
    font-size: 13px;
    color: $ink-700;
    background: #fff;
    outline: none;
    transition: border-color $dur-fast ease;

    &::placeholder {
      color: $ink-300;
    }

    &:focus {
      border-color: $mint-500;
      box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.12);
    }
  }

  &__send {
    @include center;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: $grad-mint;
    border: none;
    color: #fff;
    cursor: pointer;
    flex-shrink: 0;
    transition: opacity $dur-fast ease, transform $dur-fast ease;

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    &:not(:disabled):hover {
      transform: scale(1.05);
    }

    &:not(:disabled):active {
      transform: scale(0.95);
    }
  }
}

// ---------- 面板过渡动画 ----------
.panel-slide-enter-active {
  animation: panel-pop-in 0.4s $ease-spring both;
}
.panel-slide-leave-active {
  animation: panel-pop-out 0.25s ease both;
}

@keyframes panel-pop-in {
  0% {
    opacity: 0;
    transform: translateY(20px) scale(0.85) translateX(20px);
  }
  60% {
    opacity: 1;
    transform: translateY(-4px) scale(1.02) translateX(2px);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1) translateX(0);
  }
}

@keyframes panel-pop-out {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(16px) scale(0.92) translateX(16px);
  }
}

// ---------- 移动端适配 ----------
@include mobile {
  .ai-assistant {
    bottom: 72px;
    right: 16px;

    &__panel {
      width: calc(100vw - 32px);
      max-width: 360px;
    }
  }
}
</style>
