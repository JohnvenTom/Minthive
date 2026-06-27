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
              <span v-if="msg.hasRealData" class="ai-assistant__data-badge">📊 实时数据</span>
              <template v-for="(seg, si) in (msg.segments as MessageSegment[]) || [{ type: 'text', content: msg.content } as MessageSegment]" :key="si">
                <span v-if="seg.type === 'text'" v-html="renderMarkdown(seg.content || '')" class="ai-assistant__text"></span>
                <span
                  v-else-if="seg.type === 'navigation'"
                  v-for="(item, ni) in seg.items"
                  :key="ni"
                  class="ai-assistant__nav-chip"
                  @click="navigateTo(item)"
                >
                  <span class="ai-assistant__nav-chip-icon">{{ navIcon(item.type) }}</span>
                  <span class="ai-assistant__nav-chip-label">{{ item.label }}</span>
                  <span class="ai-assistant__nav-chip-preview">{{ item.preview }}</span>
                  <svg class="ai-assistant__nav-chip-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path d="M5 12h14M13 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </span>
              </template>
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
              <!-- 进度状态（LLM 流式输出前显示，切换时有向上淡出动画） -->
              <Transition name="status-fade">
                <div v-if="streamStatus && !streamBuffer" :key="streamStatus.message" class="ai-assistant__status">
                  <span class="ai-assistant__status-text">{{ streamStatus.message }}</span>
                  <span class="ai-assistant__status-dots">
                    <span class="dot">.</span><span class="dot">.</span><span class="dot">.</span>
                  </span>
                </div>
              </Transition>
              <span v-if="streamMeta.hasRealData" class="ai-assistant__data-badge">📊 实时数据</span>
              <template v-for="(seg, si) in streamSegments" :key="si">
                <span v-if="seg.type === 'text'" v-html="renderMarkdown(seg.content || '')" class="ai-assistant__text"></span>
                <span
                  v-else-if="seg.type === 'navigation'"
                  v-for="(item, ni) in seg.items"
                  :key="ni"
                  class="ai-assistant__nav-chip"
                  @click="navigateTo(item)"
                >
                  <span class="ai-assistant__nav-chip-icon">{{ navIcon(item.type) }}</span>
                  <span class="ai-assistant__nav-chip-label">{{ item.label }}</span>
                  <span class="ai-assistant__nav-chip-preview">{{ item.preview }}</span>
                  <svg class="ai-assistant__nav-chip-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path d="M5 12h14M13 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </span>
              </template>
              <span v-if="!streamStatus" class="ai-assistant__cursor"></span>
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

import { ref, nextTick, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { aiQueryStream } from '@/api/ai'
import type { NavigationAction } from '@/api/ai'
import { useAppStore } from '@/stores/app'
import { marked } from 'marked'

interface MessageSegment {
  type: 'text' | 'navigation'
  content?: string
  items?: NavigationAction[]
}

interface AiMessage {
  role: 'user' | 'ai'
  content: string
  segments?: MessageSegment[]
  hasRealData?: boolean
}

const appStore = useAppStore()
const router = useRouter()

/** 面板是否可见（用于 v-show + Transition 动画） */
const visible = ref(true)

/** 输入框文本 */
const inputText = ref('')

/** 是否正在加载AI回复（流式输出中） */
const isLoading = ref(false)

/** 流式输出控制器（用于中断请求） */
let abortController: AbortController | null = null

/** 消息列表（引用 appStore，组件销毁不丢失，页面刷新自动清空） */
const messages = appStore.aiChatHistory as AiMessage[]

/** 消息列表DOM引用 */
const messagesRef = ref<HTMLElement | null>(null)

/** 流式输出中的分段（文本 + 导航卡片混合） */
const streamSegments = ref<MessageSegment[]>([])

/** 流式元数据 */
const streamMeta = reactive({ hasRealData: false })

/** 流式文本缓冲区 */
let streamBuffer = ''

/** 当前进度状态 */
const streamStatus = ref<{ status: string; message: string } | null>(null)

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

  messages.push({ role: 'user', content: text })
  inputText.value = ''
  isLoading.value = true
  streamSegments.value = [{ type: 'text', content: '' }]
  streamMeta.hasRealData = false
  streamBuffer = ''
  streamStatus.value = null

  await scrollToBottom()

  abortController = aiQueryStream(
    text,
    {
      onStatus: (status) => {
        streamStatus.value = status
      },
      onChunk: (chunk) => {
        // 收到第一个文本片段时清除状态提示
        if (streamBuffer === '' && streamStatus.value) {
          streamStatus.value = null
        }
        streamBuffer += chunk
        // 更新最后一个 text segment 的内容
        const segs = streamSegments.value
        const last = segs[segs.length - 1]
        if (last && last.type === 'text') {
          last.content = streamBuffer
        } else {
          segs.push({ type: 'text', content: streamBuffer })
        }
        autoScroll()
      },
      onNavigation: (items) => {
        streamSegments.value.push({ type: 'navigation', items })
        autoScroll()
      },
      onMeta: (meta) => {
        streamMeta.hasRealData = meta.hasRealData
      },
      onDone: () => {
        abortController = null
        if (streamBuffer) {
          messages.push({
            role: 'ai',
            content: streamBuffer,
            segments: streamSegments.value.length > 1 ? streamSegments.value : undefined,
            hasRealData: streamMeta.hasRealData
          })
        }
        streamBuffer = ''
        streamSegments.value = []
        streamMeta.hasRealData = false
        isLoading.value = false
        scrollToBottom()
      },
      onError: (err) => {
        abortController = null
        console.error('[AI] 流式请求失败:', err)
        const finalText = streamBuffer || '抱歉，我暂时无法回答这个问题，请稍后再试。'
        messages.push({ role: 'ai', content: finalText })
        streamBuffer = ''
        streamSegments.value = []
        streamMeta.hasRealData = false
        isLoading.value = false
        scrollToBottom()
      }
    },
    // history：将已有消息列表作为上下文传入
    (messages as AiMessage[]).slice(0, -1).flatMap(m => [m.content])
  )
}

/**
 * 点击导航卡片跳转
 */
function navigateTo(item: NavigationAction): void {
  router.push(item.path)
}

/**
 * 导航卡片图标
 */
function renderMarkdown(text: string): string {
  return marked.parse(text, { async: false }) as string
}

function navIcon(type: string): string {
  const icons: Record<string, string> = {
    post: '📝',
    circle: '💬',
    user: '👤',
    search: '🔍',
    page: '📄'
  }
  return icons[type] || '🔗'
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

  /** 实时数据标记 */
  &__data-badge {
    display: inline-block;
    font-size: 11px;
    color: $mint-600;
    background: rgba(78, 205, 196, 0.1);
    padding: 1px 8px;
    border-radius: 10px;
    margin-bottom: 6px;
    font-weight: 600;
  }

  /** Markdown 渲染文本 */
  &__text {
    line-height: 1.6;

    p {
      margin: 0 0 6px;
      &:last-child { margin-bottom: 0; }
    }

    ul, ol {
      margin: 4px 0;
      padding-left: 18px;
    }

    li {
      margin-bottom: 2px;
    }

    strong { font-weight: 700; }
    em { font-style: italic; }

    code {
      font-family: 'Cascadia Code', 'Fira Code', monospace;
      font-size: 12px;
      background: rgba(0,0,0,0.06);
      padding: 1px 5px;
      border-radius: 3px;
    }

    pre {
      margin: 6px 0;
      padding: 8px 12px;
      background: rgba(0,0,0,0.04);
      border-radius: 6px;
      overflow-x: auto;

      code {
        background: none;
        padding: 0;
      }
    }

    a {
      color: $mint-600;
      text-decoration: underline;
    }

    blockquote {
      margin: 6px 0;
      padding-left: 10px;
      border-left: 3px solid $mint-300;
      color: $ink-500;
    }
  }

  /** 导航卡片 */
  &__nav-chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px 4px 6px;
    margin: 2px 2px;
    background: rgba(78, 205, 196, 0.08);
    border: 1px solid rgba(78, 205, 196, 0.18);
    border-radius: 6px;
    cursor: pointer;
    font-size: 12px;
    line-height: 1.4;
    transition: all 0.2s ease;
    vertical-align: baseline;

    &:hover {
      background: rgba(78, 205, 196, 0.15);
      border-color: rgba(78, 205, 196, 0.3);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
    }
  }

  &__nav-chip-icon {
    font-size: 14px;
    flex-shrink: 0;
  }

  &__nav-chip-label {
    font-weight: 600;
    color: $mint-700;
    white-space: nowrap;
  }

  &__nav-chip-preview {
    color: $ink-500;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__nav-chip-arrow {
    color: $mint-500;
    flex-shrink: 0;
  }

  /** 进度状态指示器 */
  &__status {
    display: flex;
    align-items: center;
    gap: 2px;
    padding: 6px 10px;
    margin-bottom: 6px;
    background: linear-gradient(135deg, rgba(78, 205, 196, 0.08), rgba(107, 203, 119, 0.08));
    border-radius: 8px;
    font-size: 12px;
    color: $mint-700;
  }

  &__status-text {
    font-weight: 500;
  }

  &__status-dots {
    display: inline-flex;
    gap: 1px;
    margin-left: 2px;

    .dot {
      animation: status-dot-bounce 1.4s ease-in-out infinite;
      font-size: 16px;
      line-height: 1;
      color: $mint-500;

      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }

  @keyframes status-dot-bounce {
    0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
    40% { opacity: 1; transform: translateY(-2px); }
  }

  /** 状态切换过渡动画（向上淡出 + 从下淡入） */
  :deep(.status-fade-leave-active),
  :deep(.status-fade-enter-active) {
    transition: all 0.25s ease;
  }
  :deep(.status-fade-enter-from) {
    opacity: 0;
    transform: translateY(8px);
  }
  :deep(.status-fade-leave-to) {
    opacity: 0;
    transform: translateY(-8px);
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
