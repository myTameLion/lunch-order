/**
 * 公共聊天频道 WebSocket 客户端（D-018）：
 * - 连接 `ws(s)://<host>/ws/chat?token=<JWT>`；
 * - 收到 {"type":"chat","message":{...}} 回调 onChatMessage；
 * - {"type":"error",...} 回调 onError；
 * - 断线后 3 秒自动重连（手动 close 不重连）。
 */
export interface ChatSocketHandlers {
  onChatMessage: (message: { loginName: string; displayName: string; content: string; sentAt: string }) => void
  onError?: (message: string) => void
  onOpen?: () => void
}

export class ChatSocket {
  private ws: WebSocket | null = null
  private closedByUser = false
  private reconnectTimer: number | undefined

  constructor(
    private readonly token: string,
    private readonly handlers: ChatSocketHandlers,
  ) {}

  connect(): void {
    this.closedByUser = false
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    const url = `${proto}://${location.host}/ws/chat?token=${encodeURIComponent(this.token)}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => this.handlers.onOpen?.()
    this.ws.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data as string)
        if (payload.type === 'chat' && payload.message) this.handlers.onChatMessage(payload.message)
        else if (payload.type === 'error' && payload.message) this.handlers.onError?.(payload.message)
      } catch {
        /* 非 JSON 帧忽略 */
      }
    }
    this.ws.onclose = () => {
      if (!this.closedByUser) {
        this.reconnectTimer = window.setTimeout(() => this.connect(), 3000)
      }
    }
  }

  send(content: string): boolean {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ content }))
      return true
    }
    return false
  }

  close(): void {
    this.closedByUser = true
    if (this.reconnectTimer) window.clearTimeout(this.reconnectTimer)
    this.ws?.close(1000, 'bye')
    this.ws = null
  }
}
