import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/message', () => ({
  getChatList: vi.fn(),
  getUnreadCount: vi.fn(),
  markRead: vi.fn(),
  sendMessage: vi.fn()
}))

vi.mock('@/utils/websocket', () => ({
  wsClient: { on: vi.fn(), connect: vi.fn(), close: vi.fn() }
}))

vi.mock('@/utils/preferences', () => ({ getPreferences: vi.fn() }))

import { useChatStore, type ChatItem } from '@/stores/chat'
import { getChatList, getUnreadCount, markRead as markReadApi } from '@/api/message'
import { wsClient } from '@/utils/websocket'
import { getPreferences, type UserPreferences } from '@/utils/preferences'
import type { Message } from '@/types'

/** 所有通知开关开启的偏好 */
const ALL_ON: UserPreferences = {
  notifyComment: true,
  notifyLike: true,
  notifyChat: true,
  notifyCircle: true,
  aiAutoReply: false,
  aiPostHelper: true,
  aiCommentSuggest: true
}

/**
 * 读取 chat 事件处理器
 *
 * @returns WebSocket 收到私信时注册的回调
 */
function chatHandler(): (data: Message) => void {
  const call = vi.mocked(wsClient.on).mock.calls.find(([event]) => event === 'chat')
  if (!call) throw new Error('chat 事件未注册')
  return call[1] as (data: Message) => void
}

/**
 * 构造一条服务端推送的私信
 *
 * @param fromUserId 发送方用户 ID
 * @param content 消息内容
 * @returns 消息对象
 */
function message(fromUserId: number, content: string): Message {
  return {
    id: 1,
    fromUserId,
    fromNickname: 'u' + fromUserId,
    fromAvatar: 'a.png',
    toUserId: 100,
    content,
    type: 'text',
    createTime: '2026-11-12 10:00:00'
  } as unknown as Message
}

describe('chat store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(getPreferences).mockReturnValue({ ...ALL_ON })
  })

  it('未读统计按用户通知偏好过滤私信/评论/点赞/圈子，系统消息始终计入', async () => {
    vi.mocked(getUnreadCount).mockResolvedValue({
      data: { message: 3, comment: 2, like: 1, circle: 4, system: 5 }
    } as never)
    vi.mocked(getPreferences).mockReturnValue({ ...ALL_ON, notifyChat: false, notifyComment: false })

    const store = useChatStore()
    const total = await store.fetchUnreadCount()

    expect(total).toBe(1 + 4 + 5)
    expect(store.unreadCount).toBe(10)
    expect(store.hasUnread).toBe(true)
  })

  it('标记已读后扣减未读总数且不会出现负数', async () => {
    vi.mocked(markReadApi).mockResolvedValue({ code: 200, msg: 'ok', data: null } as never)
    const store = useChatStore()
    store.chatList = [
      { userId: 7, nickname: 'u7', avatar: '', lastMessage: '', lastTime: '', unread: 5 },
      { userId: 8, nickname: 'u8', avatar: '', lastMessage: '', lastTime: '', unread: 2 }
    ]
    store.unreadCount = 3

    await store.markRead(7)

    expect(markReadApi).toHaveBeenCalledWith(7)
    expect(store.unreadCount).toBe(0)
    expect(store.chatList[0].unread).toBe(0)
    expect(store.chatList[1].unread).toBe(2)
  })

  it('收到新私信时新增会话并累加未读数', () => {
    const store = useChatStore()
    store.connectWs()
    const onChat = chatHandler()

    onChat(message(7, 'hello'))

    expect(wsClient.connect).toHaveBeenCalled()
    expect(store.chatList).toHaveLength(1)
    expect(store.chatList[0]).toMatchObject({ userId: 7, lastMessage: 'hello', unread: 1 })
    expect(store.unreadCount).toBe(1)
  })

  it('已打开该会话或关闭私信通知时不累加未读', () => {
    const store = useChatStore()
    store.connectWs()
    const onChat = chatHandler()
    const current: ChatItem = {
      userId: 7, nickname: 'u7', avatar: '', lastMessage: '', lastTime: '', unread: 0
    }
    store.chatList = [current]
    store.setCurrentChat(current)

    onChat(message(7, 'hi'))
    expect(store.unreadCount).toBe(0)

    store.setCurrentChat(null)
    vi.mocked(getPreferences).mockReturnValue({ ...ALL_ON, notifyChat: false })
    onChat(message(9, 'hey'))
    expect(store.unreadCount).toBe(0)
    expect(store.chatList[0]).toMatchObject({ userId: 9, unread: 0 })
  })

  it('拉取会话列表后整体替换本地状态', async () => {
    vi.mocked(getChatList).mockResolvedValue({
      data: [{ userId: 3, nickname: 'u3', avatar: '', lastMessage: 'x', lastTime: '', unread: 1 }]
    } as never)

    const store = useChatStore()
    const list = await store.fetchChatList()

    expect(list).toHaveLength(1)
    expect(store.chatList[0].userId).toBe(3)
  })
})
