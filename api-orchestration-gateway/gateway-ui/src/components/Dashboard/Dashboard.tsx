import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getCircuitBreakers,
  getExecutionStats,
  getExecutions,
  getFlowPerformance,
} from '../../services/api'
import type { CircuitBreakerStatus } from '../../types/circuit-breaker'
import type { DashboardStats, FlowExecutionResponse, FlowPerformanceSummary } from '../../types/execution'
import { formatDuration, formatTimeAgo } from '../../utils/formatters'
import { StatusBadge } from '../shared/StatusBadge'
import './Dashboard.css'

const emptyStats: DashboardStats = {
  totalFlows: 0,
  activeExecutions: 0,
  successRate: 0,
  circuitBreakersOpen: 0,
}

export function Dashboard() {
  const [stats, setStats] = useState<DashboardStats>(emptyStats)
  const [executions, setExecutions] = useState<FlowExecutionResponse[]>([])
  const [performance, setPerformance] = useState<FlowPerformanceSummary[]>([])
  const [breakers, setBreakers] = useState<CircuitBreakerStatus[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = async () => {
    setError(null)
    try {
      const [s, ex, perf, cb] = await Promise.all([
        getExecutionStats().catch(() => emptyStats),
        getExecutions({ limit: 10 }).catch(() => []),
        getFlowPerformance().catch(() => []),
        getCircuitBreakers().catch(() => []),
      ])
      setStats(s)
      setExecutions(ex)
      setPerformance(perf.slice(0, 6))
      setBreakers(cb)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load dashboard')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
    const t = window.setInterval(() => void load(), 15000)
    return () => window.clearInterval(t)
  }, [])

  const openBreakers = breakers.filter((b) => b.state === 'OPEN').length

  return (
    <div className="dashboard">
      {error && <div className="dashboard-banner">{error}</div>}
      {loading && <p className="u-text-secondary">Loading overview…</p>}

      <div className="dashboard-stats">
        <div className="dashboard-stat card">
          <span className="dashboard-stat-label">Total flows</span>
          <span className="dashboard-stat-value">{stats.totalFlows}</span>
        </div>
        <div className="dashboard-stat card">
          <span className="dashboard-stat-label">Active executions</span>
          <span className="dashboard-stat-value">{stats.activeExecutions}</span>
        </div>
        <div className="dashboard-stat card">
          <span className="dashboard-stat-label">Success rate</span>
          <span className="dashboard-stat-value">{`${(stats.successRate * 100).toFixed(1)}%`}</span>
        </div>
        <div className="dashboard-stat card">
          <span className="dashboard-stat-label">Circuits open</span>
          <span className="dashboard-stat-value dashboard-stat-value--alert">{openBreakers || stats.circuitBreakersOpen}</span>
        </div>
      </div>

      <div className="dashboard-grid">
        <section className="dashboard-panel card">
          <div className="dashboard-panel-head">
            <h2 className="dashboard-panel-title">Recent executions</h2>
            <Link to="/executions" className="dashboard-link">
              View all
            </Link>
          </div>
          <div className="table-wrap">
            <table className="dashboard-table">
              <thead>
                <tr>
                  <th>Flow</th>
                  <th>Status</th>
                  <th>Started</th>
                  <th>Duration</th>
                </tr>
              </thead>
              <tbody>
                {executions.length === 0 && (
                  <tr>
                    <td colSpan={4} className="dashboard-empty">
                      No recent executions
                    </td>
                  </tr>
                )}
                {executions.map((ex) => (
                  <tr key={ex.id}>
                    <td>
                      <Link to={`/executions/${ex.id}`} className="dashboard-flow-link">
                        {ex.flowName ?? ex.flowId}
                      </Link>
                      <div className="dashboard-sub">{ex.id}</div>
                    </td>
                    <td>
                      <StatusBadge status={ex.status} size="sm" />
                    </td>
                    <td>{formatTimeAgo(ex.startedAt)}</td>
                    <td>{formatDuration(ex.durationMs)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <div className="dashboard-side">
          <section className="dashboard-panel card">
            <h2 className="dashboard-panel-title">Flow performance</h2>
            <div className="dashboard-mini-list">
              {performance.length === 0 && <p className="dashboard-empty">No performance data</p>}
              {performance.map((p) => (
                <div key={p.flowId} className="dashboard-mini-card">
                  <div className="dashboard-mini-name">{p.flowName}</div>
                  <div className="dashboard-mini-meta">
                    <span>{p.runs24h} runs / 24h</span>
                    <span>{formatDuration(p.avgDurationMs)} avg</span>
                  </div>
                  <div className="dashboard-mini-bar">
                    <div
                      className="dashboard-mini-bar-fill"
                      style={{ width: `${Math.min(100, p.successRate * 100)}%` }}
                    />
                  </div>
                  <span className="dashboard-mini-rate">{(p.successRate * 100).toFixed(0)}% success</span>
                </div>
              ))}
            </div>
          </section>

          <section className="dashboard-panel card">
            <div className="dashboard-panel-head">
              <h2 className="dashboard-panel-title">Circuit health</h2>
              <Link to="/circuit-breakers" className="dashboard-link">
                Manage
              </Link>
            </div>
            <ul className="dashboard-breaker-summary">
              {breakers.length === 0 && <li className="dashboard-empty">No circuit breakers</li>}
              {breakers.slice(0, 5).map((b) => (
                <li key={b.endpoint} className="dashboard-breaker-row">
                  <span className={`cb-dot cb-dot--${b.state.toLowerCase()}`} aria-hidden />
                  <span className="dashboard-breaker-url u-truncate" title={b.endpoint}>
                    {b.endpoint}
                  </span>
                  <StatusBadge status={b.state} size="sm" />
                </li>
              ))}
            </ul>
          </section>
        </div>
      </div>
    </div>
  )
}
