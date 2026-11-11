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
 * 获取与某用户的历史消息（游标分页）
 * @param {number} userId - 对方用户ID
 * @param {number} [beforeId] - 游标：只取ID小于该值的更早消息，不传表示最新一页
 * @param {number} pageSize - 每页条数，后端上限100
 * @returns {Promise<{ data: Message[] }>} 按ID降序排列的消息数组
 */
export function getMessages(userId: number, beforeId?: number, pageSize = 30) {
  return request<Message[]>({
    url: '/message/history',
    method: 'get',
    params: { peerId: userId, beforeId, limit: pageSize }
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
 * 标记单条通知已读
 * @param {string} type - 通知类型（like/comment/follow/reply/circle/system/report/audit）
 * @param {number} id - 通知记录ID（点赞/评论/关注为业务记录ID，其余为系统消息ID）
 */
export function markNotificationRead(type: string, id: number) {
  return request({ url: '/message/notifications/read', method: 'post', params: { type, id } })
}

/**
 * 未读消息统计
 */
export function getUnreadCount() {
  return request<{ like: number; comment: number; message: number; follow: number; circle: number; system: number; report: number; audit: number }>({
    url: '/message/unread',
    method: 'get'
  })
}
