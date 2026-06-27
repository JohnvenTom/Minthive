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
 * @returns {Promise<AiPostDraft[]>} 三版文案（含风格、内容、话题）
 */
export function aiGeneratePost(keyword: string, circleCategory?: string) {
  return request<AiPostDraft[]>({
    url: '/ai/post-content',
    method: 'post',
    data: { keywords: keyword, category: circleCategory },
    timeout: 60000
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
    data: { content },
    timeout: 60000
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
    data: { postContent, category },
    timeout: 60000
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
    data: { context, incomingMessage },
    timeout: 60000
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
    violation: boolean
    violationType: string
    riskLevel: string
  }>({
    url: '/ai/detect',
    method: 'post',
    data: { text, imageBase64 },
    timeout: 60000
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
    data: { question },
    timeout: 60000
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
 * @param {(err: Error) => void} [onError] - 错误回调
 * @param {string[]} [history] - 对话历史（偶数索引为 user 消息，奇数索引为 ai 回复）
 */
export function aiChatStream(
  question: string,
  onChunk: (text: string) => void,
  onDone: () => void,
  onError?: (err: Error) => void,
  history?: string[]
): AbortController {
  const controller = new AbortController()

  /**
   * 尝试解析 JSON 字符串（处理被引号包裹的情况）
   */
  function tryUnquoteJsonString(raw: string): string {
    if (raw.startsWith('"') && raw.endsWith('"')) {
      try {
        return JSON.parse(raw)
      } catch {
        return raw
      }
    }
    return raw
  }

  // 防重入守卫：确保回调只触发一次，避免流结束后继续写入导致数据混乱
  let settled = false
  const settle = (fn: () => void) => {
    if (settled) return
    settled = true
    fn()
  }

  // 获取基础 URL
  // 注意：SSE 流式接口必须直连后端，不能走 Vite 代理
  // 原因：http-proxy 会缓冲/截断长连接 SSE 流，导致 fetch reader 提前收到 done
  // 开发环境：使用当前页面的 hostname + 后端端口，避免跨域
  const baseUrl = import.meta.env.VITE_API_BASE_URL ||
    (import.meta.env.DEV ? `${window.location.protocol}//${window.location.hostname}:8080` : '')
  console.log('[AI Stream] 开始请求:', `${baseUrl}/api/ai/qa/stream`)

  fetch(`${baseUrl}/api/ai/qa/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      // 携带认证 Token
      ...(localStorage.getItem('minthive_token')
        ? { Authorization: `Bearer ${localStorage.getItem('minthive_token')}` }
        : {})
    },
    body: JSON.stringify({ question, history: history || [] }),
    signal: controller.signal
  })
    .then(async (response) => {
      console.log('[AI Stream] 响应状态:', response.status, response.headers.get('content-type'))

      if (!response.ok) {
        // 尝试读取错误信息（非流式响应）
        const errorText = await response.text().catch(() => '')
        throw new Error(`HTTP ${response.status}: ${errorText}`)
      }

      if (!response.body) {
        throw new Error('response.body 为空，浏览器可能不支持流式读取')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          console.log('[AI Stream] 流读取完成, 剩余buffer:', buffer)
          break
        }

        buffer += decoder.decode(value, { stream: true })

        // 按 SSE 格式拆分：兼容 \n 和 \r\n 换行风格
        const lines = buffer.split(/\r?\n/)
        buffer = lines.pop() || '' // 最后一个可能不完整的行放回缓冲区

        for (const line of lines) {
          const trimmed = line.trim()
          // 跳过空行和注释行
          if (!trimmed || trimmed.startsWith(':')) continue

          // 处理 event 字段行（跳过，只关注 data 行）
          if (trimmed.startsWith('event:')) continue

          // 处理 data 行
          if (trimmed.startsWith('data:')) {
            const data = trimmed.slice(5).trimStart()

            // 流结束标记
            if (data.trim() === '[DONE]') {
              console.log('[AI Stream] 收到 [DONE] 标记')
              settle(onDone)
              return
            }

            try {
              // 已结束的流不再处理新数据
              if (settled) {
                console.warn('[AI Stream] 收到数据但流已结束, 忽略:', data)
                continue
              }

              let content = ''
              const probe = data.trimStart()
              if (probe.startsWith('{')) {
                try {
                  const json = JSON.parse(probe)
                  content = json?.choices?.[0]?.delta?.content ?? ''
                } catch {
                  content = tryUnquoteJsonString(data)
                }
              } else {
                content = tryUnquoteJsonString(data)
              }
              if (content) {
                console.log('[AI Stream] chunk:', content)
                onChunk(content)
              }
            } catch (e) {
              console.warn('[AI Stream] data行解析异常:', e, '原始数据:', trimmed)
            }
          }
        }
      }

      // 流正常结束（服务器关闭连接但没发 [DONE]）
      if (!settled) {
        console.log('[AI Stream] 流自然结束')
        settle(onDone)
      }
    })
    .catch((err) => {
      if ((err as Error).name !== 'AbortError') {
        console.error('[AI Stream] 请求异常:', err)
        settle(() => onError?.(err as Error))
      } else {
        console.log('[AI Stream] 请求被中断(AbortError)')
      }
    })

  return controller
}

/**
 * 导航动作
 */
export interface NavigationAction {
  label: string
  path: string
  type: 'post' | 'circle' | 'user' | 'search' | 'page'
  preview: string
}

/**
 * AI 数据感知问答（流式输出）
 *
 * <p>自动识别提问是否需要查数据库获取真实数据，返回流式文本 + 导航卡片 + 元数据</p>
 *
 * @param question - 用户提问
 * @param callbacks - 回调集
 * @param callbacks.onChunk - 每收到一段文本的回调（纯文本内容）
 * @param callbacks.onNavigation - 收到导航建议的回调（导航卡片列表）
 * @param callbacks.onMeta - 收到元数据回调（如 hasRealData 标记）
 * @param callbacks.onDone - 流结束回调
 * @param callbacks.onError - 错误回调
 * @param history - 对话历史
 */
export function aiQueryStream(
  question: string,
  callbacks: {
    onChunk: (text: string) => void
    onNavigation: (items: NavigationAction[]) => void
    onMeta: (meta: { hasRealData: boolean }) => void
    onStatus?: (status: { status: string; message: string }) => void
    onDone: () => void
    onError?: (err: Error) => void
  },
  history?: string[]
): AbortController {
  const controller = new AbortController()
  const { onChunk, onNavigation, onMeta, onStatus, onDone, onError } = callbacks

  let settled = false
  const settle = (fn: () => void) => {
    if (settled) return
    settled = true
    fn()
  }

  const baseUrl = import.meta.env.VITE_API_BASE_URL ||
    (import.meta.env.DEV ? `${window.location.protocol}//${window.location.hostname}:8080` : '')

  fetch(`${baseUrl}/api/ai/query/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      ...(localStorage.getItem('minthive_token')
        ? { Authorization: `Bearer ${localStorage.getItem('minthive_token')}` }
        : {})
    },
    body: JSON.stringify({ question, history: history || [] }),
    signal: controller.signal
  })
    .then(async (response) => {
      if (!response.ok) {
        const errorText = await response.text().catch(() => '')
        throw new Error(`HTTP ${response.status}: ${errorText}`)
      }
      if (!response.body) {
        throw new Error('response.body 为空')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split(/\r?\n/)
        buffer = lines.pop() || ''

        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed || trimmed.startsWith(':') || trimmed.startsWith('event:')) continue

          if (trimmed.startsWith('data:')) {
            const data = trimmed.slice(5).trimStart()

            if (data.trim() === '[DONE]') {
              settle(onDone)
              return
            }

            if (settled) continue

            try {
              const parsed = JSON.parse(data)
              // 状态事件（进度指示）
              if (parsed.type === 'status' && parsed.status) {
                onStatus?.(parsed)
                continue
              }
              // 导航事件
              if (parsed.type === 'navigation' && Array.isArray(parsed.items)) {
                onNavigation(parsed.items)
                continue
              }
              // 元数据事件
              if (parsed.type === 'meta') {
                onMeta(parsed)
                continue
              }
              // 文本事件（LLM delta text）
              if (parsed.type === 'text' && parsed.content) {
                onChunk(parsed.content)
                continue
              }
              // 兼容旧格式（纯文本字符串或 choices delta）
              const content = parsed?.choices?.[0]?.delta?.content ?? ''
              if (content) {
                onChunk(content)
              }
            } catch {
              // 非 JSON 行（纯文本数据），直接作为文本
              if (data) {
                onChunk(data)
              }
            }
          }
        }
      }

      if (!settled) settle(onDone)
    })
    .catch((err) => {
      if ((err as Error).name !== 'AbortError') {
        settle(() => onError?.(err as Error))
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
