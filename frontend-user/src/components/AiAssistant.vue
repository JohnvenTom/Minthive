<template>
  <div class="ai-assistant">
    <!-- 悬浮按钮（蜂巢形状） -->
    <button
      class="ai-assistant__fab"
      :class="{ 'is-open': isOpen }"
      @click="togglePanel"
    >
      <svg width="32" height="37" viewBox="0 0 28 32" fill="none">
        <defs>
          <linearGradient id="fab-grad" x1="0" y1="0" x2="28" y2="32">
            <stop offset="0%" stop-color="#6BCB77" />
            <stop offset="100%" stop-color="#4ECDC4" />
          </linearGradient>
          <!-- 六边形边缘发光滤镜：基于形状实际轮廓扩散，而非矩形边界框 -->
          <filter id="fab-glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur in="SourceGraphic" stdDeviation="1.5" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>
        <path d="M14 0L27 8V24L14 32L1 24V8L14 0Z" fill="url(#fab-grad)" filter="url(#fab-glow)" />
        <path d="M14 8L20 12V20L14 24L8 20V12L14 8Z" fill="rgba(255,255,255,0.25)" />
      </svg>
      <span class="ai-assistant__fab-icon">
        <svg v-if="!isOpen" width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="white"/>
        </svg>
        <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none">
          <path d="M18 6L6 18M6 6l12 12" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
        </svg>
      </span>
    </button>

    <!-- 聊天面板 -->
    <Transition name="panel-slide">
      <div v-if="isOpen" class="ai-assistant__panel">
        <!-- 面板头部 -->
        <div class="ai-assistant__panel-header">
          <div class="ai-assistant__panel-title">
            <svg width="18" height="20" viewBox="0 0 28 32" fill="none">
              <path d="M14 0L27 8V24L14 32L1 24V8L14 0Z" fill="url(#panel-grad)" />
              <defs>
                <linearGradient id="panel-grad" x1="0" y1="0" x2="28" y2="32">
                  <stop offset="0%" stop-color="#6BCB77" />
                  <stop offset="100%" stop-color="#4ECDC4" />
                </linearGradient>
              </defs>
            </svg>
            <span>MintHive AI</span>
          </div>
          <button class="ai-assistant__close" @click="togglePanel">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
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

          <!-- 加载状态 -->
          <div v-if="isLoading" class="ai-assistant__msg is-ai">
            <div class="ai-assistant__msg-avatar">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="#FFB627"/>
              </svg>
            </div>
            <div class="ai-assistant__msg-bubble ai-assistant__loading">
              <span class="ai-assistant__loading-dot"></span>
              <span class="ai-assistant__loading-dot"></span>
              <span class="ai-assistant__loading-dot"></span>
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
 * AiAssistant AI悬浮客服组件
 * @description 固定右下角蜂巢悬浮按钮，点击展开毛玻璃聊天面板，支持AI对话和加载动画
 */

import { ref, nextTick } from 'vue'
import { aiChat } from '@/api/ai'

/** 面板是否展开 */
const isOpen = ref(false)

/** 输入框文本 */
const inputText = ref('')

/** 是否正在加载AI回复 */
const isLoading = ref(false)

/** 消息列表 */
const messages = ref<Array<{ role: 'user' | 'ai'; content: string }>>([
  { role: 'ai', content: '你好！我是 MintHive AI 助手，有什么可以帮你的吗？' }
])

/** 消息列表DOM引用 */
const messagesRef = ref<HTMLElement | null>(null)

/** 切换面板展开/收起 */
function togglePanel(): void {
  isOpen.value = !isOpen.value
}

/**
 * 发送消息
 * @description 发送用户消息并调用AI问答接口获取回复
 */
async function sendMessage(): Promise<void> {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  isLoading.value = true

  await scrollToBottom()

  try {
    const res = await aiChat(text)
    // 后端返回 Result<String>，data 字段直接是回答文本
    const answer = typeof res.data === 'string' ? res.data : (res.data as any)?.answer
    messages.value.push({
      role: 'ai',
      content: answer || '抱歉，我暂时无法回答这个问题，请稍后再试。'
    })
  } catch {
    messages.value.push({
      role: 'ai',
      content: '网络异常，请稍后再试。'
    })
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
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
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.ai-assistant {
  position: fixed;
  bottom: 80px;
  right: 84px;  /* 避免与发帖 FAB（right:20px, width:56px）重叠 */
  z-index: 200;

  // ---------- 悬浮按钮 ----------
  &__fab {
    position: relative;
    width: 56px;
    height: 65px;
    border: none;
    background: none;
    cursor: pointer;
    animation: hex-breathe 3s ease-in-out infinite;
    transition: transform $dur-base $ease-spring;

    &:hover {
      transform: scale(1.08);
    }

    &:active {
      transform: scale(0.95);
    }

    &.is-open {
      animation: none;
    }
  }

  &__fab-icon {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    @include center;
  }

  // ---------- 聊天面板 ----------
  &__panel {
    position: absolute;
    bottom: 72px;
    right: 0;
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

  // ---------- 加载状态 ----------
  &__loading {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: $space-2 $space-3;
  }

  &__loading-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: $mint-500;
    animation: ai-pulse 1.4s ease-in-out infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
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

// ---------- 面板过渡动画（从FAB按钮向上弹出） ----------
.panel-slide-enter-active {
  animation: fab-pop-in 0.35s $ease-spring both;
}
.panel-slide-leave-active {
  animation: fab-pop-out 0.2s ease both;
}

@keyframes fab-pop-in {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.92);
    transform-origin: bottom right;
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    transform-origin: bottom right;
  }
}

@keyframes fab-pop-out {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
    transform-origin: bottom right;
  }
  to {
    opacity: 0;
    transform: translateY(12px) scale(0.95);
    transform-origin: bottom right;
  }
}

// ---------- 移动端适配 ----------
@include mobile {
  .ai-assistant {
    bottom: 72px;
    right: 76px;  /* 移动端发帖 FAB 在 right:16px width:50px，AI按钮左移避免重叠 */

    &__panel {
      width: calc(100vw - 24px);
      right: -4px;
    }
  }
}
</style>
