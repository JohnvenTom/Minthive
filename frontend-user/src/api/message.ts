/**
 * 私信与通知 API
 * @module api/message
 */

import { request } from './request'
import type { Message, Notification, PageResult } from '@/types'

/**
 * 私信会话列表
 */
export function getChatList() {
  return request<any[]>({ url: '/message/chats', method: 'get' })
}

/**
 * 获取与某用户的历史消息
 * @param {number} userId
 * @param {number} page
 * @param {number} pageSize
 */
export function getMessages(userId: number, page = 1, pageSize = 30) {
  return request<PageResult<Message>>({
    url: '/message/history',
    method: 'get',
    params: { peerId: userId, limit: pageSize }
  })
}

/**
 * 发送私信
 * @param {object} data
 */
export function sendMessage(data: {
  toUserId: number
  content: string
  type?: 'text' | 'image' | 'emoji'
}) {
  // 后端字段: toUserId, content, msgType(0文字/1图片/2表情)
  const typeMap: Record<string, number> = { text: 0, image: 1, emoji: 2 }
  return request<Message>({
    url: '/message',
    method: 'post',
    data: {
      toUserId: data.toUserId,
      content: data.content,
      msgType: typeMap[data.type || 'text']
    }
  })
}

/**
 * 撤回消息（支持普通消息和AI代回复消息）
 * @param {number} id - 消息ID
 */
export function revokeAiMessage(id: number) {
  return request({ url: `/message/revoke/${id}`, method: 'delete' })
}

/**
 * 标记消息已读
 * @param {number} fromUserId
 */
export function markRead(fromUserId: number) {
  return request({ url: '/message/read', method: 'post', params: { fromUserId } })
}

/**
 * 通知列表
 * @param {'like' | 'comment' | 'follow' | 'circle' | 'system'} type
 */
export function getNotifications(type?: string, page = 1, pageSize = 20) {
  return request<PageResult<Notification>>({
    url: '/message/notifications',
    method: 'get',
    params: { type, page, pageSize }
  })
}

/**
 * 未读消息统计
 */
export function getUnreadCount() {
  return request<{ like: number; comment: number; message: number; circle: number; system: number }>({
    url: '/message/unread',
    method: 'get'
  })
}
