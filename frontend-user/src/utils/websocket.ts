/**
 * WebSocket 封装
 * @module utils/websocket
 * @description 原生 WebSocket 封装，支持自动重连、心跳、消息分发
 *   用于实时评论、私信、通知推送
 */

type MessageHandler = (data: any) => void

interface WsOptions {
  url: string
  heartbeatInterval?: number
  reconnectInterval?: number
  maxReconnect?: number
}

/**
 * WebSocket 客户端单例
 */
class WsClient {
  private ws: WebSocket | null = null
  private url: string
  private heartbeatTimer: number | null = null
  private reconnectTimer: number | null = null
  private reconnectCount = 0
  private heartbeatInterval: number
  private reconnectInterval: number
  private maxReconnect: number
  private handlers: Map<string, Set<MessageHandler>> = new Map()
  private isManualClose = false

  constructor(options: WsOptions) {
    this.url = options.url
    this.heartbeatInterval = options.heartbeatInterval || 25000
    this.reconnectInterval = options.reconnectInterval || 3000
    this.maxReconnect = options.maxReconnect || 10
  }

  /**
   * 建立连接
   */
  connect(): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) return
    this.isManualClose = false
    try {
      this.ws = new WebSocket(this.url)
      this.bindEvents()
    } catch (e) {
      console.error('[WS] 连接失败', e)
      this.reconnect()
    }
  }

  /**
   * 绑定原生事件
   */
  private bindEvents(): void {
    if (!this.ws) return
    this.ws.onopen = () => {
      console.log('[WS] 已连接')
      this.reconnectCount = 0
      this.startHeartbeat()
    }
    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data)
        // 心跳响应过滤
        if (msg.type === 'pong') return
        this.dispatch(msg.type, msg.data)
      } catch {
        // 非 JSON 消息忽略
      }
    }
    this.ws.onclose = () => {
      console.log('[WS] 连接关闭')
      this.stopHeartbeat()
      if (!this.isManualClose) this.reconnect()
    }
    this.ws.onerror = (err) => {
      console.error('[WS] 错误', err)
    }
  }

  /**
   * 启动心跳
   */
  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      this.send({ type: 'ping' })
    }, this.heartbeatInterval)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 自动重连
   */
  private reconnect(): void {
    if (this.reconnectCount >= this.maxReconnect) {
      console.warn('[WS] 达到最大重连次数')
      return
    }
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectCount++
      console.log(`[WS] 第 ${this.reconnectCount} 次重连`)
      this.connect()
    }, this.reconnectInterval)
  }

  /**
   * 发送消息
   * @param {object} data
   */
  send(data: object): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    }
  }

  /**
   * 订阅消息类型
   * @param {string} type - 消息类型：comment/like/message/notice 等
   * @param {MessageHandler} handler
   */
  on(type: string, handler: MessageHandler): void {
    if (!this.handlers.has(type)) this.handlers.set(type, new Set())
    this.handlers.get(type)!.add(handler)
  }

  /**
   * 取消订阅
   */
  off(type: string, handler: MessageHandler): void {
    this.handlers.get(type)?.delete(handler)
  }

  /**
   * 分发消息
   */
  private dispatch(type: string, data: any): void {
    this.handlers.get(type)?.forEach((h) => {
      try { h(data) } catch (e) { console.error('[WS] handler error', e) }
    })
  }

  /**
   * 主动关闭
   */
  close(): void {
    this.isManualClose = true
    this.stopHeartbeat()
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.ws?.close()
    this.ws = null
  }
}

// 单例：连接地址由环境决定
const wsUrl = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/chat`

export const wsClient = new WsClient({ url: wsUrl })

export default wsClient
