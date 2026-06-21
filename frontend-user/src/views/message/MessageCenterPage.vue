<template>
  <div class="message-center">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" />

    <!-- 顶部导航 -->
    <header class="msg-header">
      <div class="header-inner">
        <button class="back-btn" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <h1 class="page-title font-heading">消息中心</h1>
        <div class="header-right">
          <span v-if="totalUnread > 0" class="unread-badge">{{ totalUnread }}</span>
        </div>
      </div>
    </header>

    <!-- Tab 切换 -->
    <nav class="msg-tabs">
      <div class="tabs-inner">
        <button
          :class="['tab-btn', { active: activeTab === 'chat' }]"
          @click="switchTab('chat')"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>私信</span>
          <span v-if="unreadData.message > 0" class="tab-dot" />
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'notify' }]"
          @click="switchTab('notify')"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>通知</span>
          <span v-if="notifyUnread > 0" class="tab-dot" />
        </button>
      </div>
    </nav>

    <!-- 私信列表 -->
    <div v-show="activeTab === 'chat'" class="tab-content">
      <!-- 加载中 -->
      <div v-if="loading && chatList.length === 0" class="loading-wrap">
        <LoadingSpinner />
      </div>

      <!-- 私信列表 -->
      <div v-else-if="chatList.length > 0" class="chat-list">
        <TransitionGroup name="list-anim">
          <div
            v-for="(chat, idx) in chatList"
            :key="chat.userId"
            class="chat-item glass-card"
            :style="{ animationDelay: `${idx * 0.05}s` }"
            @click="openChat(chat)"
          >
            <!-- 六边形头像 -->
            <div class="chat-avatar">
              <div class="hex-avatar">
                <img :src="chat.avatar || defaultAvatar" :alt="chat.nickname" />
              </div>
              <span v-if="chat.unread > 0" class="unread-dot">{{ chat.unread > 99 ? '99+' : chat.unread }}</span>
            </div>

            <!-- 消息信息 -->
            <div class="chat-info">
              <div class="chat-top">
                <span class="chat-name font-heading">{{ chat.nickname }}</span>
                <span class="chat-time">{{ formatRelativeTime(chat.lastTime) }}</span>
              </div>
              <p class="chat-preview">{{ chat.lastMessage }}</p>
            </div>
          </div>
        </TransitionGroup>
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else-if="!loading"
        title="暂无私信"
        description="去关注感兴趣的人，开始聊天吧"
        icon="chat-o"
      />
    </div>

    <!-- 通知列表 -->
    <div v-show="activeTab === 'notify'" class="tab-content">
      <!-- 通知类型筛选 -->
      <div class="notify-filter">
        <button
          v-for="filter in notifyFilters"
          :key="filter.value"
          :class="['filter-btn', { active: activeNotifyType === filter.value }]"
          @click="switchNotifyType(filter.value)"
        >
          <span class="filter-icon" v-html="filter.icon" />
          <span class="filter-label">{{ filter.label }}</span>
          <span v-if="filter.value && unreadData[filter.value as keyof typeof unreadData] > 0" class="filter-dot" />
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="notifyLoading && notifications.length === 0" class="loading-wrap">
        <LoadingSpinner />
      </div>

      <!-- 通知列表 -->
      <div v-else-if="notifications.length > 0" class="notify-list">
        <TransitionGroup name="list-anim">
          <div
            v-for="(notify, idx) in notifications"
            :key="notify.id"
            :class="['notify-item glass-card', { unread: !notify.read }]"
            :style="{ animationDelay: `${idx * 0.05}s` }"
            @click="onNotifyClick(notify)"
          >
            <!-- 通知类型图标 -->
            <div :class="['notify-type-icon', notify.type]">
              <svg v-if="notify.type === 'like'" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" fill="currentColor"/>
              </svg>
              <svg v-else-if="notify.type === 'comment'" width="20" height="20" viewBox="0 0 24 24" fill="none">
                 <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
               </svg>
               <svg v-else-if="notify.type === 'reply'" width="20" height="20" viewBox="0 0 24 24" fill="none">
                 <path d="M9 10L5 14l4 4M5 14h12a4 4 0 004-4V6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
               </svg>
              <svg v-else-if="notify.type === 'follow'" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M12.5 11a4 4 0 100-8 4 4 0 000 8zM20 8v6M23 11h-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <svg v-else-if="notify.type === 'circle'" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L22 8.5V15.5L12 22L2 15.5V8.5L12 2Z" stroke="currentColor" stroke-width="2"/>
              </svg>
              <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                <path d="M12 8v4M12 16h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>

            <!-- 通知内容 -->
            <div class="notify-content">
              <div class="notify-avatar">
                <img :src="notify.fromAvatar || defaultAvatar" :alt="notify.fromNickname" />
              </div>
              <div class="notify-text">
                <p class="notify-desc">
                  <span class="notify-name">{{ notify.fromNickname }}</span>
                  {{ getNotifyAction(notify.type) }}
                </p>
                <p v-if="notify.content" class="notify-extra">{{ notify.content }}</p>
                <span class="notify-time">{{ formatRelativeTime(notify.createTime) }}</span>
              </div>
            </div>

            <!-- 未读标记 -->
            <span v-if="!notify.read" class="unread-mark" />
          </div>
        </TransitionGroup>
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else-if="!notifyLoading"
        title="暂无通知"
        description="互动消息会在这里显示"
        icon="bell"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 消息通知中心页面
 * @description 展示私信会话列表和五类通知（点赞/评论/关注/圈子/系统），
 *   支持Tab切换、未读红点、WebSocket实时推送、通知类型筛选
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { useChatStore } from '@/stores/chat'
import { getNotifications, getUnreadCount } from '@/api/message'
import { wsClient } from '@/utils/websocket'
import { formatRelativeTime } from '@/utils/format'
import type { Notification } from '@/types'
import type { ChatItem } from '@/stores/chat'

// ---------- 路由与Store ----------
const router = useRouter()
const chatStore = useChatStore()

// ---------- 默认头像 ----------
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>')

// ---------- 响应式数据 ----------
const activeTab = ref<'chat' | 'notify'>('chat')
const loading = ref(false)
const notifyLoading = ref(false)
const chatList = ref<ChatItem[]>([])
const notifications = ref<Notification[]>([])
const activeNotifyType = ref<string>('')
const notifyPage = ref(1)
const notifyHasMore = ref(true)

/** 未读统计 */
const unreadData = ref<{
  like: number
  comment: number
  message: number
  circle: number
  system: number
}>({
  like: 0,
  comment: 0,
  message: 0,
  circle: 0,
  system: 0
})

// ---------- 通知类型筛选配置 ----------
const notifyFilters = [
  { label: '全部', value: '', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M4 6h16M4 12h16M4 18h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>' },
  { label: '点赞', value: 'like', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" fill="currentColor"/></svg>' },
  { label: '评论', value: 'comment', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke="currentColor" stroke-width="2"/></svg>' },
  { label: '关注', value: 'follow', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M20 8v6M23 11h-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>' },
  { label: '圈子', value: 'circle', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M12 2L22 8.5V15.5L12 22L2 15.5V8.5L12 2Z" stroke="currentColor" stroke-width="2"/></svg>' },
  { label: '系统', value: 'system', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/><path d="M12 8v4M12 16h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>' }
]

// ---------- 计算属性 ----------

/**
 * 通知未读总数
 * @returns {number} 点赞+评论+关注+圈子+系统未读数之和
 */
const notifyUnread = computed(() => {
  return unreadData.value.like + unreadData.value.comment +
    unreadData.value.follow + unreadData.value.circle + unreadData.value.system
})

/**
 * 全部未读总数
 * @returns {number} 私信未读+通知未读
 */
const totalUnread = computed(() => {
  return unreadData.value.message + notifyUnread.value
})

// ---------- 数据加载 ----------

/**
 * 加载私信会话列表
 * @returns {Promise<void>}
 * @description 从chatStore获取会话列表并更新本地状态
 */
async function fetchChatList(): Promise<void> {
  loading.value = true
  try {
    await chatStore.fetchChatList()
    chatList.value = chatStore.chatList
  } catch {
    showToast('加载会话失败')
  } finally {
    loading.value = false
  }
}

/**
 * 加载通知列表
 * @param {boolean} isRefresh - 是否为刷新操作
 * @returns {Promise<void>}
 * @description 根据当前通知类型筛选条件请求通知列表
 */
async function fetchNotifications(isRefresh = false): Promise<void> {
  notifyLoading.value = true
  if (isRefresh) {
    notifyPage.value = 1
    notifyHasMore.value = true
  }
  try {
    const res = await getNotifications(activeNotifyType.value || undefined, notifyPage.value, 20)
    const list = res.data?.list || []
    if (isRefresh || notifyPage.value === 1) {
      notifications.value = list
    } else {
      notifications.value.push(...list)
    }
    notifyHasMore.value = res.data?.hasMore ?? false
  } catch {
    showToast('加载通知失败')
  } finally {
    notifyLoading.value = false
  }
}

/**
 * 加载未读消息统计
 * @returns {Promise<void>}
 * @description 从服务端获取各类型未读数量
 */
async function fetchUnreadCount(): Promise<void> {
  try {
    const res = await getUnreadCount()
    if (res.data) {
      unreadData.value = res.data
    }
  } catch {
    // 静默失败
  }
}

// ---------- 交互操作 ----------

/**
 * 切换Tab
 * @param {'chat' | 'notify'} tab - 目标Tab
 * @returns {void}
 */
function switchTab(tab: 'chat' | 'notify'): void {
  if (activeTab.value === tab) return
  activeTab.value = tab
  if (tab === 'chat' && chatList.value.length === 0) {
    fetchChatList()
  }
  if (tab === 'notify' && notifications.value.length === 0) {
    fetchNotifications(true)
  }
}

/**
 * 切换通知类型筛选
 * @param {string} type - 通知类型（空字符串表示全部）
 * @returns {void}
 */
function switchNotifyType(type: string): void {
  if (activeNotifyType.value === type) return
  activeNotifyType.value = type
  fetchNotifications(true)
}

/**
 * 打开聊天页面
 * @param {ChatItem} chat - 聊天会话对象
 * @returns {void}
 * @description 跳转到与指定用户的聊天页面，并标记已读
 */
function openChat(chat: ChatItem): void {
  chatStore.setCurrentChat(chat)
  chatStore.markRead(chat.userId)
  router.push(`/chat/${chat.userId}`)
}

/**
 * 点击通知项
 * @param {Notification} notify - 通知对象
 * @returns {void}
 * @description 根据通知类型跳转到对应页面
 */
function onNotifyClick(notify: Notification): void {
  if (notify.type === 'like' || notify.type === 'comment' || notify.type === 'reply') {
    router.push(`/post/${notify.targetId}`)
  } else if (notify.type === 'follow') {
    router.push(`/profile/${notify.fromUserId}`)
  } else if (notify.type === 'circle') {
    router.push(`/circle/${notify.targetId}`)
  }
}

/**
 * 获取通知动作描述
 * @param {'like' | 'comment' | 'follow' | 'circle' | 'system'} type - 通知类型
 * @returns {string} 动作描述文本
 */
function getNotifyAction(type: 'like' | 'comment' | 'reply' | 'follow' | 'circle' | 'system'): string {
  const map: Record<string, string> = {
    like: '赞了你的动态',
    comment: '评论了你的动态',
    reply: '回复了你',
    follow: '关注了你',
    circle: '圈子有新动态',
    system: '系统通知'
  }
  return map[type] || '通知'
}

/**
 * 返回上一页
 * @returns {void}
 */
function goBack(): void {
  router.back()
}

// ---------- WebSocket 实时推送 ----------

/**
 * WebSocket 新消息推送处理
 * @param {any} data - 推送数据
 * @description 收到新私信时更新会话列表和未读计数
 */
function onWsMessage(data: any): void {
  if (activeTab.value === 'chat') {
    fetchChatList()
  }
  fetchUnreadCount()
}

/**
 * WebSocket 新通知推送处理
 * @param {any} data - 推送数据
 * @description 收到新通知时更新通知列表和未读计数
 */
function onWsNotify(data: any): void {
  if (activeTab.value === 'notify') {
    fetchNotifications(true)
  }
  fetchUnreadCount()
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchChatList()
  fetchUnreadCount()
  chatStore.connectWs()
  wsClient.on('message', onWsMessage)
  wsClient.on('notice', onWsNotify)
  wsClient.on('reply', onWsNotify)
  wsClient.on('comment', onWsNotify)
})

onUnmounted(() => {
  wsClient.off('message', onWsMessage)
  wsClient.off('notice', onWsNotify)
  wsClient.off('reply', onWsNotify)
  wsClient.off('comment', onWsNotify)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.message-center {
  min-height: 100vh;
  background: $ink-50;
  position: relative;
  padding-bottom: 80px;
}

// ---------- 蜂巢装饰背景 ----------
.hex-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  @include honeycomb-bg(rgba(78, 205, 196, 0.04), 32px);
  pointer-events: none;
  z-index: 0;
}

// ---------- 顶部导航 ----------
.msg-header {
  position: sticky;
  top: 0;
  z-index: 100;
  @include glass(20px, rgba(255, 255, 255, 0.85));

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

  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: $ink-900;
  }

  .header-right {
    width: 32px;
    display: flex;
    justify-content: flex-end;
  }

  .unread-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 20px;
    height: 20px;
    padding: 0 6px;
    border-radius: $radius-pill;
    background: $coral-500;
    color: #fff;
    font-size: 11px;
    font-weight: 600;
    font-family: $font-heading;
  }
}

// ---------- Tab 切换 ----------
.msg-tabs {
  position: sticky;
  top: 56px;
  z-index: 99;
  @include glass(16px, rgba(255, 255, 255, 0.8));
  border-bottom: 1px solid $ink-100;

  .tabs-inner {
    display: flex;
    max-width: 800px;
    margin: 0 auto;
    padding: 0 $space-4;
    gap: $space-8;
  }
}

.tab-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-3 0;
  font-size: 15px;
  font-weight: 500;
  color: $ink-500;
  background: none;
  border: none;
  cursor: pointer;
  transition: color $dur-fast $ease-out;

  &.active {
    color: $ink-900;
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 28px;
      height: 3px;
      border-radius: 2px;
      background: $grad-mint;
      animation: scale-in 0.3s $ease-spring both;
    }
  }

  &:hover {
    color: $mint-600;
  }

  svg {
    flex-shrink: 0;
  }
}

.tab-dot {
  position: absolute;
  top: 8px;
  right: -6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $coral-500;
  animation: hex-breathe 2s ease-in-out infinite;
}

// ---------- 内容区 ----------
.tab-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
  padding: $space-3 $space-4;
}

.loading-wrap {
  @include center;
  padding: $space-12 0;
}

// ---------- 私信列表 ----------
.chat-list {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-4;
  border-radius: $radius-md;
  cursor: pointer;
  transition: transform $dur-fast $ease-out, box-shadow $dur-base $ease-out;
  animation: fade-up 0.5s $ease-out both;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-sm;
  }

  &:active {
    transform: scale(0.98);
  }
}

.chat-avatar {
  position: relative;
  flex-shrink: 0;

  .hex-avatar {
    @include hexagon(48px);
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .unread-dot {
    position: absolute;
    top: -4px;
    right: -4px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: $radius-pill;
    background: $coral-500;
    color: #fff;
    font-size: 10px;
    font-weight: 600;
    font-family: $font-heading;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid #fff;
  }
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.chat-name {
  font-size: 15px;
  font-weight: 600;
  color: $ink-700;
}

.chat-time {
  font-size: 12px;
  color: $ink-300;
  flex-shrink: 0;
}

.chat-preview {
  font-size: 13px;
  color: $ink-500;
  @include ellipsis-1;
}

// ---------- 通知筛选 ----------
.notify-filter {
  display: flex;
  gap: $space-2;
  margin-bottom: $space-4;
  overflow-x: auto;
  padding-bottom: $space-2;
  @include custom-scrollbar(3px);

  &::-webkit-scrollbar-thumb {
    height: 3px;
  }
}

.filter-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: $space-2 $space-3;
  border-radius: $radius-pill;
  border: 1px solid $ink-100;
  background: rgba(255, 255, 255, 0.6);
  color: $ink-500;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all $dur-fast $ease-out;

  &.active {
    background: $grad-mint;
    color: #fff;
    border-color: transparent;
    box-shadow: $shadow-glow-mint;
  }

  &:hover:not(.active) {
    border-color: $mint-300;
    color: $mint-600;
  }

  .filter-icon {
    display: inline-flex;
    align-items: center;

    :deep(svg) {
      width: 14px;
      height: 14px;
    }
  }
}

.filter-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $coral-500;
}

// ---------- 通知列表 ----------
.notify-list {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.notify-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  border-radius: $radius-md;
  cursor: pointer;
  transition: transform $dur-fast $ease-out, box-shadow $dur-base $ease-out;
  animation: fade-up 0.5s $ease-out both;

  &.unread {
    background: rgba(78, 205, 196, 0.06);
    border-left: 3px solid $mint-500;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: $shadow-sm;
  }
}

.notify-type-icon {
  width: 36px;
  height: 36px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.like {
    background: rgba(255, 107, 107, 0.12);
    color: $coral-500;
  }

  &.comment {
    background: rgba(78, 205, 196, 0.12);
    color: $mint-500;
  }

  &.reply {
    background: rgba(255, 182, 39, 0.12);
    color: $amber-500;
  }

  &.follow {
    background: rgba(77, 163, 255, 0.12);
    color: $sky-500;
  }

  &.circle {
    background: rgba(107, 203, 119, 0.12);
    color: $leaf-500;
  }

  &.system {
    background: rgba(255, 182, 39, 0.12);
    color: $amber-500;
  }
}

.notify-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: $space-3;
  min-width: 0;
}

.notify-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.notify-text {
  flex: 1;
  min-width: 0;
}

.notify-desc {
  font-size: 14px;
  color: $ink-700;
  line-height: 1.5;
  @include ellipsis-1;
}

.notify-name {
  font-weight: 600;
  color: $ink-900;
}

.notify-extra {
  font-size: 12px;
  color: $ink-500;
  margin-top: 2px;
  @include ellipsis-1;
}

.notify-time {
  font-size: 11px;
  color: $ink-300;
  margin-top: 2px;
  display: block;
}

.unread-mark {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $coral-500;
  flex-shrink: 0;
  animation: hex-breathe 2s ease-in-out infinite;
}

// ---------- 列表动画 ----------
.list-anim-enter-active {
  animation: fade-up 0.4s $ease-out both;
}

.list-anim-leave-active {
  animation: fade-in 0.2s ease reverse both;
}

// ---------- 响应式 ----------
@include mobile {
  .msg-header .header-inner {
    padding: $space-2 $space-3;
  }

  .tab-content {
    padding: $space-2 $space-3;
  }

  .chat-item {
    padding: $space-3;
  }

  .notify-item {
    padding: $space-2 $space-3;
  }
}
</style>
