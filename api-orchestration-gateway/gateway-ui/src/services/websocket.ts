import { Client, IMessage, StompSubscription } from '@stomp/stompjs'

const RECONNECT_DELAY_MS = 3000
const MAX_RECONNECT_ATTEMPTS = 20

function buildBrokerUrl(): string {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${window.location.host}/ws`
}

export type StompMessageHandler = (msg: IMessage) => void

type SubscriptionRecord = {
  destination: string
  callback: StompMessageHandler
  stompSub?: StompSubscription
}

class WebSocketService {
  private client: Client | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectAttempts = 0
  private records: SubscriptionRecord[] = []
  private onConnectCallback: (() => void) | null = null
  private connected = false

  connect(onConnect?: () => void): void {
    this.onConnectCallback = onConnect ?? null
    this.disconnect(false)
    this.reconnectAttempts = 0
    this.initClient()
  }

  private bindSubscriptions(): void {
    const c = this.client
    if (!c?.connected) return
    for (const rec of this.records) {
      rec.stompSub?.unsubscribe()
      rec.stompSub = c.subscribe(rec.destination, rec.callback)
    }
  }

  private initClient(): void {
    const client = new Client({
      brokerURL: buildBrokerUrl(),
      reconnectDelay: 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true
        this.reconnectAttempts = 0
        this.bindSubscriptions()
        this.onConnectCallback?.()
      },
      onDisconnect: () => {
        this.connected = false
        for (const rec of this.records) {
          rec.stompSub = undefined
        }
        this.scheduleReconnect()
      },
      onStompError: () => {
        this.connected = false
        this.scheduleReconnect()
      },
      onWebSocketClose: () => {
        this.connected = false
        this.scheduleReconnect()
      },
    })

    this.client = client
    client.activate()
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) return
    if (this.reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.reconnectAttempts += 1
      if (this.client?.active) {
        this.client.deactivate()
      }
      this.initClient()
    }, RECONNECT_DELAY_MS)
  }

  subscribe(destination: string, callback: StompMessageHandler): () => void {
    const rec: SubscriptionRecord = { destination, callback }
    this.records.push(rec)
    if (this.client?.connected) {
      rec.stompSub = this.client.subscribe(destination, callback)
    }

    return () => {
      rec.stompSub?.unsubscribe()
      rec.stompSub = undefined
      const idx = this.records.indexOf(rec)
      if (idx >= 0) this.records.splice(idx, 1)
    }
  }

  disconnect(clearReconnect = true): void {
    if (clearReconnect && this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.records.forEach((r) => {
      r.stompSub?.unsubscribe()
      r.stompSub = undefined
    })
    this.records = []
    if (this.client?.active) {
      this.client.deactivate()
    }
    this.client = null
    this.connected = false
  }

  isConnected(): boolean {
    return this.connected && !!this.client?.connected
  }
}

export const websocketService = new WebSocketService()
