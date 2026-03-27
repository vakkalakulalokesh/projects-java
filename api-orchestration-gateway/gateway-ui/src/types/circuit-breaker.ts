export type CircuitState = 'CLOSED' | 'OPEN' | 'HALF_OPEN'

export interface CircuitBreakerStatus {
  endpoint: string
  key?: string
  state: CircuitState
  failureCount: number
  failureThreshold?: number
  successCount: number
  lastFailureAt?: string
  lastSuccessAt?: string
  nextRetryAt?: string
}
