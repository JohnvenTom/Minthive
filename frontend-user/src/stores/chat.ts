/**
 * 聊天状态管理
 * @module stores/chat
 * @description 管理私信会话列表、当前聊天对象、未读消息统计、WebSocket 连接状态
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { wsClient } from '@/utils/websocket'
import { getChatList, getUnreadCount, markRead as markReadApi, sendMessage as sendMessageApi } from '@/api/message'
import type { Message } from '@/types'

/** 会话项接口 */
export interface ChatItem {
  userId: number
  nickname: string
  avatar: string
  lastMessage: string
  lastTime: string
  unread: number
}

export const useChatStore = defineStore('chat', () => {
  // ---------- 状态 ----------

  /** 会话列表 */
  const chatList = ref<ChatItem[]>([])

  /** 当前聊天对象 */
  const currentChat = ref<ChatItem | null>(null)

  /** 未读消息总数 */
  const unreadCount = ref<number>(0)

  /** WebSocket 连接状态 */
  const wsConnected = ref<boolean>(false)

  // ---------- 计算属性 ----------

  /**
   * 是否有未读消息
   * @returns {boolean}
   */
  const hasUnread = computed(() => unreadCount.value > 0)

  // ---------- Actions ----------

  /**
   * 建立 WebSocket 连接
   * @description 连接聊天 WebSocket 并注册消息监听，
   *   收到新消息时自动更新会话列表和未读计数
   * @returns {void}
   */
  function connectWs(): void {
    if (wsConnected.value) return

    wsClient.on('open', () => {
      wsConnected.value = true
    })

    wsClient.on('close', () => {
      wsConnected.value = false
    })

    wsClient.on('message', (data: Message) => {
      // 收到新消息，更新会话列表中对应会话
      const existingChat = chatList.value.find(c => c.userId === data.fromUserId)
      if (existingChat) {
        existingChat.lastMessage = data.content
        existingChat.lastTime = data.createTime
        if (!currentChat.value || currentChat.value.userId !== data.fromUserId) {
          existingChat.unread++
          unreadCount.value++
        }
      } else {
        // 新会话，添加到列表顶部
        chatList.value.unshift({
          userId: data.fromUserId,
          nickname: data.fromNickname,
          avatar: data.fromAvatar,
          lastMessage: data.content,
          lastTime: data.createTime,
          unread: 1
        })
        unreadCount.value++
      }
    })

    wsClient.connect()
  }

  /**
   * 断开 WebSocket 连接
   * @returns {void}
   */
  function disconnectWs(): void {
    wsClient.close()
    wsConnected.value = false
  }

  /**
   * 拉取会话列表
   * @description 从服务端获取最新会话列表并更新本地状态
   * @returns {Promise<ChatItem[]>}
   * @throws {Error} 网络异常时抛出
   */
  async function fetchChatList(): Promise<ChatItem[]> {
    const res = await getChatList()
    chatList.value = res.data || []
    return chatList.value
  }

  /**
   * 拉取未读消息统计
   * @description 从服务端获取各类型未读数量并汇总
   * @returns {Promise<number>} 未读总数
   * @throws {Error} 网络异常时抛出
   */
  async function fetchUnreadCount(): Promise<number> {
    const res = await getUnreadCount()
    const data = res.data
    unreadCount.value = (data?.message || 0) + (data?.comment || 0) + (data?.like || 0) + (data?.circle || 0) + (data?.system || 0)
    return unreadCount.value
  }

  /**
   * 发送私信
   * @param {number} toUserId - 接收方用户 ID
   * @param {string} content - 消息内容
   * @param {'text' | 'image' | 'emoji'} type - 消息类型
   * @returns {Promise<Message>} 发送成功的消息对象
   * @throws {Error} 发送失败时抛出
   */
  async function sendMessage(toUserId: number, content: string, type: 'text' | 'image' | 'emoji' = 'text'): Promise<Message> {
    const res = await sendMessageApi({ toUserId, content, type })
    return res.data
  }

  /**
   * 标记与某用户的会话为已读
   * @param {number} fromUserId - 对方用户 ID
   * @returns {Promise<void>}
   */
  async function markRead(fromUserId: number): Promise<void> {
    await markReadApi(fromUserId)
    // 更新本地未读计数
    const chat = chatList.value.find(c => c.userId === fromUserId)
    if (chat) {
      unreadCount.value = Math.max(0, unreadCount.value - chat.unread)
      chat.unread = 0
    }
  }

  /**
   * 设置当前聊天对象
   * @param {ChatItem | null} chat - 聊天对象，null 表示关闭聊天
   * @returns {void}
   */
  function setCurrentChat(chat: ChatItem | null): void {
    currentChat.value = chat
  }

  return {
    chatList,
    currentChat,
    unreadCount,
    wsConnected,
    hasUnread,
    connectWs,
    disconnectWs,
    fetchChatList,
    fetchUnreadCount,
    sendMessage,
    markRead,
    setCurrentChat
  }
}, {
  persist: {
    key: 'minthive_chat',
    pick: ['unreadCount']
  }
})
