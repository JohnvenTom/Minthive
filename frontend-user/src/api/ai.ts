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
 * AI 智能问答（流式输出）
 *
 * <p>通过 SSE (Server-Sent Events) 逐块接收 AI 回复，
 * 每收到一个 chunk 就调用 onChunk 回调，实现打字机效果</p>
 *
 * @param {string} question - 用户提问
 * @param {(text: string) => void} onChunk - 每收到一段文本的回调
 * @param {() => void} onDone - 流结束回调
 * @param {(err: Error) => void} onError - 错误回调
 */
export function aiChatStream(
  question: string,
  onChunk: (text: string) => void,
  onDone: () => void,
  onError?: (err: Error) => void
): AbortController {
  const controller = new AbortController()

  // 获取基础 URL（兼容代理和直接访问场景）
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  fetch(`${baseUrl}/api/ai/qa/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      // 携带认证 Token
      ...(localStorage.getItem('minthive_token')
        ? { Authorization: `Bearer ${localStorage.getItem('minthive_token')}` }
        : {})
    },
    body: JSON.stringify({ question }),
    signal: controller.signal
  })
    .then(async (response) => {
      if (!response.ok || !response.body) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // 按 SSE 格式拆分：data: {...}\n\n
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 最后一个可能不完整的行放回缓冲区

        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed.startsWith('data: ')) continue
          const data = trimmed.slice(6).trim()

          if (data === '[DONE]') {
            onDone()
            return
          }

          try {
            // 后端 SseEmitter.event().name("message").data(text) 发送纯文本
            // 后端若转发原始 OpenAI chunk 则为 JSON（含 choices[0].delta.content）
            const trimmedData = data.trim()
            if (!trimmedData) continue

            // 尝试按 JSON 解析（OpenAI 原始 chunk 格式）
            if (trimmedData.startsWith('{')) {
              const json = JSON.parse(trimmedData)
              const content = json?.choices?.[0]?.delta?.content ?? ''
              if (content) onChunk(content)
            } else {
              // 纯文本，直接作为内容推送
              onChunk(trimmedData)
            }
          } catch {
            // 非 JSON 且非正常文本的行忽略
          }
        }
      }

      // 流正常结束
      if (!buffer) onDone()
    })
    .catch((err) => {
      if ((err as Error).name !== 'AbortError') {
        onError?.(err as Error)
      }
    })

  return controller
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
