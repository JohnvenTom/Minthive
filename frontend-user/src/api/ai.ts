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
 * @returns {Promise<string[]>} 三版文案（纯文本数组）
 */
export function aiGeneratePost(keyword: string, circleCategory?: string) {
  return request<string[]>({
    url: '/ai/post-content',
    method: 'post',
    data: { keywords: keyword, category: circleCategory }
  })
}

/**
 * AI 文案润色
 * @param {string} content - 原始文案
 * @returns {Promise<string>} 润色后文案（纯文本）
 */
export function aiPolishPost(content: string) {
  return request<string>({
    url: '/ai/polish',
    method: 'post',
    data: { content }
  })
}

/**
 * AI 智能评论
 * @param {string} postContent - 帖子内容
 * @param {string} category - 帖子所属圈子分类
 * @returns {Promise<string[]>} 3 条评论建议
 */
export function aiGenerateComment(postContent: string, category?: string) {
  return request<string[]>({
    url: '/ai/comment',
    method: 'post',
    data: { postContent, category }
  })
}

/**
 * AI 私信代回复
 * @param {string} context - 聊天上下文
 * @param {string} incomingMessage - 对方最新消息
 */
export function aiReplyMessage(context: string, incomingMessage: string) {
  return request<string>({
    url: '/ai/message-reply',
    method: 'post',
    data: { context, incomingMessage }
  })
}

/**
 * AI 内容检测
 * @param {string} text - 文本内容
 * @param {string} imageBase64 - 图片 Base64（可选）
 * @returns {Promise<AiDetectResult>} 检测结果
 */
export function aiCheckContent(text?: string, imageBase64?: string) {
  return request<{
    violated: boolean
    type?: string
    level?: 'low' | 'mid' | 'high'
  }>({
    url: '/ai/detect',
    method: 'post',
    data: { text, imageBase64 }
  })
}

/**
 * AI 智能问答（客服）
 * @param {string} question - 用户提问
 * @returns {Promise<{answer: string}>} AI 回答
 */
export function aiChat(question: string) {
  return request<{ answer: string }>({
    url: '/ai/qa',
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
