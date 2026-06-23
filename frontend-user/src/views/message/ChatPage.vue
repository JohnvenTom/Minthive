<template>
  <div class="chat-page" :class="{ 'page-shake': targetUserStatus === 0 }">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" />

    <!-- 顶部导航 -->
    <header class="chat-header">
      <div class="header-inner">
        <button class="back-btn" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="chat-user-info">
          <div class="chat-user-avatar">
            <img :src="targetUser.avatar || defaultAvatar" :alt="targetUser.nickname" />
          </div>
          <span class="chat-user-name font-heading">{{ targetUser.nickname }}</span>
          <span v-if="wsConnected" class="online-dot" />
        </div>
        <div class="header-spacer" />
      </div>
    </header>

    <!-- 聊天消息列表 -->
    <div ref="messageListRef" class="message-list" @scroll="onScroll">
      <!-- 封禁印章（对方被封禁时显示） -->
      <div v-if="targetUserStatus === 0" class="banned-stamp-circle">
        <svg class="banned-svg" viewBox="0 0 240 240" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <filter id="stamp-grunge-chat" x="-5%" y="-5%" width="110%" height="110%">
              <feTurbulence type="fractalNoise" baseFrequency="0.04" numOctaves="2" result="noise" />
              <feDisplacementMap in="SourceGraphic" in2="noise" scale="1" xChannelSelector="R" yChannelSelector="G" />
            </filter>
          </defs>

          <circle cx="120" cy="120" r="110" fill="none"
                  stroke="#dc2626" stroke-width="5"
                  opacity="0.85" />

          <circle cx="120" cy="120" r="100" fill="none"
                  stroke="#dc2626" stroke-width="1.5"
                  opacity="0.65" />

          <path id="topArcChat" d="M 30,120 A 90,90 0 0,1 210,120" fill="none" />
          <text font-family="'Impact','Arial Black',sans-serif" font-size="24"
                font-weight="900" letter-spacing="8" fill="#dc2626"
                filter="url(#stamp-grunge-chat)">
            <textPath href="#topArcChat" startOffset="50%" text-anchor="middle">BANNED</textPath>
          </text>

          <path id="bottomArcChat" d="M 35,138 A 85,85 0 0,0 205,138" fill="none" />
          <text font-family="'Impact','Arial Black',sans-serif" font-size="18"
                font-weight="900" letter-spacing="6" fill="#dc2626"
                filter="url(#stamp-grunge-chat)" opacity="0.75">
            <textPath href="#bottomArcChat" startOffset="50%" text-anchor="middle">BANNED</textPath>
          </text>

          <circle cx="12" cy="120" r="4.5" fill="#dc2626" opacity="0.7" />
          <circle cx="228" cy="120" r="4.5" fill="#dc2626" opacity="0.7" />
          <circle cx="45" cy="148" r="3" fill="#dc2626" opacity="0.5" />
          <circle cx="195" cy="148" r="3" fill="#dc2626" opacity="0.5" />

          <circle cx="120" cy="120" r="72" fill="none"
                  stroke="#dc2626" stroke-width="1" opacity="0.35" />

          <g transform="rotate(-18, 120, 120)">
            <rect x="28" y="96" width="184" height="48" rx="6" ry="6"
                  fill="none"
                  stroke="#dc2626" stroke-width="3.5" />
            <rect x="34" y="102" width="172" height="36" rx="4" ry="4"
                  fill="none" stroke="#dc2626" stroke-width="1"
                  opacity="0.5" />
            <text x="120" y="128" text-anchor="middle"
                  font-family="'Impact','Arial Black',sans-serif"
                  font-size="32" font-weight="900" letter-spacing="4"
                  fill="#dc2626" filter="url(#stamp-grunge-chat)">BANNED</text>
          </g>

          <g fill="#dc2626" opacity="0.15">
            <circle cx="55" cy="58" r="1.5" />
            <circle cx="180" cy="62" r="1" />
            <circle cx="150" cy="185" r="1.5" />
            <circle cx="88" cy="178" r="1" />
            <circle cx="118" cy="45" r="1.2" />
            <circle cx="195" cy="155" r="0.8" />
            <circle cx="42" cy="145" r="1.2" />
          </g>
        </svg>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="load-more-area">
        <LoadingSpinner v-if="loadingMore" size="small" />
        <button v-else class="load-more-btn" @click="loadMore">加载更多</button>
      </div>

      <!-- 消息列表 -->
      <TransitionGroup name="msg-anim">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['msg-row', { 'is-self': msg.fromUserId === currentUserId }]"
        >
          <!-- 对方消息 -->
          <template v-if="msg.fromUserId !== currentUserId">
            <div class="msg-avatar">
              <img :src="targetUser.avatar || defaultAvatar" :alt="targetUser.nickname" />
            </div>
            <div class="msg-bubble-wrap">
              <!-- AI代回复标记 -->
              <div v-if="msg.aiReply" class="ai-reply-tag">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                  <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="currentColor"/>
                </svg>
                <span>AI代回复</span>
              </div>
              <!-- 文字消息 -->
              <div v-if="msg.type === 'text'" class="msg-bubble other">{{ msg.content }}</div>
              <!-- 图片消息 -->
              <div v-else-if="msg.type === 'image'" class="msg-bubble other msg-image" @click="previewImage(msg.content)">
                <img :src="msg.content" alt="图片" loading="lazy" />
              </div>
              <!-- 表情消息 -->
              <div v-else-if="msg.type === 'emoji'" class="msg-bubble other msg-emoji">{{ msg.content }}</div>
              <!-- 时间 -->
              <span class="msg-time">{{ formatRelativeTime(msg.createTime) }}</span>
            </div>
          </template>

          <!-- 自己的消息：气泡在左，头像贴右边缘 -->
          <template v-else>
            <div class="msg-bubble-wrap is-self">
              <!-- AI代回复标记 -->
              <div v-if="msg.aiReply" class="ai-reply-tag self">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                  <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="currentColor"/>
                </svg>
                <span>AI代回复</span>
                <button class="revoke-btn" @click.stop="onRevokeAi(msg.id)">撤回</button>
              </div>
              <!-- 文字消息 -->
              <div v-if="msg.type === 'text'" class="msg-bubble self">{{ msg.content }}</div>
              <!-- 图片消息 -->
              <div v-else-if="msg.type === 'image'" class="msg-bubble self msg-image" @click="previewImage(msg.content)">
                <img :src="msg.content" alt="图片" loading="lazy" />
              </div>
              <!-- 表情消息 -->
              <div v-else-if="msg.type === 'emoji'" class="msg-bubble self msg-emoji">{{ msg.content }}</div>
              <!-- 已读状态 -->
              <span class="msg-read-status">{{ msg.read ? '已读' : '未读' }}</span>
              <!-- 时间 -->
              <span class="msg-time">{{ formatRelativeTime(msg.createTime) }}</span>
            </div>
            <div class="msg-avatar self">
              <img :src="currentUserAvatar" alt="我" />
            </div>
          </template>
        </div>
      </TransitionGroup>

      <!-- 空状态 -->
      <EmptyState
        v-if="!loading && messages.length === 0"
        title="开始聊天吧"
        description="发送第一条消息"
        icon="chat-o"
      />
    </div>

    <!-- 底部输入栏 -->
    <footer class="chat-footer glass-card" :class="{ 'footer-banned': targetUserStatus === 0 }">
      <div class="input-area">
        <!-- 图片按钮 -->
        <button class="tool-btn" :disabled="targetUserStatus === 0" @click="triggerImageUpload">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.8"/>
            <circle cx="8.5" cy="8.5" r="1.5" fill="currentColor"/>
            <path d="M21 15l-5-5L5 21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          class="hidden-input"
          @change="onImageSelected"
        />

        <!-- 输入框 -->
        <div class="input-wrap">
          <input
            v-model="inputText"
            type="text"
            class="msg-input"
            :placeholder="targetUserStatus === 0 ? '对方已被封禁，无法发送消息' : '输入消息...'"
            :disabled="targetUserStatus === 0"
            @keydown.enter="onSend"
          />
        </div>

        <!-- 发送按钮 -->
        <button
          :class="['send-btn', { active: inputText.trim() && targetUserStatus !== 0 }]"
          :disabled="!inputText.trim() || sending || targetUserStatus === 0"
          @click="onSend"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>

        <!-- AI智能回复按钮 -->
        <button
          :class="['ai-btn', { loading: aiReplying }]"
          :disabled="aiReplying || targetUserStatus === 0"
          @click="onAiReply"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="currentColor"/>
          </svg>
          <span class="ai-label">{{ aiReplying ? '思考中...' : 'AI' }}</span>
        </button>
      </div>
    </footer>

    <!-- 图片预览 -->
    <Teleport to="body">
      <Transition name="preview-fade">
        <div v-if="previewVisible" class="image-preview" @click="closePreview">
          <img :src="previewUrl" alt="预览" />
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/**
 * 私信聊天页面
 * @description 与指定用户的实时聊天界面，支持文字/图片/表情消息，
 *   AI代回复（琥珀色标识，可撤回），WebSocket实时收发，消息已读状态
 */

import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { getMessages, sendMessage as sendMessageApi, revokeAiMessage, markRead } from '@/api/message'
import { aiReplyMessage } from '@/api/ai'
import { uploadImage } from '@/api/file'
import { getUserProfile } from '@/api/user'
import { wsClient } from '@/utils/websocket'
import { formatRelativeTime } from '@/utils/format'
import type { Message } from '@/types'

// ---------- 路由与Store ----------
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()

// ---------- 默认头像 ----------
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>')

// ---------- 响应式数据 ----------
const messages = ref<Message[]>([])
const inputText = ref('')
const sending = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const aiReplying = ref(false)
const currentPage = ref(1)
const hasMore = ref(true)
const previewVisible = ref(false)
const previewUrl = ref('')

/** DOM引用 */
const messageListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

/** 聊天对象用户ID */
const targetUserId = computed(() => Number(route.params.userId))

/** 当前用户ID */
const currentUserId = computed(() => userStore.userInfo?.id || 0)

/** 当前用户头像 */
const currentUserAvatar = computed(() => userStore.userInfo?.avatar || defaultAvatar)

/** WebSocket连接状态 */
const wsConnected = computed(() => chatStore.wsConnected)

/** 聊天对象信息 */
const targetUser = ref<{ nickname: string; avatar: string }>({
  nickname: '用户',
  avatar: defaultAvatar
})

/** 聊天对象账号状态（0=封禁 1=正常） */
const targetUserStatus = ref<number>(1)

// ---------- 数据加载 ----------

/**
 * 消息字段映射
 * @param {any} raw - 后端原始消息（msgType: 0/1/2, isRead: 0/1）
 * @returns {Message} 前端统一格式（type: text/image/emoji, read: boolean）
 * @description 兼容后端 Integer 字段与前端枚举/布尔字段
 */
function normalizeMessage(raw: any): Message {
  const typeMap = ['text', 'image', 'emoji'] // 后端 0文字 1图片 2表情
  return {
    id: raw.id,
    fromUserId: raw.fromUserId,
    fromNickname: raw.fromNickname || '',
    fromAvatar: raw.fromAvatar || '',
    toUserId: raw.toUserId,
    content: raw.content,
    type: typeof raw.type === 'string' ? raw.type : (typeMap[raw.msgType] || 'text'),
    read: raw.read ?? (raw.isRead === 1),
    aiReply: raw.aiReply === true || raw.aiReply === 1,
    createTime: raw.createTime
  }
}

/**
 * 加载聊天消息列表
 * @param {boolean} isRefresh - 是否为刷新操作
 * @returns {Promise<void>}
 * @description 从服务端获取与当前聊天对象的历史消息；
 *   后端按时间降序返回（方便分页），前端反转后按时间正序渲染（旧→新）
 */
async function fetchMessages(isRefresh = false): Promise<void> {
  if (isRefresh) {
    currentPage.value = 1
    hasMore.value = true
  }
  loading.value = true
  try {
    const res = await getMessages(targetUserId.value, currentPage.value, 30)
    // 兼容后端返回：可能是数组（List<Message>），也可能是分页对象（含 list 字段）
    const data = res.data
    let list: Message[] = (Array.isArray(data) ? data : (data?.list || [])).map(normalizeMessage)
    // 后端降序返回（最新在前），反转后变为正序（旧消息在前，新消息在后）
    list = list.reverse()
    if (isRefresh || currentPage.value === 1) {
      messages.value = list
    } else {
      messages.value = [...list, ...messages.value]
    }
    hasMore.value = Array.isArray(data) ? list.length >= 30 : (data?.hasMore ?? false)
    await nextTick()
    scrollToBottom()
  } catch {
    showToast('加载消息失败')
  } finally {
    loading.value = false
  }
}

/**
 * 加载更多历史消息
 * @returns {Promise<void>}
 * @description 向上翻页加载更早的消息，反转后插入列表顶部
 */
async function loadMore(): Promise<void> {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  currentPage.value++
  const prevHeight = messageListRef.value?.scrollHeight || 0
  try {
    const res = await getMessages(targetUserId.value, currentPage.value, 30)
    // 兼容后端返回：可能是数组（List<Message>），也可能是分页对象（含 list 字段）
    const data = res.data
    let list: Message[] = (Array.isArray(data) ? data : (data?.list || [])).map(normalizeMessage)
    // 反转：后端降序返回，前端需要正序（旧→新）插入顶部
    list = list.reverse()
    messages.value = [...list, ...messages.value]
    hasMore.value = Array.isArray(data) ? list.length >= 30 : (data?.hasMore ?? false)
    await nextTick()
    // 保持滚动位置
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight - prevHeight
    }
  } catch {
    showToast('加载失败')
  } finally {
    loadingMore.value = false
  }
}

// ---------- 消息发送 ----------

/**
 * 发送消息
 * @param {Event} [event] - 键盘事件（回车发送）
 * @returns {Promise<void>}
 * @description 发送文字消息，清空输入框并滚动到底部
 */
async function onSend(event?: Event): Promise<void> {
  const content = inputText.value.trim()
  if (!content || sending.value) return

  sending.value = true
  try {
    const msg = await chatStore.sendMessage(targetUserId.value, content, 'text')
    messages.value.push(normalizeMessage(msg))
    inputText.value = ''
    await nextTick()
    scrollToBottom()
  } catch {
    showToast('发送失败')
  } finally {
    sending.value = false
  }
}

/**
 * 选择图片并发送
 * @param {Event} event - 文件选择事件
 * @returns {Promise<void>}
 * @description 上传图片后发送图片消息
 */
async function onImageSelected(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  try {
    showToast('上传中...')
    const res = await uploadImage(file)
    const url = res.data?.url
    if (url) {
      const msg = await chatStore.sendMessage(targetUserId.value, url, 'image')
      messages.value.push(normalizeMessage(msg))
      await nextTick()
      scrollToBottom()
    }
  } catch {
    showToast('图片发送失败')
  } finally {
    input.value = ''
  }
}

/**
 * 触发图片上传
 * @returns {void}
 */
function triggerImageUpload(): void {
  fileInputRef.value?.click()
}

/**
 * AI智能回复
 * @returns {Promise<void>}
 * @description 基于当前聊天上下文（含输入框待发送内容）调用AI生成代回复，
 *   使用真实昵称做角色分离，确保AI清楚区分发送方与接收方
 */
async function onAiReply(): Promise<void> {
  if (aiReplying.value) return

  // 如果输入框为空，提示用户先输入内容
  const pendingContent = inputText.value.trim()
  if (!pendingContent) {
    showToast('请先在输入框输入要发送的内容')
    return
  }

  aiReplying.value = true
  try {
    /**
     * 构建带角色分离的聊天上下文
     *
     * 系统提示词：明确告知AI当前用户的身份、对方身份和行为约束，
     * 避免AI混淆发送方与接收方
     */
    const myNickname = userStore.userInfo?.nickname || '我'
    const peerNickname = targetUser.value.nickname || '对方'

    const systemPrompt = `你是${myNickname}的聊天助手。请根据以下聊天记录，以${myNickname}的口吻生成一条回复给${peerNickname}。
要求：
- 保持自然、友好的语气，符合社交聊天的风格
- 回复内容应与上下文相关联，不要重复说过的话
- 只输出回复正文，不要加任何前缀或说明文字`

    // 取最近10条历史消息，用真实昵称标注角色
    const historyContext = messages.value
      .slice(-10)
      .map(m => {
        const role = m.fromUserId === currentUserId.value ? myNickname : peerNickname
        return `${role}: ${m.content}`
      })
      .join('\n')

    // 将输入框中的待发送内容作为"我"的最新一条消息加入上下文
    const fullContext = `${systemPrompt}\n\n【聊天记录】\n${historyContext}\n${myNickname}: ${pendingContent}`

    const res = await aiReplyMessage(targetUserId.value, fullContext)
    const content = res.data?.content
    if (content) {
      // 先发送用户输入框中的原始消息
      const userMsg = await chatStore.sendMessage(targetUserId.value, pendingContent, 'text')
      messages.value.push(normalizeMessage(userMsg))
      inputText.value = ''

      // 再发送AI生成的回复（标记为aiReply）
      const aiMsg = await chatStore.sendMessage(targetUserId.value, content, 'text')
      messages.value.push({ ...normalizeMessage(aiMsg), aiReply: true })
      await nextTick()
      scrollToBottom()
    }
  } catch {
    showToast('AI回复失败')
  } finally {
    aiReplying.value = false
  }
}

/**
 * 撤回AI代回复消息
 * @param {number} msgId - 消息ID
 * @returns {Promise<void>}
 * @description 撤回AI代回复的消息，从列表中移除
 */
async function onRevokeAi(msgId: number): Promise<void> {
  try {
    await revokeAiMessage(msgId)
    messages.value = messages.value.filter(m => m.id !== msgId)
    showToast('已撤回')
  } catch {
    showToast('撤回失败')
  }
}

// ---------- 图片预览 ----------

/**
 * 预览图片
 * @param {string} url - 图片URL
 * @returns {void}
 */
function previewImage(url: string): void {
  previewUrl.value = url
  previewVisible.value = true
}

/**
 * 关闭图片预览
 * @returns {void}
 */
function closePreview(): void {
  previewVisible.value = false
}

// ---------- 滚动与导航 ----------

/**
 * 滚动到底部
 * @returns {void}
 */
function scrollToBottom(): void {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

/**
 * 滚动事件处理
 * @returns {void}
 * @description 检测滚动到顶部触发加载更多
 */
function onScroll(): void {
  if (!messageListRef.value) return
  if (messageListRef.value.scrollTop < 100 && hasMore.value && !loadingMore.value) {
    loadMore()
  }
}

/**
 * 返回上一页
 * @returns {void}
 */
function goBack(): void {
  chatStore.setCurrentChat(null)
  router.back()
}

// ---------- WebSocket 实时收发 ----------

/**
 * WebSocket 新消息推送处理
 * @param {any} data - 推送数据
 * @description 收到新消息时添加到列表并滚动到底部
 */
function onWsMessage(data: any): void {
  if (data?.fromUserId === targetUserId.value) {
    messages.value.push(normalizeMessage(data))
    nextTick(() => scrollToBottom())
    // 标记已读
    markRead(targetUserId.value)
  }
}

// ---------- 生命周期 ----------
onMounted(async () => {
  // 确保当前用户信息已加载（currentUserId 依赖它判断"自己的消息"）
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUser()
    } catch {
      // 静默处理，拦截器会跳转登录
    }
  }

  // 从chatStore获取聊天对象信息；若为空（如从个人主页直接进入），则通过API获取
  if (chatStore.currentChat) {
    targetUser.value = {
      nickname: chatStore.currentChat.nickname,
      avatar: chatStore.currentChat.avatar
    }
  } else {
    /**
     * 获取聊天对象用户信息
     * @description 当 chatStore 中无当前聊天对象时（例如从个人主页点击"私信"进入），
     *   通过 getUserProfile 接口获取目标用户的头像和昵称，并同步设置到 chatStore
     */
    try {
      const res = await getUserProfile(targetUserId.value)
      if (res.data) {
        targetUser.value = {
          nickname: res.data.nickname || '用户',
          avatar: res.data.avatar || defaultAvatar
        }
        // 同步获取对方账号状态（封禁检测）
        targetUserStatus.value = res.data.status ?? 1
        // 同步到 chatStore，确保后续 WebSocket 消息等场景能正确引用
        chatStore.setCurrentChat({
          userId: targetUserId.value,
          nickname: targetUser.value.nickname,
          avatar: targetUser.value.avatar,
          lastMessage: '',
          lastTime: '',
          unread: 0
        })
      }
    } catch {
      // 获取失败时保持默认值，静默处理
    }
  }

  await fetchMessages(true)
  markRead(targetUserId.value)

  chatStore.connectWs()
  wsClient.on('message', onWsMessage)
})

onUnmounted(() => {
  wsClient.off('message', onWsMessage)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;

  // 桌面端（>768px）：减去 App.vue 顶部导航栏 64px
  @media (min-width: 769px) {
    height: calc(100vh - 64px);
  }

  background: $ink-50;
  position: relative;
  overflow: hidden;
}

// ---------- 蜂巢装饰背景 ----------
.hex-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  @include honeycomb-bg(rgba(78, 205, 196, 0.03), 28px);
  pointer-events: none;
  z-index: 0;
}

// ---------- 顶部导航 ----------
.chat-header {
  position: relative;
  z-index: 100;
  @include glass(20px, rgba(255, 255, 255, 0.88));
  border-bottom: 1px solid rgba(78, 205, 196, 0.1);

  .header-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-3 $space-4;
    max-width: 800px;
    margin: 0 auto;
  }

  .back-btn {
    background: none;
    border: none;
    color: $ink-700;
    cursor: pointer;
    padding: $space-1;
    border-radius: $radius-xs;
    transition: background $dur-fast ease;

    &:hover {
      background: $ink-50;
    }
  }

  .chat-user-info {
    display: flex;
    align-items: center;
    gap: $space-2;
  }

  .chat-user-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .chat-user-name {
    font-size: 16px;
    font-weight: 600;
    color: $ink-900;
  }

  .online-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: $leaf-500;
    animation: hex-breathe 2s ease-in-out infinite;
  }

  .header-spacer {
    width: 32px;
  }
}

// ---------- 消息列表 ----------
.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: $space-3 $space-4;
  position: relative;
  z-index: 1;
  @include custom-scrollbar;

  @include mobile {
    padding: $space-2 $space-3;
  }
}

.load-more-area {
  text-align: center;
  padding: $space-3 0;
}

.load-more-btn {
  font-size: 13px;
  color: $mint-600;
  background: none;
  border: none;
  cursor: pointer;
  padding: $space-2 $space-4;
  border-radius: $radius-pill;
  transition: background $dur-fast ease;

  &:hover {
    background: $mint-50;
  }
}

// ---------- 消息行 ----------
.msg-row {
  display: flex;
  align-items: flex-start;
  gap: $space-2;
  margin-bottom: $space-3;
  animation: fade-up 0.35s $ease-out both;

  &.is-self {
    justify-content: flex-end; /* 自己的消息整行靠右 */
  }
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  margin-top: 4px;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.msg-bubble-wrap {
  max-width: 70%;
  display: flex;
  flex-direction: column;

  &.is-self {
    align-items: flex-end;
  }
}

// ---------- 气泡样式 ----------
.msg-bubble {
  padding: $space-2 $space-3;
  border-radius: $radius-md;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  position: relative;

  &.other {
    background: #fff;
    color: $ink-700;
    border-bottom-left-radius: $radius-xs;
    box-shadow: $shadow-xs;
  }

  &.self {
    background: $grad-mint;
    color: #fff;
    border-bottom-right-radius: $radius-xs;
  }

  &.msg-image {
    padding: 4px;
    background: transparent;
    box-shadow: none;
    cursor: zoom-in;

    img {
      max-width: 200px;
      max-height: 200px;
      border-radius: $radius-sm;
      object-fit: cover;
    }

    &.self {
      background: transparent;
    }
  }

  &.msg-emoji {
    background: transparent;
    box-shadow: none;
    font-size: 36px;
    padding: $space-1;

    &.self {
      background: transparent;
    }
  }
}

// ---------- AI代回复标记 ----------
.ai-reply-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: $radius-pill;
  background: rgba(255, 182, 39, 0.12);
  color: $amber-500;
  font-size: 11px;
  font-weight: 500;
  margin-bottom: 4px;
  width: fit-content;

  &.self {
    margin-left: auto;
  }

  svg {
    animation: hex-breathe 2s ease-in-out infinite;
  }

  .revoke-btn {
    background: none;
    border: none;
    color: $amber-600;
    font-size: 11px;
    cursor: pointer;
    text-decoration: underline;
    padding: 0;
    margin-left: 4px;

    &:hover {
      color: $amber-700;
    }
  }
}

// ---------- 消息时间与状态 ----------
.msg-time {
  font-size: 11px;
  color: $ink-300;
  margin-top: 4px;
}

.msg-read-status {
  font-size: 11px;
  color: $ink-300;
  margin-top: 2px;
}

// ---------- 封禁圆形印章（经典双层圆形印章） ----------
.banned-stamp-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 20;
  pointer-events: none;
  overflow: visible;

  .banned-svg {
    width: 200px;
    height: 200px;
    animation: stamp-pop 0.5s $ease-spring both,
               stamp-breathe-chat 4s ease-in-out 0.5s infinite;
  }
}

@keyframes stamp-pop {
  0% { opacity: 0; transform: scale(0) rotate(-30deg); }
  40% { opacity: 1; transform: scale(1.15) rotate(-4deg); }
  55% { opacity: 1; transform: scale(1.12) rotate(-6deg); }
  100% { opacity: 1; transform: scale(1) rotate(0deg); }
}

@keyframes stamp-breathe-chat {
  0%, 100% { filter: brightness(1) drop-shadow(0 6px 20px rgba(220, 38, 38, 0.2)); }
  50% { filter: brightness(1.06) drop-shadow(0 10px 36px rgba(220, 38, 38, 0.3)); }
}

@keyframes screen-shake {
  0%, 100% { transform: translate(0, 0); }
  10% { transform: translate(-4px, -2px); }
  20% { transform: translate(4px, 2px); }
  30% { transform: translate(-3px, 1px); }
  40% { transform: translate(3px, -1px); }
  50% { transform: translate(-2px, 2px); }
  60% { transform: translate(2px, -2px); }
  70% { transform: translate(-1px, 1px); }
  80% { transform: translate(1px, -1px); }
  90% { transform: translate(0, 0); }
}

.page-shake {
  animation: screen-shake 0.45s ease-out 0.2s both;
}

// ---------- 底部输入栏 ----------
.chat-footer {
  position: relative;
  z-index: 100;
  flex-shrink: 0;
  border-top: 1px solid $ink-100;
  padding: $space-2 $space-3;
  border-radius: 0;
  transition: opacity $dur-base ease, filter $dur-base ease;

  // 封禁状态：整体置灰
  &.footer-banned {
    opacity: 0.5;
    filter: grayscale(0.6);
    pointer-events: none;
  }
}

.input-area {
  display: flex;
  align-items: center;
  gap: $space-2;
  max-width: 800px;
  margin: 0 auto;
}

.tool-btn {
  background: none;
  border: none;
  color: $ink-500;
  cursor: pointer;
  padding: $space-2;
  border-radius: $radius-sm;
  transition: color $dur-fast ease, background $dur-fast ease;
  flex-shrink: 0;

  &:hover {
    color: $mint-500;
    background: $mint-50;
  }
}

.hidden-input {
  display: none;
}

.input-wrap {
  flex: 1;
  min-width: 0;
}

.msg-input {
  width: 100%;
  padding: $space-2 $space-3;
  border: 1px solid $ink-100;
  border-radius: $radius-pill;
  font-size: 14px;
  color: $ink-700;
  background: rgba(255, 255, 255, 0.6);
  outline: none;
  transition: border-color $dur-fast ease, box-shadow $dur-fast ease;

  &::placeholder {
    color: $ink-300;
  }

  &:focus {
    border-color: $mint-500;
    box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.15);
  }
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: $ink-100;
  color: $ink-300;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all $dur-fast $ease-out;
  flex-shrink: 0;

  &.active {
    background: $grad-mint;
    color: #fff;
    box-shadow: $shadow-glow-mint;
    animation: ai-glow 3s ease-in-out infinite;
  }

  &:disabled {
    cursor: not-allowed;
  }
}

.ai-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: $space-2 $space-3;
  border-radius: $radius-pill;
  border: 1px solid $amber-500;
  background: rgba(255, 182, 39, 0.08);
  color: $amber-500;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all $dur-fast $ease-out;
  flex-shrink: 0;
  white-space: nowrap;

  &:hover:not(.loading) {
    background: rgba(255, 182, 39, 0.15);
    box-shadow: $shadow-glow-amber;
  }

  &.loading {
    opacity: 0.7;
    cursor: not-allowed;

    svg {
      animation: hex-spin 1.5s linear infinite;
    }
  }

  .ai-label {
    font-family: $font-heading;
  }
}

// ---------- 消息动画 ----------
.msg-anim-enter-active {
  animation: fade-up 0.3s $ease-out both;
}

// ---------- 图片预览 ----------
.image-preview {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(26, 29, 46, 0.92);
  @include center;
  cursor: zoom-out;

  img {
    max-width: 90vw;
    max-height: 90vh;
    object-fit: contain;
    border-radius: $radius-sm;
  }
}

.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: opacity $dur-base $ease-out;
}
.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}

// ---------- 响应式 ----------
@include mobile {
  .banned-stamp-circle {
    .banned-svg {
      width: 155px;
      height: 155px;
    }
  }

  .chat-header .header-inner {
    padding: $space-2 $space-3;
  }

  .msg-bubble-wrap {
    max-width: 80%;
  }

  .ai-btn .ai-label {
    display: none;
  }
}
</style>
