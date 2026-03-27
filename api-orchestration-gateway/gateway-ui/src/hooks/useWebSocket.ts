import { useCallback, useEffect, useRef, useState } from 'react'
import type { IMessage } from '@stomp/stompjs'
import { websocketService, type StompMessageHandler } from '../services/websocket'

export interface WsMessage {
  id: string
  destination: string
  body: string
  timestamp: number
}

function parseBody(msg: IMessage): string {
  try {
    return msg.body
  } catch {
    return ''
  }
}

export function useWebSocket() {
  const [connected, setConnected] = useState(false)
  const [messages, setMessages] = useState<WsMessage[]>([])
  const idRef = useRef(0)

  useEffect(() => {
    const tick = () => setConnected(websocketService.isConnected())
    const interval = window.setInterval(tick, 500)
    websocketService.connect(() => {
      setConnected(true)
    })
    tick()
    return () => {
      window.clearInterval(interval)
      websocketService.disconnect()
    }
  }, [])

  const subscribe = useCallback((destination: string, callback?: StompMessageHandler) => {
    return websocketService.subscribe(destination, (msg: IMessage) => {
      const body = parseBody(msg)
      idRef.current += 1
      setMessages((prev) => {
        const next: WsMessage = {
          id: `${Date.now()}-${idRef.current}`,
          destination,
          body,
          timestamp: Date.now(),
        }
        const merged = [...prev, next]
        return merged.slice(-200)
      })
      callback?.(msg)
    })
  }, [])

  return { connected, subscribe, messages }
}
