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
    url: '/message/list',
    method: 'get',
    params: { userId, page, pageSize }
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
  return request<Message>({ url: '/message', method: 'post', data })
}

/**
 * 撤回 AI 代回复消息
 * @param {number} id
 */
export function revokeAiMessage(id: number) {
  return request({ url: `/message/${id}/revoke`, method: 'post' })
}

/**
 * 标记消息已读
 * @param {number} fromUserId
 */
export function markRead(fromUserId: number) {
  return request({ url: '/message/read', method: 'post', data: { fromUserId } })
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
