/**
 * AI 智能 API
 * @module api/ai
 * @description AI 文案生成、润色、评论、私信代回复、内容检测、问答
 *   接口前缀 /api/ai/
 */

import { request } from './request'
import type { AiPostDraft, AiCommentSuggestion } from '@/types'

/**
 * AI 生成帖子文案（3版风格）
 * @param {string} keyword - 关键词
 * @param {string} circleCategory - 圈子分类
 * @returns {Promise<AiPostDraft[]>} 三版文案
 */
export function aiGeneratePost(keyword: string, circleCategory?: string) {
  return request<AiPostDraft[]>({
    url: '/ai/post/generate',
    method: 'post',
    data: { keyword, circleCategory }
  })
}

/**
 * AI 文案润色
 * @param {string} content - 原始文案
 * @returns {Promise<{content: string, topics: string[]}>}
 */
export function aiPolishPost(content: string) {
  return request<{ content: string; topics: string[] }>({
    url: '/ai/post/polish',
    method: 'post',
    data: { content }
  })
}

/**
 * AI 智能评论
 * @param {number} postId
 * @returns {Promise<AiCommentSuggestion[]>} 3 条评论建议
 */
export function aiGenerateComment(postId: number) {
  return request<AiCommentSuggestion[]>({
    url: '/ai/comment/generate',
    method: 'post',
    data: { postId }
  })
}

/**
 * AI 私信代回复
 * @param {number} toUserId
 * @param {string} context - 聊天上下文
 */
export function aiReplyMessage(toUserId: number, context: string) {
  return request<{ content: string }>({
    url: '/ai/message/reply',
    method: 'post',
    data: { toUserId, context }
  })
}

/**
 * AI 内容检测
 * @param {string} content - 文本内容
 * @param {string} imageBase64 - 图片 Base64（可选）
 * @returns {Promise<{violated: boolean, type?: string, level?: 'low'|'mid'|'high'}>}
 */
export function aiCheckContent(content?: string, imageBase64?: string) {
  return request<{ violated: boolean; type?: string; level?: 'low' | 'mid' | 'high' }>({
    url: '/ai/content/check',
    method: 'post',
    data: { content, imageBase64 }
  })
}

/**
 * AI 智能问答（客服）
 * @param {string} question
 */
export function aiChat(question: string) {
  return request<{ answer: string }>({
    url: '/ai/chat',
    method: 'post',
    data: { question }
  })
}

/**
 * AI 兴趣推荐（圈子/好友）
 */
export function aiRecommend() {
  return request<{ circles: number[]; users: number[] }>({
    url: '/ai/recommend',
    method: 'get'
  })
}
