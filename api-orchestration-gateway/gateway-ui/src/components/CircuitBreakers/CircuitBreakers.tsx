import { useCallback, useEffect, useState } from 'react'
import { ApiError, getCircuitBreakers, resetCircuitBreaker } from '../../services/api'
import type { CircuitBreakerStatus } from '../../types/circuit-breaker'
import { formatTimeAgo } from '../../utils/formatters'
import { StatusBadge } from '../shared/StatusBadge'
import './CircuitBreakers.css'

export function CircuitBreakers() {
  const [items, setItems] = useState<CircuitBreakerStatus[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setError(null)
    try {
      const data = await getCircuitBreakers()
      setItems(data)
    } catch (e) {
      setError((e as ApiError).message ?? 'Failed to load circuit breakers')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
    const t = window.setInterval(() => void load(), 12_000)
    return () => window.clearInterval(t)
  }, [load])

  const handleReset = async (key: string) => {
    try {
      await resetCircuitBreaker(key)
      await load()
    } catch (e) {
      setError((e as ApiError).message ?? 'Reset failed')
    }
  }

  return (
    <div className="circuit-breakers">
      <div className="circuit-breakers-head card u-flex u-justify-between u-items-center">
        <div>
          <h2 className="circuit-breakers-title">Circuit breakers</h2>
          <p className="circuit-breakers-sub u-text-secondary">Auto-refreshes every 12s</p>
        </div>
        <button type="button" className="btn btn-sm" onClick={() => void load()}>
          Refresh
        </button>
      </div>

      {error && <div className="circuit-breakers-banner">{error}</div>}
      {loading && <p className="u-text-secondary">Loading…</p>}

      <div className="circuit-breakers-grid">
        {items.map((cb) => {
          const key = cb.key ?? cb.endpoint
          return (
            <article key={key} className="cb-card card">
              <div className="cb-card-head">
                <span className={`cb-pill cb-pill--${cb.state.toLowerCase()}`} aria-hidden />
                <div className="cb-card-titles">
                  <StatusBadge status={cb.state} size="sm" />
                  <span className="cb-card-health">
                    {cb.state === 'CLOSED' && 'Healthy'}
                    {cb.state === 'OPEN' && 'Circuit open'}
                    {cb.state === 'HALF_OPEN' && 'Testing'}
                  </span>
                </div>
              </div>
              <p className="cb-endpoint u-truncate" title={cb.endpoint}>
                {cb.endpoint}
              </p>
              <dl className="cb-stats">
                <div>
                  <dt>Failures</dt>
                  <dd>
                    {cb.failureCount} / {cb.failureThreshold}
                  </dd>
                </div>
                <div>
                  <dt>Successes</dt>
                  <dd>{cb.successCount}</dd>
                </div>
                <div>
                  <dt>Last failure</dt>
                  <dd>{formatTimeAgo(cb.lastFailureAt)}</dd>
                </div>
                <div>
                  <dt>Last success</dt>
                  <dd>{formatTimeAgo(cb.lastSuccessAt)}</dd>
                </div>
              </dl>
              <button type="button" className="btn btn-sm btn-primary" onClick={() => void handleReset(key)}>
                Force reset
              </button>
            </article>
          )
        })}
      </div>

      {!loading && items.length === 0 && (
        <p className="circuit-breakers-empty u-text-secondary">No circuit breakers registered.</p>
      )}
    </div>
  )
}
